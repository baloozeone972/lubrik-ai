# NexusAI - Plateforme d'IA Compagnon

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Proprietary-blue.svg)]()

## Vue d'ensemble

**NexusAI** est une plateforme française d'IA compagnon de nouvelle génération permettant aux utilisateurs de créer, personnaliser et interagir avec des compagnons virtuels intelligents. La plateforme combine des technologies d'IA avancées pour offrir des conversations naturelles, une génération multimédia, et une expérience utilisateur immersive.

### Fonctionnalités principales

- **Compagnons IA personnalisables** : Création de compagnons avec personnalité, apparence et voix configurables
- **Conversations intelligentes** : Dialogue naturel avec mémoire contextuelle et émotionnelle
- **Génération multimédia** : Images, audio et vidéos générés par IA
- **Modération automatique** : Filtrage de contenu et conformité RGPD
- **Système d'abonnement** : Gestion des souscriptions via Stripe
- **Analytics avancés** : Suivi d'utilisation et métriques en temps réel

## Architecture

```
nexusai/
├── nexus-commons/        # Utilitaires et exceptions communes
├── nexus-core/           # Entités JPA et repositories
├── nexus-auth/           # Authentification JWT et gestion utilisateurs
├── nexus-companion/      # Gestion des compagnons IA
├── nexus-conversation/   # Moteur de conversation et messagerie
├── nexus-ai-engine/      # Intégration LLM (Ollama/OpenAI)
├── nexus-media/          # Stockage fichiers (MinIO)
├── nexus-moderation/     # Filtrage contenu et RGPD
├── nexus-analytics/      # Événements et métriques
├── nexus-payment/        # Paiements Stripe
└── nexus-api/            # REST API et contrôleurs
```

## Prérequis

- **Java 21** (Eclipse Temurin recommandé)
- **Maven 3.9+**
- **Docker & Docker Compose**
- **PostgreSQL 16** (via Docker)
- **Redis 7** (via Docker)
- **Kafka** (via Docker)

## Démarrage rapide

### 1. Cloner le repository

```bash
git clone https://github.com/your-org/nexusai.git
cd nexusai
```

### 2. Démarrer l'infrastructure

```bash
# Services de base (PostgreSQL, Redis, Kafka, MinIO, Ollama)
docker-compose up -d

# Avec monitoring (Prometheus, Grafana)
docker-compose --profile monitoring up -d

# Avec outils d'administration (pgAdmin)
docker-compose --profile tools up -d
```

### 3. Télécharger un modèle LLM

```bash
docker exec -it nexusai-ollama ollama pull llama3
```

### 4. Compiler et lancer l'application

```bash
# Compilation
./mvnw clean package -DskipTests

# Lancement en mode développement
./mvnw spring-boot:run -pl nexus-api
```

### 5. Accéder aux services

| Service | URL | Identifiants |
|---------|-----|--------------|
| API NexusAI | http://localhost:8080 | - |
| Swagger UI | http://localhost:8080/swagger-ui.html | - |
| MinIO Console | http://localhost:9001 | minioadmin / minioadmin |
| MailHog | http://localhost:8025 | - |
| pgAdmin | http://localhost:5050 | admin@nexusai.com / admin |
| Grafana | http://localhost:3000 | admin / admin |

## Configuration

### Variables d'environnement principales

```bash
# Base de données
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/nexusai_auth
SPRING_DATASOURCE_USERNAME=nexusai
SPRING_DATASOURCE_PASSWORD=nexusai_password

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PASSWORD=nexusai_redis_password

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# MinIO
NEXUSAI_STORAGE_MINIO_ENDPOINT=http://localhost:9000
NEXUSAI_STORAGE_MINIO_ACCESS_KEY=minioadmin
NEXUSAI_STORAGE_MINIO_SECRET_KEY=minioadmin

# Ollama
NEXUSAI_AI_OLLAMA_BASE_URL=http://localhost:11434

# JWT
NEXUSAI_SECURITY_JWT_SECRET=your-256-bit-secret-key-minimum-32-chars
NEXUSAI_SECURITY_JWT_EXPIRATION=86400000

# Stripe
STRIPE_SECRET_KEY=sk_test_xxx
STRIPE_WEBHOOK_SECRET=whsec_xxx
```

## API Endpoints

### Authentification
- `POST /api/v1/auth/register` - Inscription
- `POST /api/v1/auth/login` - Connexion
- `POST /api/v1/auth/refresh` - Rafraîchir le token
- `POST /api/v1/auth/logout` - Déconnexion

### Compagnons
- `GET /api/v1/companions` - Liste des compagnons
- `POST /api/v1/companions` - Créer un compagnon
- `GET /api/v1/companions/{id}` - Détails d'un compagnon
- `PUT /api/v1/companions/{id}` - Modifier un compagnon
- `DELETE /api/v1/companions/{id}` - Supprimer un compagnon

### Conversations
- `GET /api/v1/conversations` - Liste des conversations
- `POST /api/v1/conversations` - Nouvelle conversation
- `POST /api/v1/conversations/{id}/messages` - Envoyer un message
- `POST /api/v1/conversations/{id}/generate` - Générer une réponse IA
- `GET /api/v1/conversations/{id}/stream` - Streaming SSE

### Médias
- `POST /api/v1/media/upload` - Upload de fichier
- `GET /api/v1/media/{id}` - Télécharger un fichier
- `DELETE /api/v1/media/{id}` - Supprimer un fichier

### Paiements
- `POST /api/v1/payments/subscribe` - Créer un abonnement
- `GET /api/v1/payments/subscription` - État de l'abonnement
- `POST /api/v1/payments/cancel` - Annuler l'abonnement

## Tests

```bash
# Tous les tests
./mvnw test

# Tests d'un module spécifique
./mvnw test -pl nexus-core

# Tests avec couverture
./mvnw test jacoco:report
```

## Structure des modules

| Module | Description | Dépendances |
|--------|-------------|-------------|
| `nexus-commons` | Utilitaires, exceptions, DTOs communs | - |
| `nexus-core` | Entités JPA, repositories, enums | commons |
| `nexus-auth` | JWT, Spring Security, gestion users | core |
| `nexus-companion` | CRUD compagnons, personnalisation | core, auth |
| `nexus-conversation` | Messages, contexte, streaming | core, ai-engine, moderation |
| `nexus-ai-engine` | Ollama/OpenAI, génération texte | core |
| `nexus-media` | MinIO, upload/download fichiers | commons, core |
| `nexus-moderation` | Filtrage contenu, RGPD | core, auth |
| `nexus-analytics` | Events, métriques, Kafka | core |
| `nexus-payment` | Stripe, abonnements | core |
| `nexus-api` | REST controllers, OpenAPI | tous |

## Documentation

- [Architecture Technique](ARCHITECTURE.md)
- [Guide d'exploitation](OPERATIONS.md)
- [Guide Frontend](docs/FRONTEND.md)
- [API Reference](http://localhost:8080/swagger-ui.html)

## Abonnements disponibles

| Plan | Tokens/mois | Compagnons | Prix |
|------|-------------|------------|------|
| FREE | 100 | 1 | Gratuit |
| STANDARD | 1,000 | 3 | 9.99€ |
| PREMIUM | 5,000 | 10 | 19.99€ |
| VIP | 20,000 | 50 | 49.99€ |
| VIP_PLUS | Illimité | Illimité | 99.99€ |

## Technologies utilisées

### Backend
- Java 21, Spring Boot 3.2.5
- Spring Security, JWT
- Spring Data JPA, Hibernate
- Spring WebFlux (streaming)
- PostgreSQL, Redis, Kafka
- MinIO, Ollama

### Infrastructure
- Docker, Docker Compose
- Prometheus, Grafana
- Flyway (migrations)

### Tests
- JUnit 5, Mockito
- AssertJ, MockMvc
- H2 (tests)

## Contribution

1. Fork le repository
2. Créer une branche feature (`git checkout -b feature/amazing-feature`)
3. Commit les changements (`git commit -m 'feat: add amazing feature'`)
4. Push sur la branche (`git push origin feature/amazing-feature`)
5. Ouvrir une Pull Request

## Licence

Propriétaire - Tous droits réservés

## Contact

- **Email**: contact@nexusai.fr
- **Site**: https://nexusai.fr
