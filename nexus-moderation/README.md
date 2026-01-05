# Nexus Moderation

Module de modération de contenu et conformité RGPD pour assurer la sécurité de la plateforme.

## Responsabilités

- **Filtrage Contenu** : Détection automatique de contenu inapproprié
- **Signalements** : Gestion des signalements utilisateurs
- **Actions** : Workflow d'actions de modération
- **Incidents** : Suivi et résolution des incidents
- **RGPD** : Conformité et droits des utilisateurs

## Structure

```
nexus-moderation/
├── src/main/java/com/nexusai/moderation/
│   ├── service/
│   │   ├── ModerationService.java      # Service principal
│   │   ├── ContentFilterService.java   # Filtrage automatique
│   │   ├── IncidentService.java        # Gestion incidents
│   │   └── GDPRService.java            # Conformité RGPD
│   ├── entity/
│   │   ├── ContentFlag.java            # Signalement
│   │   └── ModerationAction.java       # Action modération
│   ├── repository/
│   │   ├── ContentFlagRepository.java
│   │   └── ModerationActionRepository.java
│   └── dto/
│       ├── ContentFlagDTO.java
│       ├── ModerationActionDTO.java
│       └── CreateFlagRequest.java
```

## Filtrage Automatique

Le `ContentFilterService` analyse le contenu en temps réel :

```java
ContentAnalysis analysis = contentFilterService.analyze(message);

if (analysis.isFlagged()) {
    // Contenu bloqué ou signalé automatiquement
    List<String> reasons = analysis.getReasons();
    // HATE_SPEECH, VIOLENCE, SEXUAL_CONTENT, etc.
}
```

### Catégories de Filtrage

| Catégorie | Description |
|-----------|-------------|
| HATE_SPEECH | Discours haineux |
| VIOLENCE | Contenu violent |
| SEXUAL_CONTENT | Contenu sexuel explicite |
| HARASSMENT | Harcèlement |
| SPAM | Spam et publicité |
| ILLEGAL | Contenu illégal |
| SELF_HARM | Auto-mutilation |

## Workflow de Modération

```
1. Contenu signalé (auto ou manuel)
   ↓
2. ContentFlag créé (status: PENDING)
   ↓
3. Modérateur examine
   ↓
4. Action: APPROVE, REMOVE, WARN, SUSPEND, BAN
   ↓
5. ModerationAction enregistrée
```

## RGPD - Droits Utilisateurs

```java
@Autowired
private GDPRService gdprService;

// Export des données
byte[] data = gdprService.exportUserData(userId);

// Suppression compte
gdprService.deleteUserData(userId);

// Anonymisation
gdprService.anonymizeUser(userId);
```

## Endpoints (via nexus-api)

### Signalements
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/moderation/flags` | Créer signalement |
| GET | `/api/v1/moderation/flags` | Lister (admin) |
| PUT | `/api/v1/moderation/flags/{id}` | Traiter |

### RGPD
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/v1/users/me/data-export` | Export données |
| DELETE | `/api/v1/users/me` | Suppression compte |

## Configuration

```yaml
nexusai:
  moderation:
    auto-filter:
      enabled: true
      threshold: 0.7        # Score de confiance
    retention:
      flags-days: 365       # Conservation signalements
      actions-days: 730     # Conservation actions
```

## Dépendances

- nexus-commons
- nexus-core

## Tests

```bash
mvn test -pl nexus-moderation
```
