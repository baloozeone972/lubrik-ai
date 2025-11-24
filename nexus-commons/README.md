# Nexus Commons

Module de bibliothèque partagée contenant les utilitaires communs et les classes de base utilisés par tous les autres modules.

## Responsabilités

- **Exceptions** : Hiérarchie d'exceptions personnalisées (NexusException, BusinessException, ValidationException, ResourceNotFoundException)
- **Utilitaires** : Classes utilitaires partagées (JsonUtils, DateTimeUtils, StringUtils)
- **Constants** : Constantes communes à l'application

## Structure

```
nexus-commons/
├── src/main/java/com/nexusai/commons/
│   ├── exception/
│   │   ├── NexusException.java         # Exception de base
│   │   ├── BusinessException.java      # Erreurs métier
│   │   ├── ValidationException.java    # Erreurs de validation
│   │   └── ResourceNotFoundException.java
│   └── util/
│       ├── JsonUtils.java              # Sérialisation JSON
│       ├── DateTimeUtils.java          # Manipulation dates
│       └── StringUtils.java            # Manipulation chaînes
```

## Dépendances

Ce module est la base de tous les autres modules et n'a aucune dépendance interne.

**Dépendances externes** :
- Lombok
- Jackson (JSON)

## Utilisation

```java
// Exceptions
throw new BusinessException("USER_NOT_FOUND", "User not found");
throw new ValidationException("email", "Invalid email format");

// Utilitaires
String json = JsonUtils.toJson(object);
LocalDateTime date = DateTimeUtils.parseISO("2024-01-15T10:30:00Z");
```

## Tests

```bash
mvn test -pl nexus-commons
```
