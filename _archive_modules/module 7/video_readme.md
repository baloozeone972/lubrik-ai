# Module 7 : Video Generation Pipeline 🎬

## Vue d'Ensemble

Le Module de Génération Vidéo est un système complet de création de vidéos personnalisées avec des compagnons IA. Il utilise une architecture distribuée basée sur Kafka pour orchestrer un pipeline de génération en 6 phases.

### Caractéristiques Principales

- ✅ Génération automatique de scénarios avec GPT-4
- ✅ Support multi-qualité (Standard 1080p 30fps, HD 1080p 60fps, Ultra 4K 60fps)
- ✅ Pipeline asynchrone avec suivi de progression en temps réel
- ✅ Workers Python distribués avec auto-scaling
- ✅ Gestion intelligente des jetons et coûts
- ✅ Stockage S3 avec CDN pour diffusion rapide
- ✅ API REST complète avec pagination
- ✅ Tests unitaires et d'intégration (>80% coverage)

---

## Architecture

### Diagramme du Pipeline

```
┌────────────────────────────────────────────────────────────────┐
│                     VIDEO GENERATION PIPELINE                  │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  Client Request                                                │
│       │                                                        │
│       ├──→ [REST API]                                          │
│       │     ├─ Validation                                      │
│       │     ├─ Check tokens                                    │
│       │     └─ Save to DB (status: QUEUED)                     │
│       │                                                        │
│       ├──→ [Kafka Topic: video.generation.requests]            │
│       │                                                        │
│       ├──→ [Python Worker]                                     │
│       │     │                                                  │
│       │     ├─ Phase 1: Script Generation (GPT-4)             │
│       │     ├─ Phase 2: Asset Generation (Images + Audio)     │
│       │     ├─ Phase 3: Compositing                           │
│       │     ├─ Phase 4: Rendering (FFmpeg)                    │
│       │     ├─ Phase 5: Encoding (H.265/H.264)                │
│       │     └─ Phase 6: Finalization (S3 Upload)              │
│       │                                                        │
│       ├──→ [Kafka Topic: video.generation.events]             │
│       │     └─ Progress updates                               │
│       │                                                        │
│       └──→ [Event Listener]                                   │
│             ├─ Update DB                                       │
│             └─ Notify user (WebSocket/Push)                   │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

### Stack Technologique

**Backend (Java 21)**
- Spring Boot 3.2+
- Spring Data JPA
- Spring Kafka
- PostgreSQL 16
- Redis (cache)
- AWS SDK S3

**Workers (Python 3.11)**
- Kafka Consumer
- OpenAI API (GPT-4)
- ElevenLabs API (TTS)
- FFmpeg (video processing)
- Boto3 (S3)

---

## Installation

### Prérequis

- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 16
- Kafka 3.x
- Redis 7+
- Python 3.11+ (pour les workers)
- FFmpeg 6.x

### 1. Clone du Repository

```bash
git clone https://github.com/nexusai/video-generation.git
cd video-generation
```

### 2. Configuration des Variables d'Environnement

Créez un fichier `.env` à la racine :

```bash
# Base de données
DATABASE_URL=jdbc:postgresql://localhost:5432/nexusai
DATABASE_USERNAME=nexusai
DATABASE_PASSWORD=nexusai123

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# AWS S3
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_REGION=eu-west-1
S3_BUCKET_VIDEOS=nexusai-videos-prod
S3_BUCKET_ASSETS=nexusai-video-assets-prod

# APIs externes
OPENAI_API_KEY=sk-your-openai-key
ELEVENLABS_API_KEY=your-elevenlabs-key

# Configuration vidéo
VIDEO_COST_BASE_PER_SECOND=5
VIDEO_QUEUE_MAX_SIZE=100
VIDEO_GENERATION_TIMEOUT_MINUTES=60
```

### 3. Création de la Base de Données

```bash
# Se connecter à PostgreSQL
psql -U postgres

# Créer la base
CREATE DATABASE nexusai;
CREATE USER nexusai WITH PASSWORD 'nexusai123';
GRANT ALL PRIVILEGES ON DATABASE nexusai TO nexusai;

# Exécuter les migrations
\c nexusai
\i sql/V1_0__create_video_tables.sql
```

### 4. Build du Projet

```bash
# Build du service Java
cd nexus-video-generation
mvn clean install

# Build des images Docker
docker-compose build
```

### 5. Lancement des Services

```bash
# Démarrer l'infrastructure (PostgreSQL, Kafka, Redis)
docker-compose up -d postgres kafka redis

# Démarrer le service vidéo
docker-compose up -d video-service

# Démarrer les workers (3 instances)
docker-compose up -d video-worker-1 video-worker-2 video-worker-3
```

### 6. Vérification

```bash
# Vérifier que tous les services sont UP
docker-compose ps

# Vérifier les logs
docker-compose logs -f video-service

# Tester l'API
curl http://localhost:8084/actuator/health
```

---

## Utilisation de l'API

### Documentation OpenAPI

Une fois le service démarré, accédez à la documentation interactive :

```
http://localhost:8084/swagger-ui.html
```

### Endpoints Principaux

#### 1. Créer une Génération Vidéo

```http
POST /api/v1/videos/generate
Authorization: Bearer {token}
Content-Type: application/json

{
  "prompt": "Une vidéo de mon compagnon me souhaitant joyeux anniversaire dans un jardin fleuri avec des ballons colorés",
  "companionId": "550e8400-e29b-41d4-a716-446655440000",
  "durationSeconds": 120,
  "quality": "HD",
  "visualStyle": "REALISTIC",
  "musicStyle": "UPBEAT",
  "includeElements": ["ballons", "gâteau", "confettis"]
}
```

**Réponse (201 Created):**

```json
{
  "videoId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "QUEUED",
  "queuePosition": 3,
  "estimatedWaitMinutes": 15,
  "tokensCost": 600,
  "message": "Votre vidéo a été ajoutée à la file d'attente",
  "createdAt": "2025-10-21T14:30:00Z"
}
```

#### 2. Récupérer les Détails d'une Vidéo

```http
GET /api/v1/videos/{videoId}
Authorization: Bearer {token}
```

**Réponse (200 OK):**

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "user-uuid",
  "companionId": "companion-uuid",
  "prompt": "Une vidéo de mon compagnon...",
  "durationSeconds": 120,
  "resolution": "1080p",
  "frameRate": 60,
  "quality": "HD",
  "status": "PROCESSING",
  "currentPhase": "RENDERING",
  "progressPercentage": 75,
  "storageUrl": null,
  "thumbnailUrls": null,
  "fileSizeMb": null,
  "generationTimeMinutes": null,
  "isFavorite": false,
  "tokensCost": 600,
  "errorMessage": null,
  "createdAt": "2025-10-21T14:30:00Z",
  "completedAt": null
}
```

#### 3. Lister les Vidéos de l'Utilisateur

```http
GET /api/v1/videos/user?page=0&size=20
Authorization: Bearer {token}
```

**Réponse (200 OK):**

```json
{
  "videos": [
    {
      "id": "uuid-1",
      "prompt": "Une vidéo de...",
      "durationSeconds": 120,
      "quality": "HD",
      "status": "COMPLETED",
      "progressPercentage": 100,
      "thumbnailUrl": "https://s3.amazonaws.com/...",
      "isFavorite": true,
      "createdAt": "2025-10-21T14:30:00Z"
    }
  ],
  "currentPage": 0,
  "pageSize": 20,
  "totalElements": 156,
  "totalPages": 8,
  "isLast": false
}
```

#### 4. Marquer comme Favori

```http
POST /api/v1/videos/{videoId}/favorite
Authorization: Bearer {token}
```

**Réponse (200 OK):**

```json
{
  "isFavorite": true
}
```

#### 5. Supprimer une Vidéo

```http
DELETE /api/v1/videos/{videoId}
Authorization: Bearer {token}
```

**Réponse (204 No Content)**

#### 6. Statut de la File d'Attente

```http
GET /api/v1/videos/queue-status
```

**Réponse (200 OK):**

```json
{
  "queuedCount": 12,
  "processingCount": 5,
  "activeWorkers": 3,
  "averageWaitMinutes": 8,
  "lastUpdate": "2025-10-21T14:35:00Z"
}
```

---

## Configuration

### Paramètres de Génération

| Paramètre | Description | Valeurs | Défaut |
|-----------|-------------|---------|--------|
| `quality` | Qualité vidéo | STANDARD, HD, ULTRA | STANDARD |
| `durationSeconds` | Durée en secondes | 10-600 | 60 |
| `visualStyle` | Style visuel | REALISTIC, ANIME, ARTISTIC, CINEMATIC | REALISTIC |
| `musicStyle` | Style musical | UPBEAT, CALM, EPIC, ROMANTIC | UPBEAT |

### Coût en Jetons

Le coût est calculé selon la formule :

```
coût = durationSeconds × baseCostPerSecond × qualityMultiplier

Multiplicateurs de qualité:
- STANDARD: ×1
- HD: ×2
- ULTRA: ×5

Exemple:
- 120 secondes en STANDARD = 120 × 5 × 1 = 600 jetons
- 120 secondes en HD = 120 × 5 × 2 = 1200 jetons
- 120 secondes en ULTRA = 120 × 5 × 5 = 3000 jetons
```

### Limites par Plan d'Abonnement

| Plan | Durée Max | Qualité Max | Concurrent |
|------|-----------|-------------|------------|
| FREE | 60s | STANDARD | 1 |
| STANDARD | 180s | HD | 2 |
| PREMIUM | 300s | HD | 3 |
| VIP+ | 600s | ULTRA | 5 |

---

## Monitoring & Métriques

### Prometheus Metrics

Le service expose des métriques Prometheus sur `/actuator/prometheus` :

**Métriques personnalisées:**

```
# Nombre de vidéos en file d'attente
video_generation_queue_size

# Nombre de vidéos en traitement
video_generation_processing_count

# Temps moyen de génération (minutes)
video_generation_avg_time_minutes

# Taux d'erreur
video_generation_error_rate

# Nombre de workers actifs
video_generation_active_workers
```

### Grafana Dashboard

Importez le dashboard fourni dans `monitoring/grafana/video-generation-dashboard.json`

### Logs

Les logs sont disponibles via :

```bash
# Service Java
docker-compose logs -f video-service

# Workers Python
docker-compose logs -f video-worker-1 video-worker-2 video-worker-3

# Logs centralisés (si ELK configuré)
http://localhost:5601/app/kibana#/discover
```

---

## Tests

### Tests Unitaires

```bash
# Lancer tous les tests unitaires
mvn test

# Avec coverage
mvn test jacoco:report

# Voir le rapport
open target/site/jacoco/index.html
```

### Tests d'Intégration

```bash
# Lancer les tests d'intégration (avec TestContainers)
mvn verify -P integration-tests
```

### Tests de Performance

```bash
# Lancer JMeter
jmeter -n -t tests/performance/video-load-test.jmx -l results.jtl

# Ou avec Gatling
mvn gatling:test
```

---

## Déploiement Production

### 1. Build des Images

```bash
# Build et tag des images
docker build -t nexusai/video-service:1.0.0 ./nexus-video-generation
docker build -t nexusai/video-worker:1.0.0 ./video-worker

# Push vers registry
docker push nexusai/video-service:1.0.0
docker push nexusai/video-worker:1.0.0
```

### 2. Déploiement Kubernetes

```bash
# Appliquer les manifests
kubectl apply -f k8s/production/

# Vérifier le déploiement
kubectl get pods -n nexusai-production

# Vérifier les logs
kubectl logs -f deployment/video-service -n nexusai-production
```

### 3. Auto-Scaling

Les workers s'auto-scalent automatiquement selon la charge :

```yaml
# HPA Configuration (k8s/production/video-worker-hpa.yaml)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: video-worker-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: video-worker
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

---

## Troubleshooting

### Problème: Vidéo bloquée en "QUEUED"

**Cause**: Aucun worker actif ou file Kafka pleine

**Solution**:
```bash
# Vérifier les workers
docker-compose ps | grep worker

# Redémarrer les workers
docker-compose restart video-worker-1 video-worker-2 video-worker-3

# Vérifier Kafka
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Problème: Génération échoue avec erreur S3

**Cause**: Credentials AWS invalides ou bucket inexistant

**Solution**:
```bash
# Vérifier les credentials
aws s3 ls s3://nexusai-videos-prod --profile nexusai

# Créer le bucket si nécessaire
aws s3 mb s3://nexusai-videos-prod --region eu-west-1
```

### Problème: Out of Memory dans le worker

**Cause**: Génération 4K nécessite beaucoup de RAM

**Solution**:
```yaml
# Augmenter les ressources dans docker-compose.yml
video-worker-1:
  deploy:
    resources:
      limits:
        memory: 8G
        cpus: '4'
```

---

## Contribution

### Structure du Code

```
nexus-video-generation/
├── src/
│   ├── main/
│   │   ├── java/com/nexusai/video/
│   │   │   ├── controller/        # REST Controllers
│   │   │   ├── service/           # Business Logic
│   │   │   ├── repository/        # Data Access
│   │   │   ├── domain/entity/     # JPA Entities
│   │   │   ├── dto/               # DTOs
│   │   │   ├── messaging/         # Kafka Listeners
│   │   │   ├── config/            # Configuration
│   │   │   └── exception/         # Exceptions
│   │   └── resources/
│   │       ├── application.yml    # Configuration
│   │       └── db/migration/      # SQL Scripts
│   └── test/                      # Tests
├── Dockerfile
└── pom.xml

video-worker/
├── worker.py                      # Worker principal
├── requirements.txt               # Dépendances Python
└── Dockerfile
```

### Guidelines

1. **Code Style**: Suivre Google Java Style Guide
2. **Documentation**: Javadoc obligatoire pour classes publiques
3. **Tests**: Minimum 80% coverage
4. **Commits**: Messages explicites en français
5. **Pull Requests**: Description détaillée + tests

---

## Roadmap

### Version 1.1 (Q2 2025)
- [ ] Support des templates de vidéo prédéfinis
- [ ] Génération de sous-titres automatiques
- [ ] Export multi-formats (MP4, WebM, GIF)

### Version 1.2 (Q3 2025)
- [ ] Éditeur de scénario visuel
- [ ] Effets de transition avancés
- [ ] Support 8K pour VIP+

### Version 2.0 (Q4 2025)
- [ ] Génération en temps réel (streaming)
- [ ] Support multi-compagnons dans une vidéo
- [ ] IA de direction artistique

---

## Licence

Copyright © 2025 NexusAI. Tous droits réservés.

---

## Support

- 📧 Email: support@nexusai.com
- 💬 Discord: https://discord.gg/nexusai
- 📚 Documentation: https://docs.nexusai.com
- 🐛 Issues: https://github.com/nexusai/video-generation/issues

---

**Développé avec ❤️ par l'équipe NexusAI**
