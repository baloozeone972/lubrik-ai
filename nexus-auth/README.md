# Nexus Auth

Module d'authentification et d'autorisation gérant la sécurité de l'application.

## Responsabilités

- **Authentification** : Login/Register avec email et mot de passe
- **JWT** : Génération et validation des tokens (access + refresh)
- **Sécurité Spring** : Configuration Spring Security
- **Vérification Email** : Envoi et validation des emails de vérification
- **Sessions** : Gestion des refresh tokens et déconnexion multi-appareils

## Structure

```
nexus-auth/
├── src/main/java/com/nexusai/auth/
│   ├── service/
│   │   ├── AuthenticationService.java      # Service principal auth
│   │   ├── EmailVerificationService.java   # Vérification email
│   │   └── RefreshTokenService.java        # Gestion refresh tokens
│   ├── security/
│   │   ├── JwtTokenProvider.java           # Génération/validation JWT
│   │   ├── JwtAuthenticationFilter.java    # Filtre auth HTTP
│   │   └── UserPrincipal.java              # Détails utilisateur
│   ├── config/
│   │   └── SecurityConfig.java             # Config Spring Security
│   └── dto/
│       ├── AuthRequest.java                # Login request
│       ├── RegisterRequest.java            # Register request
│       └── AuthResponse.java               # Response avec tokens
```

## Endpoints

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/auth/register` | Inscription |
| POST | `/api/v1/auth/login` | Connexion |
| POST | `/api/v1/auth/refresh` | Rafraîchir token |
| POST | `/api/v1/auth/logout` | Déconnexion |
| POST | `/api/v1/auth/logout-all` | Déconnexion tous appareils |
| GET | `/api/v1/auth/verify-email` | Vérifier email |

## Flow d'Authentification

```
1. Register/Login → AuthResponse(accessToken, refreshToken)
2. API Calls → Header: Authorization: Bearer {accessToken}
3. Token expiré → POST /refresh avec refreshToken
4. Logout → Révocation du refreshToken
```

## Configuration

```yaml
nexusai:
  security:
    jwt:
      secret: ${JWT_SECRET}
      access-token-expiration: 900000    # 15 minutes
      refresh-token-expiration: 604800000 # 7 jours
```

## Dépendances

- nexus-commons
- nexus-core

**Dépendances externes** :
- Spring Security
- JWT (jjwt)
- Spring Mail (pour vérification email)

## Tests

```bash
mvn test -pl nexus-auth
```
