-- ============================================================================
-- MIGRATION BASE DE DONNÉES - MODULE VIDEO GENERATION
-- Version: V1.0
-- Description: Création des tables pour la génération vidéo
-- Auteur: NexusAI Team
-- Date: 2025-01
-- ============================================================================

-- Extension UUID si pas déjà activée
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- TABLE: generated_videos
-- Description: Stocke les métadonnées des vidéos générées
-- ============================================================================
CREATE TABLE IF NOT EXISTS generated_videos (
    -- Identifiants
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    companion_id VARCHAR(255),
    
    -- Paramètres de génération
    prompt TEXT NOT NULL,
    scenario_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    duration_seconds INTEGER NOT NULL CHECK (duration_seconds BETWEEN 10 AND 600),
    resolution VARCHAR(20) NOT NULL,
    frame_rate INTEGER NOT NULL CHECK (frame_rate IN (24, 30, 60)),
    quality VARCHAR(20) NOT NULL CHECK (quality IN ('STANDARD', 'HD', 'ULTRA')),
    
    -- Statut et progression
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED' 
        CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    current_phase VARCHAR(50)
        CHECK (current_phase IN (
            'SCRIPT_GENERATION',
            'ASSET_GENERATION',
            'COMPOSITING',
            'RENDERING',
            'ENCODING',
            'FINALIZATION'
        )),
    progress_percentage INTEGER DEFAULT 0 CHECK (progress_percentage BETWEEN 0 AND 100),
    
    -- Résultats
    storage_url TEXT,
    thumbnail_urls TEXT[],
    file_size_mb DECIMAL(10,2) CHECK (file_size_mb >= 0),
    generation_time_minutes INTEGER CHECK (generation_time_minutes >= 0),
    
    -- Métadonnées
    is_favorite BOOLEAN DEFAULT FALSE,
    tokens_cost INTEGER NOT NULL CHECK (tokens_cost > 0),
    error_message TEXT,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Contraintes
    CONSTRAINT valid_completion CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL AND storage_url IS NOT NULL) OR
        (status != 'COMPLETED')
    ),
    CONSTRAINT valid_failure CHECK (
        (status = 'FAILED' AND error_message IS NOT NULL) OR
        (status != 'FAILED')
    )
);

-- Commentaires sur les colonnes
COMMENT ON TABLE generated_videos IS 'Table principale des vidéos générées';
COMMENT ON COLUMN generated_videos.id IS 'Identifiant unique de la vidéo';
COMMENT ON COLUMN generated_videos.user_id IS 'Référence vers l''utilisateur propriétaire';
COMMENT ON COLUMN generated_videos.companion_id IS 'Référence vers le compagnon associé';
COMMENT ON COLUMN generated_videos.prompt IS 'Prompt utilisateur décrivant la vidéo';
COMMENT ON COLUMN generated_videos.scenario_json IS 'Scénario généré au format JSON';
COMMENT ON COLUMN generated_videos.status IS 'Statut actuel de la génération';
COMMENT ON COLUMN generated_videos.current_phase IS 'Phase actuelle du pipeline';
COMMENT ON COLUMN generated_videos.progress_percentage IS 'Progression de 0 à 100%';
COMMENT ON COLUMN generated_videos.storage_url IS 'URL S3 de la vidéo finale';
COMMENT ON COLUMN generated_videos.tokens_cost IS 'Coût en jetons de la génération';

-- ============================================================================
-- TABLE: video_assets
-- Description: Stocke les assets individuels (images, audio) d'une vidéo
-- ============================================================================
CREATE TABLE IF NOT EXISTS video_assets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    video_id UUID NOT NULL REFERENCES generated_videos(id) ON DELETE CASCADE,
    
    asset_type VARCHAR(50) NOT NULL
        CHECK (asset_type IN (
            'BACKGROUND',
            'CHARACTER',
            'AUDIO_VOICE',
            'AUDIO_MUSIC',
            'AUDIO_SFX',
            'SUBTITLE'
        )),
    
    scene_number INTEGER CHECK (scene_number > 0),
    storage_url TEXT NOT NULL,
    metadata JSONB DEFAULT '{}'::JSONB,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_asset_per_scene UNIQUE (video_id, asset_type, scene_number)
);

COMMENT ON TABLE video_assets IS 'Assets individuels composant une vidéo';
COMMENT ON COLUMN video_assets.asset_type IS 'Type d''asset (image, audio, etc.)';
COMMENT ON COLUMN video_assets.scene_number IS 'Numéro de scène associé';
COMMENT ON COLUMN video_assets.storage_url IS 'URL S3 de l''asset';
COMMENT ON COLUMN video_assets.metadata IS 'Métadonnées additionnelles au format JSON';

-- ============================================================================
-- INDEX POUR PERFORMANCES
-- ============================================================================

-- Index pour les requêtes fréquentes
CREATE INDEX idx_videos_user_id ON generated_videos(user_id);
CREATE INDEX idx_videos_status ON generated_videos(status) WHERE status IN ('QUEUED', 'PROCESSING');
CREATE INDEX idx_videos_created_at ON generated_videos(created_at DESC);
CREATE INDEX idx_videos_user_created ON generated_videos(user_id, created_at DESC);
CREATE INDEX idx_videos_user_status ON generated_videos(user_id, status);
CREATE INDEX idx_videos_favorites ON generated_videos(user_id, is_favorite) WHERE is_favorite = TRUE;
CREATE INDEX idx_videos_companion ON generated_videos(companion_id) WHERE companion_id IS NOT NULL;

-- Index pour les assets
CREATE INDEX idx_assets_video_id ON video_assets(video_id);
CREATE INDEX idx_assets_type ON video_assets(asset_type);

-- Index GIN pour recherche JSON
CREATE INDEX idx_videos_scenario_gin ON generated_videos USING GIN (scenario_json);

-- ============================================================================
-- TRIGGER POUR updated_at AUTOMATIQUE
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_videos_updated_at
    BEFORE UPDATE ON generated_videos
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- VUES UTILES
-- ============================================================================

-- Vue des vidéos actives (en cours de traitement)
CREATE OR REPLACE VIEW active_videos AS
SELECT 
    id,
    user_id,
    prompt,
    quality,
    status,
    current_phase,
    progress_percentage,
    created_at,
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - created_at))/60 AS processing_minutes
FROM generated_videos
WHERE status IN ('QUEUED', 'PROCESSING')
ORDER BY created_at ASC;

COMMENT ON VIEW active_videos IS 'Vue des vidéos actuellement en traitement';

-- Vue statistiques par utilisateur
CREATE OR REPLACE VIEW user_video_stats AS
SELECT 
    user_id,
    COUNT(*) AS total_videos,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed_videos,
    COUNT(*) FILTER (WHERE status = 'FAILED') AS failed_videos,
    COUNT(*) FILTER (WHERE status IN ('QUEUED', 'PROCESSING')) AS active_videos,
    SUM(tokens_cost) AS total_tokens_spent,
    AVG(generation_time_minutes) FILTER (WHERE status = 'COMPLETED') AS avg_generation_time,
    SUM(file_size_mb) FILTER (WHERE status = 'COMPLETED') AS total_storage_mb
FROM generated_videos
GROUP BY user_id;

COMMENT ON VIEW user_video_stats IS 'Statistiques de génération vidéo par utilisateur';

-- ============================================================================
-- FONCTIONS UTILITAIRES
-- ============================================================================

-- Fonction pour obtenir la position dans la file d'attente
CREATE OR REPLACE FUNCTION get_queue_position(p_video_id UUID)
RETURNS INTEGER AS $$
DECLARE
    v_position INTEGER;
BEGIN
    SELECT COUNT(*) + 1 INTO v_position
    FROM generated_videos
    WHERE status = 'QUEUED'
    AND created_at < (
        SELECT created_at 
        FROM generated_videos 
        WHERE id = p_video_id
    );
    
    RETURN COALESCE(v_position, 0);
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_queue_position IS 'Retourne la position d''une vidéo dans la file d''attente';

-- Fonction pour nettoyer les anciennes vidéos échouées
CREATE OR REPLACE FUNCTION cleanup_failed_videos(p_days_old INTEGER DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE
    v_deleted_count INTEGER;
BEGIN
    WITH deleted AS (
        DELETE FROM generated_videos
        WHERE status = 'FAILED'
        AND created_at < CURRENT_TIMESTAMP - (p_days_old || ' days')::INTERVAL
        RETURNING id
    )
    SELECT COUNT(*) INTO v_deleted_count FROM deleted;
    
    RETURN v_deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_failed_videos IS 'Supprime les vidéos échouées de plus de N jours';

-- ============================================================================
-- DONNÉES DE TEST (uniquement en environnement de développement)
-- ============================================================================

-- Exemple d'insertion (à adapter selon les besoins)
/*
INSERT INTO generated_videos (
    user_id,
    companion_id,
    prompt,
    duration_seconds,
    resolution,
    frame_rate,
    quality,
    tokens_cost
) VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'companion-123',
    'Une vidéo de mon compagnon dans un jardin fleuri',
    60,
    '1080p',
    30,
    'STANDARD',
    300
);
*/

-- ============================================================================
-- GRANTS (à adapter selon votre système de rôles)
-- ============================================================================

-- GRANT SELECT, INSERT, UPDATE, DELETE ON generated_videos TO nexusai_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON video_assets TO nexusai_app;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO nexusai_app;

-- ============================================================================
-- FIN DE LA MIGRATION
-- ============================================================================

---
# Dockerfile pour le service Java
# Fichier: nexus-video-generation/Dockerfile

FROM eclipse-temurin:21-jdk-alpine AS build

# Installation des dépendances système
RUN apk add --no-cache maven

# Définition du répertoire de travail
WORKDIR /app

# Copie des fichiers Maven
COPY pom.xml .
COPY src ./src

# Build de l'application
RUN mvn clean package -DskipTests

# ============================================================================
# Image finale
# ============================================================================
FROM eclipse-temurin:21-jre-alpine

# Installation des dépendances runtime
RUN apk add --no-cache curl

# Création de l'utilisateur applicatif
RUN addgroup -g 1000 nexusai && \
    adduser -D -u 1000 -G nexusai nexusai

# Définition du répertoire de travail
WORKDIR /app

# Copie du JAR depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Changement de propriétaire
RUN chown -R nexusai:nexusai /app

# Bascule vers l'utilisateur applicatif
USER nexusai

# Exposition du port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Point d'entrée
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

---
# Dockerfile pour le worker Python
# Fichier: video-worker/Dockerfile

FROM python:3.11-slim

# Installation des dépendances système
RUN apt-get update && apt-get install -y \
    ffmpeg \
    libsm6 \
    libxext6 \
    libxrender-dev \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Définition du répertoire de travail
WORKDIR /app

# Copie des requirements
COPY requirements.txt .

# Installation des dépendances Python
RUN pip install --no-cache-dir -r requirements.txt

# Copie du code source
COPY worker.py .

# Création du répertoire temporaire
RUN mkdir -p /tmp/video-generation && \
    chmod 777 /tmp/video-generation

# Variables d'environnement par défaut
ENV PYTHONUNBUFFERED=1
ENV TEMP_DIR=/tmp/video-generation

# Point d'entrée
CMD ["python", "-u", "worker.py"]

---
# Requirements Python
# Fichier: video-worker/requirements.txt

# Kafka
kafka-python==2.0.2

# AWS
boto3==1.34.0

# OpenAI
openai==1.6.1

# Image processing
Pillow==10.2.0
numpy==1.26.3
opencv-python==4.9.0.80

# HTTP requests
requests==2.31.0

---
# Docker Compose pour développement local
# Fichier: docker-compose.yml

version: '3.8'

services:
  # Service Java
  video-service:
    build:
      context: ./nexus-video-generation
      dockerfile: Dockerfile
    container_name: nexus-video-service
    ports:
      - "8084:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/nexusai
      SPRING_DATASOURCE_USERNAME: nexusai
      SPRING_DATASOURCE_PASSWORD: nexusai123
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SPRING_DATA_REDIS_HOST: redis
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      ELEVENLABS_API_KEY: ${ELEVENLABS_API_KEY}
    depends_on:
      - postgres
      - kafka
      - redis
    networks:
      - nexusai-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  # Worker Python (3 instances)
  video-worker-1:
    build:
      context: ./video-worker
      dockerfile: Dockerfile
    container_name: nexus-video-worker-1
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      KAFKA_TOPIC_REQUESTS: video.generation.requests
      KAFKA_TOPIC_EVENTS: video.generation.events
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
      AWS_REGION: eu-west-1
      S3_BUCKET_VIDEOS: nexusai-videos-dev
      S3_BUCKET_ASSETS: nexusai-video-assets-dev
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      ELEVENLABS_API_KEY: ${ELEVENLABS_API_KEY}
      WORKER_ID: worker-1
    depends_on:
      - kafka
    networks:
      - nexusai-network
    restart: unless-stopped
    volumes:
      - worker-temp-1:/tmp/video-generation

  video-worker-2:
    build:
      context: ./video-worker
      dockerfile: Dockerfile
    container_name: nexus-video-worker-2
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      KAFKA_TOPIC_REQUESTS: video.generation.requests
      KAFKA_TOPIC_EVENTS: video.generation.events
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
      AWS_REGION: eu-west-1
      S3_BUCKET_VIDEOS: nexusai-videos-dev
      S3_BUCKET_ASSETS: nexusai-video-assets-dev
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      ELEVENLABS_API_KEY: ${ELEVENLABS_API_KEY}
      WORKER_ID: worker-2
    depends_on:
      - kafka
    networks:
      - nexusai-network
    restart: unless-stopped
    volumes:
      - worker-temp-2:/tmp/video-generation

  video-worker-3:
    build:
      context: ./video-worker
      dockerfile: Dockerfile
    container_name: nexus-video-worker-3
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      KAFKA_TOPIC_REQUESTS: video.generation.requests
      KAFKA_TOPIC_EVENTS: video.generation.events
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
      AWS_REGION: eu-west-1
      S3_BUCKET_VIDEOS: nexusai-videos-dev
      S3_BUCKET_ASSETS: nexusai-video-assets-dev
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      ELEVENLABS_API_KEY: ${ELEVENLABS_API_KEY}
      WORKER_ID: worker-3
    depends_on:
      - kafka
    networks:
      - nexusai-network
    restart: unless-stopped
    volumes:
      - worker-temp-3:/tmp/video-generation

volumes:
  worker-temp-1:
  worker-temp-2:
  worker-temp-3:

networks:
  nexusai-network:
    external: true
