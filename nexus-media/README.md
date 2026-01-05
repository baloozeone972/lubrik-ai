# Nexus Media

Module de gestion des fichiers médias (images, audio, vidéo) avec stockage MinIO.

## Responsabilités

- **Upload** : Téléchargement sécurisé de fichiers
- **Stockage** : Intégration avec MinIO (S3-compatible)
- **Validation** : Vérification des types et tailles de fichiers
- **URLs** : Génération d'URLs présignées temporaires
- **Métadonnées** : Extraction et stockage des métadonnées

## Structure

```
nexus-media/
├── src/main/java/com/nexusai/media/
│   ├── service/
│   │   └── MediaService.java           # Service principal
│   ├── config/
│   │   └── MinioConfig.java            # Config MinIO
│   └── dto/
│       ├── MediaUploadResponse.java    # Réponse upload
│       └── MediaMetadata.java          # Métadonnées
```

## Types de Médias Supportés

| Type | Extensions | Taille Max |
|------|------------|------------|
| Image | jpg, png, gif, webp | 10 MB |
| Audio | mp3, wav, ogg, m4a | 50 MB |
| Vidéo | mp4, webm, mov | 100 MB |
| Document | pdf | 20 MB |

## Configuration MinIO

```yaml
nexusai:
  media:
    minio:
      endpoint: http://localhost:9000
      access-key: ${MINIO_ACCESS_KEY}
      secret-key: ${MINIO_SECRET_KEY}
      bucket: nexusai-media
    presigned-url-expiration: 3600  # 1 heure
```

## Utilisation

```java
@Autowired
private MediaService mediaService;

// Upload
MediaUploadResponse response = mediaService.upload(
    userId,
    file,           // MultipartFile
    MediaType.IMAGE
);

// Récupérer URL présignée
String url = mediaService.getPresignedUrl(mediaId);

// Supprimer
mediaService.delete(mediaId);
```

## Structure de Stockage

```
nexusai-media/
├── users/
│   └── {userId}/
│       ├── avatars/
│       │   └── {fileId}.jpg
│       └── attachments/
│           └── {conversationId}/
│               └── {fileId}.png
└── companions/
    └── {companionId}/
        └── avatar.jpg
```

## Endpoints (via nexus-api)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/media/upload` | Upload fichier |
| GET | `/api/v1/media/{id}` | Récupérer URL |
| DELETE | `/api/v1/media/{id}` | Supprimer |

## Sécurité

- Validation MIME type côté serveur
- Scan antivirus (optionnel)
- URLs présignées avec expiration
- Contrôle d'accès par propriétaire

## Dépendances

- nexus-commons
- nexus-core

**Dépendances externes** :
- MinIO Java SDK
- Apache Tika (détection MIME)

## Tests

```bash
mvn test -pl nexus-media
```
