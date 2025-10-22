# 📦 GUIDE D'EXTRACTION DES FICHIERS - MODULE 3

## 🎯 Vue d'Ensemble

Ce guide explique comment extraire automatiquement tous les fichiers du Module 3 depuis les artifacts générés et les placer dans l'arborescence correcte du projet.

---

## 📋 Mapping Artifacts → Fichiers

### Artifact 1: `module3-config` (Configuration Maven & Application)

| Fichier Généré | Chemin de Destination |
|----------------|----------------------|
| `pom.xml` | `pom.xml` |
| `application.yml` | `src/main/resources/application.yml` |

### Artifact 2: `module3-models` (Modèles de Domaine)

| Fichier Généré | Chemin de Destination |
|----------------|----------------------|
| `Companion.java` | `src/main/java/com/nexusai/companion/domain/Companion.java` |
| `CompanionTemplate.java` | `src/main/java/com/nexusai/companion/domain/CompanionTemplate.java` |
| `CompanionLike.java` | `src/main/java/com/nexusai/companion/domain/CompanionLike.java` |

### Artifact 3: `module3-dtos` (Data Transfer Objects)

| Fichier Généré | Chemin de Destination |
|----------------|----------------------|
| `CreateCompanionRequest.java` | `src/main/java/com/nexusai/companion/dto/CreateCompanionRequest.java` |
| `UpdateCompanionRequest.java` | `src/main/java/com/nexusai/companion/dto/UpdateCompanionRequest.java` |
| `CompanionResponse.java` | `src/main/java/com/nexusai/companion/dto/CompanionResponse.java` |
| `AppearanceDto.java` | `src/main/java/com/nexusai/companion/dto/AppearanceDto.java` |
| `PersonalityDto.java` | `src/main/java/com/nexusai/companion/dto/PersonalityDto.java` |
| `TraitsDto.java` | `src/main/java/com/nexusai/companion/dto/TraitsDto.java` |
| `VoiceDto.java` | `src/main/java/com/nexusai/companion/dto/VoiceDto.java` |
| `GeneticProfileDto.java` | `src/main/java/com/nexusai/companion/dto/GeneticProfileDto.java` |
| `EmotionalStateDto.java` | `src/main/java/com/nexusai/companion/dto/EmotionalStateDto.java` |
| `EvolveCompanionRequest.java` | `src/main/java/com/nexusai/companion/dto/EvolveCompanionRequest.java` |
| `FreezeTraitsRequest.java` | `src/main/java/com/nexusai/companion/dto/FreezeTraitsRequest.java` |
| `MergeCompanionsRequest.java` | `src/main/java/com/nexusai/companion/dto/MergeCompanionsRequest.java` |
| `MergeResult.java` | `src/main/java/com/nexusai/companion/dto/MergeResult.java` |
| `PublicCompanionsResponse.java` | `src/main/java/com/nexusai/companion/dto/PublicCompanionsResponse.java` |
| `CompanionSummary.java` | `src/main/java/com/nexusai/companion/dto/CompanionSummary.java` |

### Artifact 4: `module3-repositories` (Accès Données)

| Fichier Généré | Chemin de Destination |
|----------------|----------------------|
| `CompanionRepository.java` | `src/main/java/com/nexusai/companion/repository/CompanionRepository.java` |
| `CompanionTemplateRepository.java` | `src/main/java/com/nexusai/companion/repository/CompanionTemplateRepository.java` |
| `CompanionLikeRepository.java` | `src/main/java/com/nexusai/companion/repository/CompanionLikeRepository.java` |
| `CustomCompanionRepository.java` | `src/main/java/com/nexusai/companion/repository/CustomCompanionRepository.java` |
| `CustomCompanionRepositoryImpl.java` | `src/main/java/com/nexusai/companion/repository/CustomCompanionRepositoryImpl.java` |

### Artifact 5: `module3-services` (Services Métier)

| Fichier Généré | Chemin de Destination |
|----------------|----------------------|
| `CompanionService.java` | `src/main/java/com/nexusai/companion/service/CompanionService.java` |
| `GeneticService.java` | `src/main/java/com/nexusai/companion/service/GeneticService.java` |

### Artifact 6: `module3-services-additional` (Services Additionnels)

| Fichier Généré | Chemin de Destination |
|----------------|----------------------|
| `EvolutionService.java` | `src/main/java/com/nexusai/companion/service/EvolutionService.java` |
| `TemplateService.java` | `src/main/java/com/nexusai/companion/service/TemplateService.java` |
| `LikeService.java` | `src/main/java/com/nexusai/companion/service/LikeService.java` |

### Artifact 7: `module3-controllers` (Contrôleurs REST)

| Fichier Généré | Chemin de Destination |
|----------------|----------------------|
| `CompanionController.java` | `src/main/java/com/nexusai/companion/controller/CompanionController.java` |
| `CompanionEvolutionController.java` | `src/main/java/com/nexusai/companion/controller/CompanionEvolutionController.java` |
| `CompanionTemplateController.java` | `src/main/java/com/nexusai/companion/controller/CompanionTemplateController.java` |
| `CompanionLikeController.java` | `src/main/java/com/nexusai/companion/controller/CompanionLikeController.java` |

### Artifact 8: `module3-utilities` (Services Utilitaires & Exceptions)

| Fichier Généré | Chemin de Destination |
|----------------|----------------------|
| `QuotaService.java` | `src/main/java/com/nexusai/companion/service/QuotaService.java` |
| `EventPublisherService.java` | `src/main/java/com/nexusai/companion/service/EventPublisherService.java` |
| `StorageService.java` | `src/main/java/com/nexusai/companion/service/StorageService.java` |
| `CompanionNotFoundException.java` | `src/main/java/com/nexusai/companion/exception/CompanionNotFoundException.java` |
| `QuotaExceededException.java` | `src/main/java/com/nexusai/companion/exception/QuotaExceededException.java` |
| `DuplicateNameException.java` | `src/main/java/com/nexusai/companion/exception/DuplicateNameException.java` |
| `UnauthorizedException.java` | `src/main/java/com/nexusai/companion/exception/UnauthorizedException.java` |
| `GlobalExceptionHandler.java` | `src/main/java/com/nexusai/companion/exception/GlobalExceptionHandler.java` |
| `CompanionMapper.java` | `src/main/java/com/nexusai/companion/mapper/CompanionMapper.java` |

### Artifact 9: `module3-events-tests` (Événements & Tests)

| Fichier Généré | Chemin de Destination |
|----------------|----------------------|
| `CompanionEvent.java` | `src/main/java/com/nexusai/companion/event/CompanionEvent.java` |
| `EventListenerService.java` | `src/main/java/com/nexusai/companion/event/EventListenerService.java` |
| `CompanionServiceTest.java` | `src/test/java/com/nexusai/companion/service/CompanionServiceTest.java` |
| `GeneticServiceTest.java` | `src/test/java/com/nexusai/companion/service/GeneticServiceTest.java` |

### Artifact 10: `module3-main-readme` (Application & Documentation)

| Fichier Généré | Chemin de Destination |
|----------------|----------------------|
| `CompanionServiceApplication.java` | `src/main/java/com/nexusai/companion/CompanionServiceApplication.java` |
| `SecurityConfig.java` | `src/main/java/com/nexusai/companion/config/SecurityConfig.java` |
| `ScheduledTasks.java` | `src/main/java/com/nexusai/companion/scheduler/ScheduledTasks.java` |
| `README.md` | `README.md` |

### Artifact 11: `module3-deployment` (Docker & Kubernetes)

| Fichier Généré | Chemin de Destination |
|----------------|----------------------|
| `Dockerfile` | `Dockerfile` |
| `docker-compose.yml` | `docker-compose.yml` |
| `mongo-init.js` | `scripts/mongo-init.js` |
| `init-minio.sh` | `scripts/init-minio.sh` |
| `deploy.sh` | `scripts/deploy.sh` |
| `.gitignore` | `.gitignore` |
| `Makefile` | `Makefile` |
| `deployment.yaml` | `kubernetes/deployment.yaml` |

### Artifact 12: `module3-monitoring` (Monitoring & Observabilité)

| Fichier Généré | Chemin de Destination |
|----------------|----------------------|
| `MetricsConfig.java` | `src/main/java/com/nexusai/companion/config/MetricsConfig.java` |
| `CompanionMetrics.java` | `src/main/java/com/nexusai/companion/config/CompanionMetrics.java` |
| `prometheus.yml` | `monitoring/prometheus.yml` |
| `alerts.yml` | `monitoring/alerts.yml` |
| `dashboard.json` | `monitoring/grafana/dashboard.json` |
| `MetricsAspect.java` | `src/main/java/com/nexusai/companion/aspect/MetricsAspect.java` |
| `LoggingAspect.java` | `src/main/java/com/nexusai/companion/aspect/LoggingAspect.java` |
| `logback-spring.xml` | `src/main/resources/logback-spring.xml` |

### Artifact 13: `module3-client-examples` (Exemples Client)

| Fichier Généré | Chemin de Destination |
|----------------|----------------------|
| `companion-client.js` | `client-examples/javascript/companion-client.js` |
| `usage-example.js` | `client-examples/usage-example.js` |
| `load-test.js` | `tests/load-test.js` |
| `integration-test.sh` | `tests/integration-test.sh` |
| `NexusAI-Companion.postman_collection.json` | `postman/NexusAI-Companion.postman_collection.json` |

---

## 🚀 Méthodes d'Extraction

### Méthode 1: Utilisation de ModuleFileExtractor (Recommandé)

#### Étape 1: Compiler l'extracteur

```bash
# Créer le répertoire pour l'extracteur
mkdir -p extractor/src/com/nexusai/companion/util

# Copier ModuleFileExtractor.java
# (depuis l'artifact module3-file-extractor)
cp ModuleFileExtractor.java extractor/src/com/nexusai/companion/util/

# Compiler
cd extractor
javac src/com/nexusai/companion/util/ModuleFileExtractor.java
```

#### Étape 2: Créer un fichier consolidé des artifacts

```bash
# Créer un fichier avec tous les artifacts
cat > all-artifacts.txt << 'EOF'
// FICHIER: pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
...
</project>

// FICHIER: src/main/java/com/nexusai/companion/CompanionServiceApplication.java
package com.nexusai.companion;
...

// ... (copier tout le contenu de tous les artifacts)
EOF
```

#### Étape 3: Exécuter l'extraction

```bash
# Extraction vers le répertoire de destination
java -cp extractor/src com.nexusai.companion.util.ModuleFileExtractor \
  companion-service \
  all-artifacts.txt
```

Ou avec stdin:

```bash
cat all-artifacts.txt | java -cp extractor/src \
  com.nexusai.companion.util.ModuleFileExtractor \
  companion-service
```

#### Étape 4: Vérifier l'arborescence

```bash
cd companion-service
tree -L 3

# Devrait afficher:
# companion-service/
# ├── pom.xml
# ├── src/
# │   ├── main/
# │   │   ├── java/
# │   │   └── resources/
# │   └── test/
# ├── scripts/
# ├── kubernetes/
# └── ...
```

---

### Méthode 2: Extraction Manuelle (Pas à Pas)

#### Pour les fichiers Java:

1. Créer l'arborescence des packages:
```bash
mkdir -p src/main/java/com/nexusai/companion/{domain,dto,repository,service,controller,exception,mapper,config,aspect,scheduler,event}
```

2. Copier chaque fichier .java dans son package respectif

3. Exemple pour Companion.java:
```bash
# Le fichier contient: package com.nexusai.companion.domain;
# Donc le placer dans: src/main/java/com/nexusai/companion/domain/Companion.java
```

#### Pour les fichiers de configuration:

```bash
mkdir -p src/main/resources
# Copier application.yml, logback-spring.xml, etc.
```

#### Pour les scripts:

```bash
mkdir -p scripts
# Copier les .sh et .js
chmod +x scripts/*.sh  # Rendre exécutables
```

#### Pour Docker/Kubernetes:

```bash
mkdir -p kubernetes monitoring
# Copier les fichiers respectifs
```

---

### Méthode 3: Utilisation du CLI (Interface Ligne de Commande)

#### Compiler le CLI:

```bash
javac -cp extractor/src \
  extractor/src/com/nexusai/companion/util/ExtractorCLI.java
```

#### Mode fichier:

```bash
java -cp extractor/src com.nexusai.companion.util.ExtractorCLI \
  --output ./companion-service \
  --input artifacts.txt
```

#### Mode interactif:

```bash
java -cp extractor/src com.nexusai.companion.util.ExtractorCLI \
  --interactive \
  --output ./companion-service

# Puis coller le contenu et terminer avec "END"
```

#### Mode silencieux:

```bash
java -cp extractor/src com.nexusai.companion.util.ExtractorCLI \
  --output ./companion-service \
  --input artifacts.txt \
  --quiet
```

---

## 📝 Format des Artifacts

Les fichiers dans les artifacts suivent ce format:

```java
// FICHIER: chemin/relatif/du/fichier.java
package com.example;

public class MyClass {
    // Code...
}
```

Ou pour d'autres langages:

```yaml
# FICHIER: config/application.yml
server:
  port: 8080
```

L'extracteur détecte automatiquement:
- `// FICHIER:` (Java, JavaScript)
- `# FICHIER:` (YAML, Shell, Python)
- `<!-- FICHIER: -->` (XML, HTML)

---

## ✅ Vérification Post-Extraction

### Checklist:

1. **Structure des répertoires**:
```bash
companion-service/
├── src/main/java/com/nexusai/companion/  ✓
├── src/main/resources/                    ✓
├── src/test/java/                         ✓
├── scripts/                               ✓
├── kubernetes/                            ✓
└── monitoring/                            ✓
```

2. **Compilation Maven**:
```bash
cd companion-service
mvn clean compile
# Devrait réussir sans erreurs
```

3. **Tests**:
```bash
mvn test
# Tous les tests devraient passer
```

4. **Permissions scripts**:
```bash
ls -l scripts/*.sh
# Devraient être exécutables (rwxr-xr-x)
```

---

## 🐛 Dépannage

### Problème: Fichiers manquants

**Solution**: Vérifier que tous les artifacts ont été inclus dans le fichier consolidé.

### Problème: Erreurs de compilation

**Solution**: Vérifier que les packages Java correspondent bien aux chemins de fichiers:
- Package: `com.nexusai.companion.domain`
- Chemin: `src/main/java/com/nexusai/companion/domain/`

### Problème: Scripts non exécutables

**Solution**:
```bash
chmod +x scripts/*.sh
```

### Problème: Caractères spéciaux

**Solution**: S'assurer que tous les fichiers sont en UTF-8:
```bash
file -i src/main/java/com/nexusai/companion/**/*.java
```

---

## 📞 Support

Pour toute question sur l'extraction:
- Email: dev@nexusai.com
- Issues: https://github.com/nexusai/companion-service/issues

---

**Version du Guide**: 1.0.0  
**Dernière Mise à Jour**: 18 Octobre 2025