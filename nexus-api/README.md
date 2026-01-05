# Nexus API

Module REST API exposant tous les endpoints de l'application. Point d'entrée principal pour les clients.

## Responsabilités

- **Controllers** : Endpoints REST pour toutes les fonctionnalités
- **OpenAPI** : Documentation Swagger automatique
- **Rate Limiting** : Protection contre les abus
- **Exception Handling** : Gestion globale des erreurs
- **Audit** : Logging des actions sensibles

## Structure

```
nexus-api/
├── src/main/java/com/nexusai/api/
│   ├── controller/
│   │   ├── AuthController.java         # /api/v1/auth/*
│   │   ├── UserController.java         # /api/v1/users/*
│   │   ├── CompanionController.java    # /api/v1/companions/*
│   │   ├── ConversationController.java # /api/v1/conversations/*
│   │   ├── MediaController.java        # /api/v1/media/*
│   │   ├── PaymentController.java      # /api/v1/payments/*
│   │   ├── ModerationController.java   # /api/v1/moderation/*
│   │   └── AnalyticsController.java    # /api/v1/analytics/*
│   ├── config/
│   │   └── OpenApiConfig.java          # Config Swagger
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java # Handler global
│   │   └── ErrorResponse.java          # Format erreur
│   └── security/
│       ├── ratelimit/
│       │   └── RateLimitInterceptor.java
│       └── audit/
│           ├── Auditable.java          # Annotation audit
│           └── AuditAspect.java        # Aspect AOP
```

## Endpoints Principaux

### Authentification
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
```

### Utilisateurs
```
GET    /api/v1/users/me
PUT    /api/v1/users/me
DELETE /api/v1/users/me
```

### Compagnons
```
POST   /api/v1/companions
GET    /api/v1/companions
GET    /api/v1/companions/{id}
PUT    /api/v1/companions/{id}
DELETE /api/v1/companions/{id}
```

### Conversations
```
POST   /api/v1/conversations
GET    /api/v1/conversations
GET    /api/v1/conversations/{id}
GET    /api/v1/conversations/{id}/messages
POST   /api/v1/conversations/{id}/messages
```

### Médias
```
POST   /api/v1/media/upload
GET    /api/v1/media/{id}
DELETE /api/v1/media/{id}
```

### Paiements
```
POST   /api/v1/payments/subscribe
GET    /api/v1/payments/subscription
POST   /api/v1/payments/cancel
GET    /api/v1/payments/invoices
```

## Documentation API

Swagger UI disponible à : `http://localhost:8080/swagger-ui.html`

OpenAPI JSON : `http://localhost:8080/v3/api-docs`

## Rate Limiting

```java
@RateLimit(requests = 100, period = 60) // 100 req/min
@PostMapping("/messages")
public ResponseEntity<?> sendMessage(...) { }
```

## Audit

```java
@Auditable(action = "DELETE_ACCOUNT")
@DeleteMapping("/me")
public ResponseEntity<?> deleteAccount(...) { }
```

## Format des Erreurs

```json
{
    "timestamp": "2024-01-15T10:30:00Z",
    "status": 400,
    "error": "Bad Request",
    "code": "VALIDATION_ERROR",
    "message": "Invalid email format",
    "path": "/api/v1/auth/register"
}
```

## Dépendances

- nexus-commons
- nexus-core
- nexus-auth
- nexus-companion
- nexus-conversation
- nexus-media
- nexus-payment
- nexus-moderation
- nexus-analytics

**Dépendances externes** :
- Spring Web
- SpringDoc OpenAPI
- Spring AOP

## Tests

```bash
mvn test -pl nexus-api
```
