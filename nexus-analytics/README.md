# Nexus Analytics

Module d'analytics et métriques pour le suivi de l'utilisation de la plateforme.

## Responsabilités

- **Événements** : Tracking des événements utilisateur
- **Métriques** : Calcul et agrégation de métriques
- **Activité** : Suivi de l'activité utilisateur
- **Rapports** : Génération de rapports (admin)
- **Dashboard** : Données pour tableaux de bord

## Structure

```
nexus-analytics/
├── src/main/java/com/nexusai/analytics/
│   ├── service/
│   │   ├── EventService.java           # Tracking événements
│   │   └── MetricService.java          # Calcul métriques
│   ├── entity/
│   │   ├── AnalyticsEvent.java         # Événement brut
│   │   ├── UserActivity.java           # Activité agrégée
│   │   └── DailyMetric.java            # Métrique journalière
│   ├── repository/
│   │   ├── AnalyticsEventRepository.java
│   │   ├── UserActivityRepository.java
│   │   └── DailyMetricRepository.java
│   └── dto/
│       ├── AnalyticsEventDTO.java
│       ├── TrackEventRequest.java
│       ├── UserActivitySummary.java
│       └── MetricSummary.java
```

## Types d'Événements

| Événement | Description |
|-----------|-------------|
| USER_LOGIN | Connexion utilisateur |
| USER_REGISTER | Inscription |
| CONVERSATION_START | Nouvelle conversation |
| MESSAGE_SENT | Message envoyé |
| COMPANION_CREATED | Création compagnon |
| SUBSCRIPTION_STARTED | Abonnement souscrit |
| SUBSCRIPTION_CANCELLED | Abonnement annulé |

## Utilisation

```java
@Autowired
private EventService eventService;

// Tracker un événement
eventService.track(TrackEventRequest.builder()
    .userId(userId)
    .eventType("MESSAGE_SENT")
    .properties(Map.of(
        "companionId", companionId,
        "messageLength", content.length()
    ))
    .build());

// Récupérer activité utilisateur
UserActivitySummary activity = eventService.getUserActivity(userId, period);
```

## Métriques Disponibles

### Métriques Utilisateur
- Messages envoyés (jour/semaine/mois)
- Temps de conversation
- Compagnons créés
- Tokens consommés

### Métriques Plateforme (Admin)
- Utilisateurs actifs (DAU/WAU/MAU)
- Nouveaux inscrits
- Conversations totales
- Revenue (MRR/ARR)

## Endpoints (via nexus-api)

### Utilisateur
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/analytics/events` | Tracker événement |
| GET | `/api/v1/analytics/me/activity` | Mon activité |
| GET | `/api/v1/analytics/me/usage` | Mon utilisation |

### Admin
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/v1/analytics/dashboard` | Métriques globales |
| GET | `/api/v1/analytics/users` | Stats utilisateurs |
| GET | `/api/v1/analytics/revenue` | Stats revenus |

## Agrégation

Les données sont agrégées par batch :
- **Temps réel** : Compteurs Redis
- **Horaire** : Agrégation vers UserActivity
- **Journalier** : Calcul DailyMetric

## Configuration

```yaml
nexusai:
  analytics:
    event-retention-days: 90
    batch-size: 1000
    aggregation-cron: "0 0 * * * *"  # Toutes les heures
```

## Dépendances

- nexus-commons
- nexus-core

**Dépendances externes** :
- Spring Data JPA
- Redis (compteurs temps réel)

## Tests

```bash
mvn test -pl nexus-analytics
```
