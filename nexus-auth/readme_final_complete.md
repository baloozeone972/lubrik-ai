# 🚀 NexusAI - Module User & Authentication

> Module d'authentification et de gestion des utilisateurs pour la plateforme NexusAI.
> Architecture microservices avec Spring Boot 3.2, PostgreSQL 16, Redis 7 et JWT.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](LICENSE)

---

## 📋 Table des Matières

- [Fonctionnalités](#-fonctionnalités)
- [Architecture](#-architecture)
- [Prérequis](#-prérequis)
- [Installation Rapide](#-installation-rapide)
- [Configuration](#️-configuration)
- [Structure du Projet](#-structure-du-projet)
- [API Endpoints](#-api-endpoints)
- [Tests](#-tests)
- [Déploiement](#-déploiement)
- [Monitoring](#-monitoring)
- [Sécurité](#-sécurité)
- [FAQ](#-faq)
- [Support](#-support)

---

## ✨ Fonctionnalités

### 🔐 Authentification
- ✅ Inscription avec validation email
- ✅ Connexion sécurisée avec JWT
- ✅ Refresh tokens avec rotation
- ✅ Réinitialisation de mot de passe
- ✅ Changement de mot de passe
- ✅ Verrouillage de compte après échecs
- ⏳ OAuth2 (Google, Facebook, Apple) - *À venir*
- ⏳ 2FA (TOTP, SMS) - *À venir*

### 👤 Gestion Utilisateurs
- ✅ Profils utilisateurs complets
- ✅ Rôles (USER, MODERATOR, ADMIN)
- ✅ Statistiques utilisateur
- ✅ Historique des actions

### 💳 Abonnements
- ✅ 5 plans (FREE, STANDARD, PREMIUM, VIP, VIP_PLUS)
- ✅ Upgrade/Downgrade avec prorata
- ✅ Renouvellement automatique
- ✅ Annulation immédiate ou différée
- ⏳ Intégration Stripe - *En cours*

### 🪙 Système de Jetons
- ✅ Portefeuille de jetons
- ✅ Achats de jetons
- ✅ Bonus quotidien
- ✅ Historique des transactions
- ✅ Remboursements

### 📊 Audit & Conformité
- ✅ Journal d'audit complet
- ✅ Traçabilité des actions
- ✅ Conformité RGPD
- ✅ Export de données

---

## 🏗️ Architecture

```
nexus-ai-parent/
├── nexus-core/              # Entités et exceptions partagées
│   ├── domain/              # Entités JPA
│   ├── enums/               # Énumérations
│   └── exception/           # Exceptions métier
│
└── nexus-auth/              # Service d'authentification
    ├── config/              # Configurations Spring
    ├── controller/          # Contrôleurs REST
    ├── dto/                 # Data Transfer Objects
    ├── repository/          # Repositories JPA
    ├── service/             # Services métier
    ├── security/            # Sécurité JWT
    ├── mapper/              # Mappers entités/DTOs
    └── resources/
        ├── db/migration/    # Scripts Flyway
        └── application.yml  # Configuration
```

### Technologies

| Composant | Technologie | Version |
|-----------|-------------|---------|
| **Backend** | Java | 21 |
| **Framework** | Spring Boot | 3.2.0 |
| **Sécurité** | Spring Security | 6.x |
| **Database** | PostgreSQL | 16 |
| **Cache** | Redis | 7 |
| **Build** | Maven | 3.9+ |
| **Migrations** | Flyway | 10.x |
| **JWT** | JJWT | 0.12.3 |
| **Documentation** | Springdoc OpenAPI | 2.3.0 |
| **Container** | Docker | 24.x |

---

## 📦 Prérequis

- **Java 21+** (requis)
- **Maven 3.9+** (recommandé)
- **Docker & Docker Compose** (requis)
- **Git** (recommandé)

### Installation des prérequis

#### Java 21 (Adoptium)
```bash
# Linux
wget https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse
tar -xzf eclipse-temurin-21-jdk.tar.gz
export JAVA_HOME=/path/to/jdk-21

# macOS
brew install openjdk@21

# Windows
# Télécharger depuis https://adoptium.net/
```

#### Docker
```bash
# Linux
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# macOS
brew install docker docker-compose

# Windows
# Télécharger Docker Desktop depuis https://www.docker.com/products/docker-desktop/
```

---

## 🚀 Installation Rapide

### Méthode 1 : Script Automatisé (Recommandé)

```bash
# 1. Cloner le repository
git clone https://github.com/nexusai/nexus-auth.git
cd nexus-auth

# 2. Rendre le script exécutable
chmod +x start-nexusai.sh

# 3. Lancer le script
./start-nexusai.sh
```

Le script va :
- ✅ Vérifier les prérequis
- ✅ Créer les répertoires nécessaires
- ✅ Démarrer PostgreSQL et Redis
- ✅ Compiler le projet Maven
- ✅ Démarrer l'application

### Méthode 2 : Manuelle

```bash
# 1. Cloner et naviguer
git clone https://github.com/nexusai/nexus-auth.git
cd nexus-auth

# 2. Démarrer les services Docker
docker-compose up -d

# 3. Attendre que PostgreSQL soit prêt (30 secondes)
sleep 30

# 4. Compiler le projet
mvn clean install

# 5. Démarrer l'application
cd nexus-auth
mvn spring-boot:run
```

### Vérification

Une fois démarré, accédez à :
- **Application** : http://localhost:8081
- **Swagger UI** : http://localhost:8081/swagger-ui.html
- **Health Check** : http://localhost:8081/actuator/health

---

## ⚙️ Configuration

### Variables d'Environnement

Créer un fichier `.env` à la racine :

```env
# Base de données
DATABASE_URL=jdbc:postgresql://localhost:5432/nexusai_auth
DATABASE_USERNAME=nexusai
DATABASE_PASSWORD=nexusai_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT
JWT_SECRET=votre_secret_jwt_change_moi_en_production_256_bits
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Application
SPRING_PROFILE=dev
PORT=8081
APP_URL=http://localhost:3000

# Email (SendGrid)
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=your_sendgrid_api_key

# Stripe (à venir)
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

### Profils Spring

#### Développement (`dev`)
```yaml
spring:
  profiles:
    active: dev
  jpa:
    show-sql: true
logging:
  level:
    com.nexusai: DEBUG
```

#### Production (`prod`)
```yaml
spring:
  profiles:
    active: prod
  jpa:
    show-sql: false
logging:
  level:
    com.nexusai: WARN
```

---

## 📁 Structure du Projet

```
nexus-ai-parent/
│
├── pom.xml                             # Parent POM
├── docker-compose.yml                  # Services Docker
├── start-nexusai.sh                    # Script de démarrage
├── README.md                           # Cette documentation
│
├── nexus-core/                         # Module Core
│   ├── pom.xml
│   └── src/main/java/com/nexusai/core/
│       ├── domain/                     # Entités JPA
│       │   ├── User.java
│       │   ├── Subscription.java
│       │   ├── TokenWallet.java
│       │   ├── TokenTransaction.java
│       │   ├── RefreshToken.java
│       │   ├── EmailVerification.java
│       │   ├── PasswordReset.java
│       │   └── AuditLog.java
│       ├── enums/                      # Énumérations
│       │   ├── UserRole.java
│       │   ├── SubscriptionPlan.java
│       │   ├── TransactionType.java
│       │   ├── EmailVerificationStatus.java
│       │   └── AuditAction.java
│       └── exception/                  # Exceptions
│           ├── ResourceNotFoundException.java
│           ├── UnauthorizedException.java
│           └── ErrorResponse.java
│
└── nexus-auth/                         # Module Auth
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/nexusai/auth/
        │   │   ├── NexusAuthApplication.java
        │   │   ├── config/            # Configurations
        │   │   │   ├── SecurityConfig.java
        │   │   │   ├── JwtConfig.java
        │   │   │   ├── CorsConfig.java
        │   │   │   └── OpenApiConfig.java
        │   │   ├── controller/        # Contrôleurs REST
        │   │   │   ├── AuthController.java
        │   │   │   ├── UserController.java
        │   │   │   ├── SubscriptionController.java
        │   │   │   ├── TokenController.java
        │   │   │   └── AdminController.java
        │   │   ├── dto/              # DTOs
        │   │   │   ├── request/
        │   │   │   └── response/
        │   │   ├── repository/       # Repositories
        │   │   ├── service/          # Services métier
        │   │   │   ├── AuthService.java
        │   │   │   ├── UserService.java
        │   │   │   ├── TokenService.java
        │   │   │   ├── SubscriptionService.java
        │   │   │   └── EmailService.java
        │   │   ├── security/         # Sécurité JWT
        │   │   │   ├── JwtTokenProvider.java
        │   │   │   ├── JwtAuthenticationFilter.java
        │   │   │   └── CustomUserDetails.java
        │   │   ├── mapper/           # Mappers
        │   │   └── exception/        # Gestion erreurs
        │   └── resources/
        │       ├── application.yml
        │       ├── application-dev.yml
        │       ├── application-prod.yml
        │       └── db/migration/     # Scripts Flyway
        │           ├── V1__create_users_table.sql
        │           ├── V2__create_subscriptions_table.sql
        │           ├── V3__create_token_wallets_table.sql
        │           ├── V4__create_auth_tables.sql
        │           ├── V5__create_audit_logs_table.sql
        │           ├── V6__create_functions.sql
        │           └── V7__insert_initial_data.sql
        └── test/
            └── java/com/nexusai/auth/
                ├── controller/
                ├── service/
                └── security/
```

---

## 🌐 API Endpoints

### 📍 Authentication

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `POST` | `/api/v1/auth/register` | Inscription | ❌ |
| `POST` | `/api/v1/auth/login` | Connexion | ❌ |
| `POST` | `/api/v1/auth/refresh` | Rafraîchir token | ❌ |
| `POST` | `/api/v1/auth/logout` | Déconnexion | ✅ |
| `POST` | `/api/v1/auth/verify-email` | Vérifier email | ❌ |
| `POST` | `/api/v1/auth/forgot-password` | Mot de passe oublié | ❌ |
| `POST` | `/api/v1/auth/reset-password` | Réinitialiser MDP | ❌ |
| `POST` | `/api/v1/auth/change-password` | Changer MDP | ✅ |

### 👤 Users

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `GET` | `/api/v1/users/me` | Mon profil | ✅ |
| `PUT` | `/api/v1/users/me` | Modifier profil | ✅ |
| `DELETE` | `/api/v1/users/me` | Supprimer compte | ✅ |
| `GET` | `/api/v1/users/health` | Health check | ❌ |

### 💳 Subscriptions

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `GET` | `/api/v1/subscriptions/plans` | Plans disponibles | ❌ |
| `GET` | `/api/v1/subscriptions/current` | Mon abonnement | ✅ |
| `POST` | `/api/v1/subscriptions/subscribe` | S'abonner | ✅ |
| `POST` | `/api/v1/subscriptions/upgrade` | Upgrade | ✅ |
| `POST` | `/api/v1/subscriptions/downgrade` | Downgrade | ✅ |
| `POST` | `/api/v1/subscriptions/cancel` | Annuler | ✅ |
| `POST` | `/api/v1/subscriptions/reactivate` | Réactiver | ✅ |

### 🪙 Tokens

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `GET` | `/api/v1/tokens/balance` | Solde de jetons | ✅ |
| `GET` | `/api/v1/tokens/statistics` | Statistiques | ✅ |
| `POST` | `/api/v1/tokens/purchase` | Acheter jetons | ✅ |
| `POST` | `/api/v1/tokens/daily-bonus` | Bonus quotidien | ✅ |
| `GET` | `/api/v1/tokens/transactions` | Historique | ✅ |

### 🛡️ Admin

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `GET` | `/api/v1/admin/users` | Liste utilisateurs | 🔐 ADMIN |
| `GET` | `/api/v1/admin/users/{id}` | Détails utilisateur | 🔐 ADMIN |
| `PUT` | `/api/v1/admin/users/{id}/lock` | Verrouiller compte | 🔐 ADMIN |
| `PUT` | `/api/v1/admin/users/{id}/unlock` | Déverrouiller | 🔐 ADMIN |
| `POST` | `/api/v1/admin/tokens/grant` | Accorder jetons | 🔐 ADMIN |
| `GET` | `/api/v1/admin/audit-logs` | Logs d'audit | 🔐 ADMIN |
| `GET` | `/api/v1/admin/statistics` | Statistiques | 🔐 ADMIN |

---

## 🧪 Tests

### Tests Unitaires
```bash
mvn test
```

### Tests d'Intégration
```bash
mvn verify
```

### Couverture de Code
```bash
mvn jacoco:report
# Rapport disponible dans : target/site/jacoco/index.html
```

### Tester l'API avec cURL

#### 1. Inscription
```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "username": "testuser",
    "password": "Test@1234",
    "birthDate": "2000-01-01"
  }'
```

#### 2. Connexion
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "emailOrUsername": "test@example.com",
    "password": "Test@1234"
  }'
```

#### 3. Récupérer son profil
```bash
curl -X GET http://localhost:8081/api/v1/users/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 🚢 Déploiement

### Docker

```bash
# Build l'image
docker build -t nexusai/nexus-auth:1.0.0 .

# Run le conteneur
docker run -p 8081:8081 \
  -e DATABASE_URL=jdbc:postgresql://db:5432/nexusai_auth \
  -e REDIS_HOST=redis \
  nexusai/nexus-auth:1.0.0
```

### Kubernetes

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nexus-auth
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nexus-auth
  template:
    metadata:
      labels:
        app: nexus-auth
    spec:
      containers:
      - name: nexus-auth
        image: nexusai/nexus-auth:1.0.0
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
```

---

## 📊 Monitoring

### Prometheus + Grafana

Les métriques sont exposées sur `/actuator/prometheus` et collectées automatiquement par Prometheus.

Dashboards Grafana disponibles :
- **Application Metrics** : http://localhost:3000
- **JVM Metrics**
- **Database Metrics**
- **API Performance**

---

## 🔒 Sécurité

### Bonnes Pratiques Implémentées

✅ **JWT avec rotation**
✅ **Rate limiting**
✅ **Verrouillage de compte après échecs**
✅ **Chiffrement des mots de passe (BCrypt)**
✅ **CORS configuré**
✅ **Headers de sécurité**
✅ **Audit logging complet**
✅ **Validation des entrées**
✅ **Protection CSRF**

### Changements Requis pour la Production

⚠️ **IMPORTANT** : Avant de déployer en production :

1. Changer le `JWT_SECRET`
2. Utiliser des mots de passe forts pour PostgreSQL/Redis
3. Activer HTTPS/TLS
4. Configurer le SMTP réel
5. Activer le pare-feu
6. Restreindre les CORS

---

## ❓ FAQ

**Q: Comment changer le port de l'application ?**
```bash
export PORT=8082
mvn spring-boot:run
```

**Q: Comment réinitialiser la base de données ?**
```bash
./clean-nexusai.sh
./start-nexusai.sh
```

**Q: Où sont les logs ?**
```
logs/nexus-auth.log
```

**Q: Comment tester en mode production ?**
```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

---

## 📞 Support

- 📧 **Email** : support@nexusai.com
- 🐛 **Issues** : [GitHub Issues](https://github.com/nexusai/issues)
- 📚 **Documentation** : [docs.nexusai.com](https://docs.nexusai.com)
- 💬 **Discord** : [discord.gg/nexusai](https://discord.gg/nexusai)

---

## 📄 Licence

Proprietary © 2025 NexusAI. Tous droits réservés.

---

## 👥 Équipe

Développé avec ❤️ par l'équipe NexusAI

- **Tech Lead** : john.doe@nexusai.com
- **Backend Team** : backend-team@nexusai.com
- **DevOps** : devops@nexusai.com

---

**🎉 Merci d'utiliser NexusAI !**
