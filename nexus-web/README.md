# Nexus Web

Module principal Spring Boot servant de point d'entrée de l'application.

## Responsabilités

- **Application** : Classe main Spring Boot
- **Configuration** : Configuration globale de l'application
- **Profils** : Gestion des profils (dev, staging, prod)
- **Health Checks** : Endpoints de santé pour monitoring

## Structure

```
nexus-web/
├── src/main/java/com/nexusai/
│   └── NexusAiApplication.java         # Main class
├── src/main/resources/
│   ├── application.yml                  # Config principale
│   ├── application-dev.yml              # Config développement
│   ├── application-staging.yml          # Config staging
│   └── application-prod.yml             # Config production
```

## Démarrage

```java
@SpringBootApplication
@EnableJpaAuditing
public class NexusAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusAiApplication.class, args);
    }
}
```

## Profils

### Développement (dev)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
- Base de données H2 en mémoire (optionnel)
- Logs DEBUG
- Swagger UI activé
- Hot reload

### Staging
```bash
java -jar nexus-web.jar --spring.profiles.active=staging
```
- PostgreSQL staging
- Logs INFO
- Swagger UI activé

### Production (prod)
```bash
java -jar nexus-web.jar --spring.profiles.active=prod
```
- PostgreSQL production
- Logs WARN/ERROR uniquement
- Swagger UI désactivé
- SSL activé

## Configuration

```yaml
# application.yml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  application:
    name: nexusai
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}

nexusai:
  ai:
    ollama:
      base-url: ${OLLAMA_URL}
  security:
    jwt:
      secret: ${JWT_SECRET}
  media:
    minio:
      endpoint: ${MINIO_ENDPOINT}
```

## Health Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | État global |
| `/actuator/health/db` | Base de données |
| `/actuator/health/redis` | Redis |
| `/actuator/health/minio` | MinIO |
| `/actuator/info` | Info application |
| `/actuator/metrics` | Métriques |

## Build

```bash
# Build
mvn clean package -pl nexus-web -am

# Le JAR exécutable
java -jar nexus-web/target/nexus-web-1.0.0.jar
```

## Docker

```dockerfile
FROM eclipse-temurin:21-jre
COPY nexus-web/target/nexus-web-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t nexusai:latest .
docker run -p 8080:8080 --env-file .env nexusai:latest
```

## Dépendances

Ce module agrège tous les autres modules :
- nexus-api
- nexus-auth
- nexus-companion
- nexus-conversation
- nexus-ai-engine
- nexus-media
- nexus-payment
- nexus-moderation
- nexus-analytics

**Dépendances externes** :
- Spring Boot Starter Web
- Spring Boot Actuator
- Spring Boot DevTools (dev only)

## Tests

```bash
# Tests unitaires
mvn test -pl nexus-web

# Tests d'intégration
mvn verify -pl nexus-web
```
