# Nexus Companion

Module de gestion des compagnons IA personnalisés par les utilisateurs.

## Responsabilités

- **CRUD Compagnons** : Création, lecture, mise à jour, suppression des compagnons
- **Personnalité** : Configuration de la personnalité et du style de conversation
- **Avatar** : Gestion des avatars des compagnons
- **Partage** : Fonctionnalités de partage entre utilisateurs (à venir)

## Structure

```
nexus-companion/
├── src/main/java/com/nexusai/companion/
│   ├── service/
│   │   └── CompanionService.java           # Service principal
│   └── dto/
│       ├── CompanionCreateRequest.java     # Création
│       ├── CompanionUpdateRequest.java     # Mise à jour
│       └── CompanionResponse.java          # Réponse API
```

## Modèle Companion

```java
Companion {
    id: UUID
    userId: UUID (propriétaire)
    name: String
    description: String
    personality: JSON {
        traits: ["amical", "curieux", "patient"]
        tone: "décontracté"
        specialties: ["technologie", "musique"]
        customPrompt: "Tu es un assistant..."
    }
    style: CompanionStyle (FRIENDLY, PROFESSIONAL, PLAYFUL, WISE, CREATIVE)
    status: CompanionStatus (DRAFT, ACTIVE, ARCHIVED)
    avatarUrl: String
}
```

## Styles de Compagnon

| Style | Description |
|-------|-------------|
| FRIENDLY | Amical et accessible |
| PROFESSIONAL | Formel et précis |
| PLAYFUL | Ludique et humoristique |
| WISE | Sage et réfléchi |
| CREATIVE | Créatif et imaginatif |

## Utilisation

```java
@Autowired
private CompanionService companionService;

// Créer un compagnon
CompanionResponse companion = companionService.create(userId,
    CompanionCreateRequest.builder()
        .name("Assistant Tech")
        .description("Expert en technologie")
        .style(CompanionStyle.PROFESSIONAL)
        .personality(Map.of(
            "traits", List.of("précis", "pédagogue"),
            "specialties", List.of("programmation", "IA")
        ))
        .build());

// Lister les compagnons de l'utilisateur
Page<CompanionResponse> companions = companionService.findByUser(userId, pageable);
```

## Endpoints (via nexus-api)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/companions` | Créer un compagnon |
| GET | `/api/v1/companions` | Lister mes compagnons |
| GET | `/api/v1/companions/{id}` | Détails d'un compagnon |
| PUT | `/api/v1/companions/{id}` | Modifier |
| DELETE | `/api/v1/companions/{id}` | Supprimer |

## Dépendances

- nexus-commons
- nexus-core

## Tests

```bash
mvn test -pl nexus-companion
```
