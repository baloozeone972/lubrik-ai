# MODULE 5 : IMAGE GENERATION - DOCUMENTATION COMPLÈTE

## 📋 Table des Matières

1. [Scripts SQL](#scripts-sql)
2. [Installation](#installation)
3. [Configuration](#configuration)
4. [Utilisation](#utilisation)
5. [API Documentation](#api-documentation)
6. [Tests](#tests)
7. [Déploiement](#déploiement)
8. [Troubleshooting](#troubleshooting)

---

## 🗄️ Scripts SQL

### Script de Création des Tables

```sql
-- ═══════════════════════════════════════════════════════════════════════════
-- NEXUSAI - MODULE IMAGE GENERATION - SCHÉMA DATABASE
-- Version: 1.0.0
-- Date: 2025-01-01
-- ═══════════════════════════════════════════════════════════════════════════

-- Activation de l'extension UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ═══════════════════════════════════════════════════════════════════════════
-- Table: generated_images
-- Description: Stocke les métadonnées des images générées
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS generated_images (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    companion_id VARCHAR(255),
    
    -- Paramètres de génération
    prompt TEXT NOT NULL,
    negative_prompt TEXT,
    style VARCHAR(50),
    resolution VARCHAR(20) NOT NULL,
    seed INTEGER,
    parameters JSONB,
    
    -- Statut et résultats
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    storage_url TEXT,
    thumbnail_url TEXT,
    generation_time_seconds INTEGER,
    
    -- Métadonnées
    is_favorite BOOLEAN DEFAULT FALSE,
    is_public BOOLEAN DEFAULT FALSE,
    tokens_cost INTEGER NOT NULL,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    
    -- Contraintes
    CONSTRAINT valid_status CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT valid_resolution CHECK (resolution IN ('512x512', '768x768', '1024x1024', '1024x1536')),
    CONSTRAINT valid_style CHECK (style IN ('realistic', 'anime', 'artistic', '3d', 'sketch'))
);

-- Index pour optimiser les requêtes
CREATE INDEX idx_generated_images_user_id ON generated_images(user_id);
CREATE INDEX idx_generated_images_status ON generated_images(status);
CREATE INDEX idx_generated_images_created_at ON generated_images(created_at DESC);
CREATE INDEX idx_generated_images_favorite ON generated_images(user_id, is_favorite) WHERE is_favorite = TRUE;
CREATE INDEX idx_generated_images_public ON generated_images(is_public) WHERE is_public = TRUE;
CREATE INDEX idx_generated_images_prompt_search ON generated_images USING gin(to_tsvector('english', prompt));

-- Commentaires
COMMENT ON TABLE generated_images IS 'Métadonnées des images générées par IA';
COMMENT ON COLUMN generated_images.status IS 'QUEUED: En attente | PROCESSING: En cours | COMPLETED: Terminé | FAILED: Échec';
COMMENT ON COLUMN generated_images.tokens_cost IS 'Coût en tokens de la génération';

-- ═══════════════════════════════════════════════════════════════════════════
-- Table: image_albums
-- Description: Albums pour organiser les images
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS image_albums (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    cover_image_id UUID,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Contraintes
    CONSTRAINT fk_cover_image FOREIGN KEY (cover_image_id) 
        REFERENCES generated_images(id) ON DELETE SET NULL,
    CONSTRAINT unique_album_name_per_user UNIQUE(user_id, name)
);

CREATE INDEX idx_image_albums_user_id ON image_albums(user_id);

COMMENT ON TABLE image_albums IS 'Albums pour organiser les images par thème';

-- ═══════════════════════════════════════════════════════════════════════════
-- Table: album_images
-- Description: Table de liaison entre albums et images
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS album_images (
    album_id UUID NOT NULL,
    image_id UUID NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (album_id, image_id),
    
    CONSTRAINT fk_album FOREIGN KEY (album_id) 
        REFERENCES image_albums(id) ON DELETE CASCADE,
    CONSTRAINT fk_image FOREIGN KEY (image_id) 
        REFERENCES generated_images(id) ON DELETE CASCADE
);

CREATE INDEX idx_album_images_album_id ON album_images(album_id);
CREATE INDEX idx_album_images_image_id ON album_images(image_id);

COMMENT ON TABLE album_images IS 'Liaison many-to-many entre albums et images';

-- ═══════════════════════════════════════════════════════════════════════════
-- Triggers pour nettoyage automatique
-- ═══════════════════════════════════════════════════════════════════════════

-- Fonction pour nettoyer les images échouées de plus de 24h
CREATE OR REPLACE FUNCTION cleanup_failed_images()
RETURNS void AS $$
BEGIN
    DELETE FROM generated_images
    WHERE status = 'FAILED'
    AND created_at < NOW() - INTERVAL '24 hours';
END;
$$ LANGUAGE plpgsql;

-- ═══════════════════════════════════════════════════════════════════════════
-- Vues utiles pour le monitoring
-- ═══════════════════════════════════════════════════════════════════════════

-- Vue: Statistiques de génération par utilisateur
CREATE OR REPLACE VIEW user_generation_stats AS
SELECT 
    user_id,
    COUNT(*) as total_images,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') as completed_images,
    COUNT(*) FILTER (WHERE status = 'FAILED') as failed_images,
    COUNT(*) FILTER (WHERE is_favorite = TRUE) as favorite_images,
    SUM(tokens_cost) as total_tokens_spent,
    AVG(generation_time_seconds) FILTER (WHERE status = 'COMPLETED') as avg_generation_time,
    MIN(created_at) as first_image_at,
    MAX(created_at) as last_image_at
FROM generated_images
GROUP BY user_id;

COMMENT ON VIEW user_generation_stats IS 'Statistiques de génération par utilisateur';

-- Vue: File d'attente actuelle
CREATE OR REPLACE VIEW generation_queue_status AS
SELECT 
    status,
    COUNT(*) as count,
    AVG(EXTRACT(EPOCH FROM (NOW() - created_at))) as avg_wait_seconds
FROM generated_images
WHERE status IN ('QUEUED', 'PROCESSING')
GROUP BY status;

COMMENT ON VIEW generation_queue_status IS 'État actuel de la file de génération';

-- ═══════════════════════════════════════════════════════════════════════════
-- Données de test (optionnel)
-- ═══════════════════════════════════════════════════════════════════════════

-- Insertion d'un utilisateur de test (à adapter selon votre module User)
-- INSERT INTO generated_images (user_id, prompt, resolution, status, tokens_cost)
-- VALUES (
--     'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
--     'A beautiful sunset over mountains',
--     '1024x1024',
--     'COMPLETED',
--     20
-- );

-- ═══════════════════════════════════════════════════════════════════════════
-- Permissions (à adapter selon votre configuration)
-- ═══════════════════════════════════════════════════════════════════════════

-- GRANT SELECT, INSERT, UPDATE, DELETE ON generated_images TO nexusai_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON image_albums TO nexusai_app;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON album_images TO nexusai_app;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO nexusai_app;
```

### Script de Migration Flyway

```sql
-- V1__Create_Image_Generation_Schema.sql

-- Contenu identique au script ci-dessus
-- Ce format est utilisé pour Flyway migrations
```

---

## 📦 Installation

### Prérequis

- **Java 21+**
- **Maven 3.9+**
- **Docker & Docker Compose**
- **PostgreSQL 16** (ou via Docker)
- **Kafka** (ou via Docker)
- **GPU NVIDIA** (optionnel, pour le worker Python)

### Installation Rapide

```bash
# 1. Cloner le repository
git clone https://github.com/nexusai/nexus-image-generation.git
cd nexus-image-generation

# 2. Lancer l'infrastructure avec Docker Compose
docker-compose up -d

# 3. Vérifier que tous les services sont démarrés
docker-compose ps

# 4. Créer les tables de base de données
psql -h localhost -U nexusai -d nexusai -f scripts/schema.sql

# 5. Compiler le projet Java
mvn clean install

# 6. Lancer l'application
java -jar nexus-image-api/target/nexus-image-api-1.0.0-SNAPSHOT.jar

# 7. Lancer le worker Python (dans un autre terminal)
cd nexus-image-worker
pip install -r requirements.txt
python worker.py
```

L'API sera accessible sur `http://localhost:8085`

---

## ⚙️ Configuration

### Variables d'Environnement

#### API Java

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/nexusai
SPRING_DATASOURCE_USERNAME=nexusai
SPRING_DATASOURCE_PASSWORD=nexusai123

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# S3 (MinIO ou AWS)
AWS_S3_BUCKET_NAME=nexusai-images
AWS_S3_REGION=eu-west-1
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
AWS_S3_ENDPOINT=http://localhost:9000  # Pour MinIO local

# Services externes
SERVICES_PAYMENT_URL=http://localhost:8081
SERVICES_MODERATION_URL=http://localhost:8089
```

#### Worker Python

```bash
# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=nexusai
DB_USER=nexusai
DB_PASSWORD=nexusai123

# S3
AWS_S3_BUCKET=nexusai-images
AWS_S3_REGION=eu-west-1
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
AWS_S3_ENDPOINT=http://localhost:9000

# GPU (optionnel)
CUDA_VISIBLE_DEVICES=0
```

---

## 🚀 Utilisation

### Exemples d'Appels API

#### 1. Générer une image

```bash
curl -X POST http://localhost:8085/api/v1/images/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "prompt": "A beautiful sunset over mountains, golden hour, cinematic",
    "negative_prompt": "blurry, low quality, distorted",
    "style": "realistic",
    "resolution": "1024x1024",
    "seed": 42
  }'
```

**Réponse:**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "user_id": "user-uuid",
  "prompt": "A beautiful sunset over mountains...",
  "status": "QUEUED",
  "tokens_cost": 20,
  "created_at": "2025-01-20T10:30:00Z"
}
```

#### 2. Récupérer le statut d'une image

```bash
curl http://localhost:8085/api/v1/images/a1b2c3d4-e5f6-7890-abcd-ef1234567890 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Réponse:**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "COMPLETED",
  "storage_url": "https://s3.amazonaws.com/nexusai-images/images/a1/a1b2c3d4.png",
  "thumbnail_url": "https://s3.amazonaws.com/nexusai-images/thumbnails/a1/a1b2c3d4_thumb.png",
  "completed_at": "2025-01-20T10:30:45Z"
}
```

#### 3. Lister mes images

```bash
curl "http://localhost:8085/api/v1/images/user/me?page=0&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 4. Marquer comme favorite

```bash
curl -X POST "http://localhost:8085/api/v1/images/a1b2c3d4-e5f6-7890-abcd-ef1234567890/favorite?isFavorite=true" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 5. Supprimer une image

```bash
curl -X DELETE http://localhost:8085/api/v1/images/a1b2c3d4-e5f6-7890-abcd-ef1234567890 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 📚 API Documentation

### Swagger UI

Accessible sur: `http://localhost:8085/swagger-ui.html`

### Endpoints Principaux

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/images/generate` | Génère une nouvelle image |
| GET | `/api/v1/images/{id}` | Récupère une image |
| DELETE | `/api/v1/images/{id}` | Supprime une image |
| GET | `/api/v1/images/user/me` | Liste mes images |
| GET | `/api/v1/images/favorites` | Liste mes favoris |
| POST | `/api/v1/images/{id}/favorite` | Toggle favorite |
| POST | `/api/v1/albums` | Crée un album |
| GET | `/api/v1/albums/{id}` | Récupère un album |

---

## 🧪 Tests

### Exécuter les Tests Unitaires

```bash
mvn test
```

### Exécuter les Tests d'Intégration

```bash
mvn verify -P integration-tests
```

### Coverage

```bash
mvn jacoco:report

# Rapport disponible dans: target/site/jacoco/index.html
```

### Tests E2E avec Testcontainers

Les tests d'intégration utilisent Testcontainers pour démarrer automatiquement PostgreSQL et Kafka:

```bash
mvn verify -Dtest=ImageGenerationIntegrationTest
```

---

## 🚢 Déploiement

### Déploiement Docker

```bash
# Build des images
docker build -t nexusai/image-api:latest ./nexus-image-api
docker build -t nexusai/image-worker:latest ./nexus-image-worker

# Push vers registry
docker push nexusai/image-api:latest
docker push nexusai/image-worker:latest

# Déploiement
docker-compose -f docker-compose.prod.yml up -d
```

### Déploiement Kubernetes

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: image-generation-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: image-api
  template:
    metadata:
      labels:
        app: image-api
    spec:
      containers:
      - name: api
        image: nexusai/image-api:latest
        ports:
        - containerPort: 8085
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-secrets
              key: url
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: image-generation-worker
spec:
  replicas: 2
  selector:
    matchLabels:
      app: image-worker
  template:
    metadata:
      labels:
        app: image-worker
    spec:
      containers:
      - name: worker
        image: nexusai/image-worker:latest
        env:
        - name: KAFKA_BOOTSTRAP_SERVERS
          value: "kafka-service:9092"
        resources:
          requests:
            memory: "4Gi"
            cpu: "2"
            nvidia.com/gpu: "1"
          limits:
            memory: "8Gi"
            cpu: "4"
            nvidia.com/gpu: "1"
```

---

## 🔧 Troubleshooting

### Problème: Images en timeout (status PROCESSING > 10min)

**Cause:** Worker tombé ou surchargé

**Solution:**
```bash
# Vérifier les logs du worker
docker logs nexus-image-worker

# Redémarrer le worker
docker restart nexus-image-worker

# Lancer le job de cleanup
psql -c "SELECT cleanup_failed_images();"
```

### Problème: Erreur "Insufficient tokens"

**Cause:** Solde de tokens insuffisant

**Solution:**
```bash
# Vérifier le solde
curl http://localhost:8081/api/v1/tokens/balance?userId=USER_ID

# Acheter des tokens via l'API Payment
```

### Problème: Kafka connection refused

**Cause:** Kafka non démarré

**Solution:**
```bash
# Vérifier Kafka
docker-compose ps kafka

# Redémarrer Kafka
docker-compose restart kafka zookeeper
```

### Problème: Out of memory (Worker Python)

**Cause:** Pas assez de RAM pour Stable Diffusion

**Solution:**
```python
# Dans worker.py, réduire la résolution ou activer l'optimisation
self.pipe.enable_attention_slicing()
self.pipe.enable_vae_slicing()
self.pipe.enable_sequential_cpu_offload()  # Offload vers CPU
```

---

## 📊 Monitoring

### Métriques Prometheus

Endpoints disponibles:
- `http://localhost:8085/actuator/prometheus`
- `http://localhost:8085/actuator/metrics`

### Dashboard Grafana

Importer le dashboard: `monitoring/grafana-dashboard.json`

Métriques clés:
- Nombre d'images en file d'attente
- Temps moyen de génération
- Taux d'échec
- Utilisation GPU
- Consommation tokens

---

## 📝 Changelog

### v1.0.0 (2025-01-20)
- ✨ Génération d'images via Stable Diffusion
- ✨ Support multi-résolutions (512x512 à 1024x1536)
- ✨ Système de favoris et albums
- ✨ Intégration Kafka pour async processing
- ✨ Stockage S3/MinIO
- ✨ Tests unitaires et d'intégration complets
- ✨ Documentation OpenAPI complète

---

## 👥 Contributeurs

- **NexusAI Team** - Développement initial

## 📄 Licence

Copyright © 2025 NexusAI. Tous droits réservés.
