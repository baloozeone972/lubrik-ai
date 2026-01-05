# Nexus Conversation

Module de gestion des conversations et messages entre utilisateurs et compagnons IA.

## Responsabilités

- **Conversations** : Création et gestion du cycle de vie des conversations
- **Messages** : Stockage et récupération des messages
- **WebSocket** : Communication temps réel pour le chat
- **Contexte** : Gestion du contexte de conversation pour l'IA
- **Streaming** : Réponses en temps réel de l'IA

## Structure

```
nexus-conversation/
├── src/main/java/com/nexusai/conversation/
│   ├── service/
│   │   ├── ConversationService.java    # Service conversations
│   │   └── ContextService.java         # Gestion contexte IA
│   ├── websocket/
│   │   └── ChatWebSocketHandler.java   # Handler WebSocket
│   ├── config/
│   │   └── WebSocketConfig.java        # Config WebSocket
│   └── dto/
│       ├── ConversationDTO.java
│       ├── MessageDTO.java
│       ├── AttachmentDTO.java
│       ├── CreateConversationRequest.java
│       ├── SendMessageRequest.java
│       └── StreamChunk.java            # Chunk streaming
```

## Flow de Conversation

```
1. Créer conversation → POST /conversations
2. Connecter WebSocket → ws://api/chat/{conversationId}
3. Envoyer message → WebSocket message
4. Recevoir réponse IA → Stream de chunks via WebSocket
5. Archiver → PUT /conversations/{id}/archive
```

## WebSocket Protocol

### Connexion
```
ws://localhost:8080/ws/chat?token={jwt}&conversationId={uuid}
```

### Message Client → Serveur
```json
{
    "type": "MESSAGE",
    "content": "Bonjour, comment ça va ?",
    "attachments": []
}
```

### Message Serveur → Client (Stream)
```json
{
    "type": "CHUNK",
    "content": "Bonjour",
    "done": false
}
{
    "type": "CHUNK",
    "content": " ! Je vais",
    "done": false
}
{
    "type": "DONE",
    "messageId": "uuid",
    "tokensUsed": 45
}
```

## Gestion du Contexte

Le `ContextService` prépare le contexte pour l'IA :

```java
List<ChatMessage> context = contextService.buildContext(conversation, companion);
// Inclut:
// - System prompt avec personnalité du compagnon
// - Historique des messages récents (limité)
// - Nouveau message utilisateur
```

## Endpoints (via nexus-api)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/conversations` | Créer conversation |
| GET | `/api/v1/conversations` | Lister conversations |
| GET | `/api/v1/conversations/{id}` | Détails |
| GET | `/api/v1/conversations/{id}/messages` | Messages |
| POST | `/api/v1/conversations/{id}/messages` | Envoyer (REST) |
| PUT | `/api/v1/conversations/{id}/archive` | Archiver |
| DELETE | `/api/v1/conversations/{id}` | Supprimer |

## Configuration

```yaml
nexusai:
  conversation:
    max-context-messages: 20      # Messages dans le contexte
    max-message-length: 4000      # Caractères max par message
```

## Dépendances

- nexus-commons
- nexus-core
- nexus-ai-engine

**Dépendances externes** :
- Spring WebSocket
- Project Reactor (streaming)

## Tests

```bash
mvn test -pl nexus-conversation
```
