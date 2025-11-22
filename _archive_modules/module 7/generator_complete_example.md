# Guide Complet - Project Structure Generator

## 📋 Vue d'Ensemble

Le **Project Structure Generator** est un outil Java qui parse vos fichiers de documentation (Markdown) contenant des blocs de code et génère automatiquement l'arborescence complète du projet.

### Cas d'Usage Principal

Vous avez reçu une documentation complète du **Module 7 : Video Generation** contenant :
- Classes Java
- Fichiers XML (pom.xml)
- Fichiers YAML (application.yml, docker-compose.yml)
- Scripts SQL
- Workers Python
- Manifests Kubernetes
- Scripts Bash

**Au lieu de créer manuellement chaque fichier**, le générateur le fait automatiquement ! 🚀

---

## 🚀 Installation Rapide

### Étape 1: Créer la Structure du Générateur

```bash
# Créer le répertoire du projet
mkdir -p project-generator/src/main/java/com/nexusai/tools
mkdir -p project-generator/src/main/resources
mkdir -p project-generator/scripts

cd project-generator
```

### Étape 2: Copier les Fichiers

Copiez les 3 artifacts Java dans `src/main/java/com/nexusai/tools/`:
- `ProjectStructureGenerator.java`
- `FileTypeDetector.java`, `CodeBlockParser.java`, `FileStructureWriter.java`
- `GeneratorUsageExample.java`

Copiez le `pom.xml` à la racine du projet.

Copiez les scripts dans `scripts/`:
- `generate-project.sh`
- `generate-project.bat`

### Étape 3: Compiler

```bash
mvn clean package
```

Cela crée le JAR exécutable:
```
target/project-structure-generator-1.0.0-jar-with-dependencies.jar
```

---

## 📝 Préparer le Fichier d'Entrée

### Option 1: Sauvegarder les Artifacts Claude

Copiez tout le contenu des artifacts que j'ai créés dans un fichier:

```bash
# Créer le fichier
touch docs/module-7-complete.md

# Copier le contenu de TOUS les artifacts dedans
# Les artifacts avec IDs:
# - video_module_pom
# - video_entities  
# - video_dtos
# - video_repositories
# - video_controller
# - video_services_aux
# - video_config_tests
# - video_kafka_listeners
# - video_python_worker
# - video_sql_docker
# - video_kubernetes
# - video_cicd
```

### Option 2: Format Attendu

Le générateur comprend ces formats:

#### Format 1: Markdown avec chemin dans commentaire

```markdown
## Classe VideoService

```java
// Fichier: nexus-video-generation/src/main/java/com/nexusai/video/service/VideoService.java
package com.nexusai.video.service;

public class VideoService {
    // ...
}
```
```

#### Format 2: Markdown avec détection automatique

```markdown
```java
package com.nexusai.video.controller;

@RestController
public class VideoController {
    // Le générateur détecte automatiquement le package
    // et place dans: nexus-video-generation/src/main/java/com/nexusai/video/controller/VideoController.java
}
```
```

#### Format 3: Pour fichiers de config

```markdown
```yaml
# Fichier: nexus-video-generation/src/main/resources/application.yml
spring:
  application:
    name: nexus-video-generation
```
```

---

## 🎯 Utilisation

### Méthode 1: Script Shell (Recommandé - Linux/Mac)

```bash
# Rendre exécutable
chmod +x scripts/generate-project.sh

# Usage basique
./scripts/generate-project.sh docs/module-7-complete.md

# Avec sortie personnalisée
./scripts/generate-project.sh -o ~/projects/nexusai docs/module-7-complete.md

# Avec overwrite (écraser fichiers existants)
./scripts/generate-project.sh -w -o ./nexusai docs/module-7-complete.md

# Sans backup
./scripts/generate-project.sh -b docs/module-7-complete.md
```

### Méthode 2: Script Batch (Windows)

```cmd
REM Usage basique
scripts\generate-project.bat docs\module-7-complete.md

REM Avec options
scripts\generate-project.bat -o C:\projects\nexusai -w docs\module-7-complete.md
```

### Méthode 3: Java Direct

```bash
java -jar target/project-structure-generator-1.0.0-jar-with-dependencies.jar \
  docs/module-7-complete.md \
  ./nexusai-output
```

### Méthode 4: En Programmation Java

```java
import com.nexusai.tools.ProjectStructureGenerator;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        ProjectStructureGenerator generator = new ProjectStructureGenerator();
        generator.setOutputPath(Paths.get("./nexusai-project"));
        generator.setOverwriteExisting(false);
        generator.setCreateBackup(true);
        
        generator.parseAndGenerate(Paths.get("docs/module-7-complete.md"));
        
        System.out.println("✓ Projet généré !");
    }
}
```

---

## 📂 Structure Générée

Après exécution, vous obtiendrez:

```
nexusai-output/
├── nexus-video-generation/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/
│       │   │       └── nexusai/
│       │   │           └── video/
│       │   │               ├── controller/
│       │   │               │   └── VideoController.java
│       │   │               ├── service/
│       │   │               │   ├── VideoService.java
│       │   │               │   ├── TokenService.java
│       │   │               │   ├── S3StorageService.java
│       │   │               │   └── VideoOrchestrationService.java
│       │   │               ├── repository/
│       │   │               │   └── GeneratedVideoRepository.java
│       │   │               ├── domain/
│       │   │               │   └── entity/
│       │   │               │       ├── GeneratedVideo.java
│       │   │               │       └── VideoAsset.java
│       │   │               ├── dto/
│       │   │               │   ├── VideoGenerationRequestDto.java
│       │   │               │   ├── VideoGenerationResponseDto.java
│       │   │               │   └── VideoDetailsDto.java
│       │   │               ├── messaging/
│       │   │               │   ├── VideoEventListener.java
│       │   │               │   └── NotificationService.java
│       │   │               ├── config/
│       │   │               │   └── KafkaConfiguration.java
│       │   │               └── exception/
│       │   │                   ├── VideoNotFoundException.java
│       │   │                   └── GlobalExceptionHandler.java
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/
│       │           └── migration/
│       │               └── V1_0__create_video_tables.sql
│       └── test/
│           └── java/
│               └── com/
│                   └── nexusai/
│                       └── video/
│                           ├── VideoServiceTest.java
│                           └── VideoIntegrationTest.java
├── video-worker/
│   ├── worker.py
│   ├── requirements.txt
│   └── Dockerfile
├── k8s/
│   └── production/
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── hpa.yaml
│       └── ingress.yaml
├── scripts/
│   ├── quick-start.sh
│   ├── monitor.sh
│   ├── cleanup.sh
│   └── backup.sh
├── docker-compose.yml
├── Makefile
└── README.md
```

---

## ✅ Vérification

### 1. Vérifier la Structure

```bash
# Afficher l'arborescence
tree -L 4 nexusai-output/

# Ou avec find
find nexusai-output -type f | head -30
```

### 2. Vérifier les Fichiers Java

```bash
# Compter les fichiers Java générés
find nexusai-output -name "*.java" | wc -l

# Vérifier qu'ils compilent
cd nexusai-output/nexus-video-generation
mvn clean compile
```

### 3. Vérifier les Statistiques

Le générateur affiche automatiquement:

```
═══════════════════════════════════════════════════
  STATISTIQUES DE GÉNÉRATION
═══════════════════════════════════════════════════
  total: 87
  java: 23
  yaml: 12
  xml: 3
  sql: 2
  python: 1
  bash: 6
  markdown: 2
  unknown: 0
  errors: 0
═══════════════════════════════════════════════════
```

---

## 🔧 Options Avancées

### Overwrite Mode

Par défaut, le générateur NE PAS écraser les fichiers existants.

```bash
# Forcer l'overwrite
./scripts/generate-project.sh -w docs/module-7.md
```

### Backup Automatique

Par défaut, un backup est créé avant d'écraser:

```
VideoService.java.backup_20250121_143052
```

Pour désactiver:

```bash
./scripts/generate-project.sh -b docs/module-7.md
```

### Traiter Plusieurs Fichiers

```java
List<Path> files = Arrays.asList(
    Paths.get("docs/module-7-backend.md"),
    Paths.get("docs/module-7-frontend.md"),
    Paths.get("docs/module-7-infra.md")
);

generator.parseAndGenerate(files);
```

### Créer Structure Vide d'Abord

```java
FileStructureWriter writer = new FileStructureWriter();
writer.createProjectStructure(Paths.get("./nexusai"));

// Puis générer les fichiers
generator.setOutputPath(Paths.get("./nexusai"));
generator.parseAndGenerate(Paths.get("docs/module-7.md"));
```

---

## 🐛 Troubleshooting

### Problème: Fichiers non générés

**Cause:** Le parser ne trouve pas les blocs de code

**Solution:** Vérifier le format Markdown:

```markdown
✅ CORRECT:
```java
package com.test;
public class Test {}
```

❌ INCORRECT (manque la ligne vide):
```java
package com.test;
```

### Problème: Chemin incorrect

**Cause:** Le générateur ne détecte pas le bon chemin

**Solution:** Ajouter un commentaire explicite:

```java
// Fichier: nexus-video-generation/src/main/java/com/nexusai/Test.java
package com.nexusai;
```

### Problème: Compilation Maven échoue

**Cause:** Lombok non installé

**Solution:**

```bash
# Vérifier Lombok dans le pom.xml
grep -A 5 "lombok" pom.xml

# Réinstaller
mvn clean install
```

---

## 📊 Exemple Complet Étape par Étape

### Étape 1: Préparer l'Environnement

```bash
# Créer workspace
mkdir -p ~/workspace/nexusai-generator
cd ~/workspace/nexusai-generator

# Créer structure
mkdir -p project-generator/{src/main/{java/com/nexusai/tools,resources},scripts}
mkdir -p docs
```

### Étape 2: Installer le Générateur

```bash
cd project-generator

# Copier les fichiers (depuis les artifacts Claude)
# - pom.xml à la racine
# - Classes Java dans src/main/java/com/nexusai/tools/
# - Scripts dans scripts/

# Compiler
mvn clean package
```

### Étape 3: Préparer la Documentation

```bash
cd ..

# Copier tout le contenu des artifacts Module 7 dans:
cat > docs/module-7-all.md << 'EOF'
# Module 7 - Video Generation Complete

[Coller ici tout le contenu des artifacts]
EOF
```

### Étape 4: Générer le Projet

```bash
cd project-generator

# Exécuter
./scripts/generate-project.sh \
  -o ../nexusai-video-module \
  ../docs/module-7-all.md
```

### Étape 5: Vérifier et Tester

```bash
cd ../nexusai-video-module

# Vérifier la structure
tree -L 3

# Compiler le service Java
cd nexus-video-generation
mvn clean test

# Lancer avec Docker
cd ..
docker-compose up -d
```

---

## 🎉 Résultat Final

Vous avez maintenant un projet complet prêt à l'emploi:

✅ **Backend Java Spring Boot** compilable et testable  
✅ **Workers Python** avec toutes les dépendances  
✅ **Configuration Docker** complète  
✅ **Manifests Kubernetes** pour production  
✅ **Scripts** d'administration  
✅ **Tests** unitaires et d'intégration  
✅ **Documentation** README.md  

**Gain de temps: 8-10 heures de création manuelle de fichiers !**

---

## 💡 Conseils

1. **Toujours vérifier les chemins** avant de générer massivement
2. **Utiliser le mode backup** la première fois
3. **Tester avec un petit fichier** d'abord
4. **Vérifier la compilation** après génération
5. **Commiter avant d'overwrite** si vous avez des modifications

---

## 📚 Ressources

- Code source: `project-generator/`
- Documentation: `README.md`
- Exemples: `GeneratorUsageExample.java`
- Tests: `ProjectStructureGeneratorTest.java`

---

**Développé pour NexusAI avec ❤️**
