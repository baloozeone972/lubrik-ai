# Nexus AI Engine

Module d'intégration avec les fournisseurs d'IA pour la génération de réponses des compagnons.

## Responsabilités

- **Intégration Ollama** : Communication avec le serveur Ollama local
- **Streaming** : Support des réponses en temps réel (SSE/WebSocket)
- **Abstraction** : Interface commune pour différents providers IA
- **Gestion Contexte** : Préparation des prompts avec personnalité du compagnon

## Structure

```
nexus-ai-engine/
├── src/main/java/com/nexusai/ai/
│   ├── service/
│   │   ├── AIProviderService.java      # Interface provider
│   │   └── OllamaService.java          # Implémentation Ollama
│   └── dto/
│       ├── ChatRequest.java            # Requête interne
│       ├── ChatResponse.java           # Réponse interne
│       ├── OllamaChatRequest.java      # Format Ollama
│       └── OllamaChatResponse.java     # Format Ollama
```

## Interface AIProviderService

```java
public interface AIProviderService {
    ChatResponse chat(ChatRequest request);
    Flux<String> chatStream(ChatRequest request);
    String getProviderName();
    boolean isAvailable();
}
```

## Configuration Ollama

```yaml
nexusai:
  ai:
    ollama:
      base-url: http://localhost:11434
      model: llama3
```

## Modèles Supportés

| Modèle | Taille | Utilisation |
|--------|--------|-------------|
| llama3 | 8B | Par défaut, équilibré |
| llama3:70b | 70B | Haute qualité |
| mistral | 7B | Rapide |
| mixtral | 8x7B | Multi-tâches |

## Utilisation

```java
@Autowired
private OllamaService ollamaService;

// Réponse synchrone
ChatResponse response = ollamaService.chat(ChatRequest.builder()
    .model("llama3")
    .messages(List.of(
        new ChatMessage(MessageRole.SYSTEM, "Tu es un assistant amical."),
        new ChatMessage(MessageRole.USER, "Bonjour!")
    ))
    .build());

// Streaming
Flux<String> stream = ollamaService.chatStream(request);
```

## Extensibilité

Pour ajouter un nouveau provider (OpenAI, Anthropic, etc.) :

1. Implémenter `AIProviderService`
2. Ajouter la configuration correspondante
3. Enregistrer le bean dans le contexte Spring

## Dépendances

- nexus-commons

**Dépendances externes** :
- Spring WebFlux (WebClient réactif)
- Project Reactor

## Tests

```bash
mvn test -pl nexus-ai-engine
```
