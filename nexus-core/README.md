# Nexus Core

Module central contenant les entités JPA, les enums et les repositories de base pour le domaine métier.

## Responsabilités

- **Entités** : Définition des modèles de données (User, Companion, Conversation, Message)
- **Enums** : Types énumérés métier (UserRole, AccountStatus, CompanionStyle, etc.)
- **Repositories** : Interfaces Spring Data JPA pour l'accès aux données
- **BaseEntity** : Classe de base avec ID UUID et timestamps automatiques

## Structure

```
nexus-core/
├── src/main/java/com/nexusai/core/
│   ├── entity/
│   │   ├── BaseEntity.java            # Entité de base (UUID, timestamps)
│   │   ├── User.java                  # Utilisateur
│   │   ├── Companion.java             # Compagnon IA
│   │   ├── Conversation.java          # Conversation
│   │   ├── Message.java               # Message
│   │   └── MessageAttachment.java     # Pièce jointe
│   ├── enums/
│   │   ├── UserRole.java              # USER, ADMIN, MODERATOR
│   │   ├── AccountStatus.java         # PENDING, ACTIVE, SUSPENDED, DELETED
│   │   ├── CompanionStyle.java        # FRIENDLY, PROFESSIONAL, etc.
│   │   ├── CompanionStatus.java       # DRAFT, ACTIVE, ARCHIVED
│   │   ├── ConversationStatus.java    # ACTIVE, ARCHIVED, DELETED
│   │   ├── MessageRole.java           # USER, ASSISTANT, SYSTEM
│   │   └── MessageType.java           # TEXT, IMAGE, AUDIO, VIDEO
│   └── repository/
│       ├── UserRepository.java
│       ├── CompanionRepository.java
│       ├── ConversationRepository.java
│       └── MessageRepository.java
```

## Modèle de Données

### User
```java
- id (UUID)
- username (unique)
- email (unique)
- passwordHash
- role (UserRole)
- accountStatus (AccountStatus)
- emailVerified (boolean)
- subscriptionType (SubscriptionType)
- createdAt, updatedAt
```

### Companion
```java
- id (UUID)
- userId (owner)
- name
- description
- personality (JSON)
- style (CompanionStyle)
- status (CompanionStatus)
- avatarUrl
```

### Conversation
```java
- id (UUID)
- userId
- companionId
- title
- status (ConversationStatus)
- lastMessageAt
```

### Message
```java
- id (UUID)
- conversationId
- role (MessageRole)
- type (MessageType)
- content
- tokensUsed
- attachments (List<MessageAttachment>)
```

## Dépendances

- nexus-commons

**Dépendances externes** :
- Spring Data JPA
- PostgreSQL
- Lombok

## Tests

```bash
mvn test -pl nexus-core
```
