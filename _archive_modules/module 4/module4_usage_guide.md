# 🚀 GUIDE D'UTILISATION DU GÉNÉRATEUR DE PROJET

## 📋 Vue d'Ensemble

Le `ProjectFileGenerator` est un outil Java qui parse automatiquement les fichiers de documentation contenant du code et génère la structure complète du projet avec tous les fichiers aux bons emplacements.

---

## 🛠️ Installation & Configuration

### Prérequis
```bash
- Java 21+
- Maven 3.9+
```

### Compilation
```bash
# Compiler le générateur
javac -d target/classes \
  --source 21 \
  src/main/java/com/nexusai/tools/ProjectFileGenerator.java

# Créer un JAR exécutable
jar cfm project-generator.jar \
  manifest.txt \
  -C target/classes .
```

### Manifest (manifest.txt)
```
Manifest-Version: 1.0
Main-Class: com.nexusai.tools.ProjectFileGenerator
```

---

## 📖 Utilisation

### Méthode 1: Ligne de Commande

```bash
# Syntaxe de base
java -jar project-generator.jar <document-path> <output-path>

# Exemple
java -jar project-generator.jar \
  docs/nexusai-complete-docs.md \
  /path/to/output/nexusai-conversation-module
```

### Méthode 2: Utilisation Programmatique

```java
import com.nexusai.tools.ProjectFileGenerator;

public class GenerateProject {
    public static void main(String[] args) throws Exception {
        // Créer le générateur
        ProjectFileGenerator generator = 
            new ProjectFileGenerator("/output/path");
        
        // Parser et générer les fichiers
        generator.parseAndGenerate("docs/nexusai-complete-docs.md");
        
        // Générer la structure Maven
        generator.generateMavenStructure();
        
        // Générer les fichiers de configuration
        generator.generateConfigFiles();
        
        // Générer le rapport
        generator.generateReport();
        
        System.out.println("✅ Projet généré avec succès!");
    }
}
```

### Méthode 3: Builder Pattern

```java
ProjectFileGenerator generator = new ProjectGeneratorBuilder()
    .outputPath("/output/path")
    .withTests(true)
    .withDocs(true)
    .withScripts(true)
    .build();

generator.parseAndGenerate("docs/nexusai-complete-docs.md");
```

---

## 📁 Structure Générée

Après exécution, vous obtiendrez:

```
nexusai-conversation-module/
├── pom.xml                          # POM parent
├── README.md                        # Documentation
├── .gitignore                       # Git ignore
├── docker-compose.yml               # Infrastructure locale
├── GENERATION_REPORT.md             # Rapport de génération
│
├── conversation-common/             # Module DTOs
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/nexusai/conversation/common/
│       │   │       ├── dto/
│       │   │       ├── enums/
│       │   │       └── exceptions/
│       │   └── resources/
│       └── test/
│
├── conversation-api/                # Module REST & WebSocket
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/nexusai/conversation/api/
│       │   │       ├── controller/
│       │   │       ├── websocket/
│       │   │       └── exception/
│       │   └── resources/
│       │       └── application.yml
│       └── test/
│
├── conversation-core/               # Module Business Logic
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   └── java/
│       │       └── com/nexusai/conversation/core/
│       │           ├── service/
│       │           └── events/
│       └── test/
│
├── conversation-llm/                # Module LLM Integration
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   └── java/
│       │       └── com/nexusai/conversation/llm/
│       │           ├── provider/
│       │           ├── prompt/
│       │           └── emotion/
│       └── test/
│
├── conversation-memory/             # Module Memory System
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   └── java/
│       │       └── com/nexusai/conversation/memory/
│       │           ├── shortterm/
│       │           ├── longterm/
│       │           └── embedding/
│       └── test/
│
├── conversation-persistence/        # Module Data Access
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   └── java/
│       │       └── com/nexusai/conversation/persistence/
│       │           ├── entity/
│       │           ├── repository/
│       │           └── mapper/
│       └── test/
│
└── scripts/                         # Scripts utilitaires
    ├── start.sh
    ├── stop.sh
    ├── backup.sh
    └── deploy.sh
```

---

## 🔧 Options Avancées

### Personnalisation de la Détection

Le générateur détecte automatiquement les fichiers via:

1. **Commentaires avec chemin explicite**:
```java
// src/main/java/com/nexusai/Service.java
public class Service { }
```

2. **Package Java**:
```java
package com.nexusai.conversation.core.service;

public class ConversationService { }
// → conversation-core/src/main/java/com/nexusai/conversation/core/service/ConversationService.java
```

3. **Extensions de fichiers**:
- `.java` → Classe Java
- `.xml` → Configuration Maven/XML
- `.yml`, `.yaml` → Configuration YAML
- `.properties` → Properties
- `.sh` → Scripts shell
- `.sql` → Scripts SQL

### Filtrage des Fichiers

```java
ProjectFileGenerator generator = new ProjectFileGenerator("/output");

// Définir des filtres
generator.setFileFilter(path -> {
    // Ignorer les fichiers de test
    if (path.contains("/test/")) {
        return false;
    }
    
    // Ignorer les fichiers temporaires
    if (path.endsWith(".tmp")) {
        return false;
    }
    
    return true;
});

generator.parseAndGenerate("docs.md");
```

---

## 📊 Rapport de Génération

Un rapport détaillé est automatiquement généré:

```markdown
# Rapport de Génération du Projet

## Statistiques

- **Fichiers générés**: 127
- **Répertoires créés**: 45
- **Chemin de sortie**: /output/path

## Fichiers Générés

### conversation-common (15 fichiers)
- `conversation-common/src/main/java/com/nexusai/conversation/common/dto/ConversationDTO.java`
- `conversation-common/src/main/java/com/nexusai/conversation/common/dto/MessageDTO.java`
- ...

### conversation-api (23 fichiers)
- `conversation-api/src/main/java/com/nexusai/conversation/api/controller/ConversationController.java`
- ...
```

---

## 🐛 Dépannage

### Problème: Fichier non détecté

**Solution**: Ajouter un commentaire explicite avec le chemin
```java
// conversation-core/src/main/java/com/nexusai/Service.java
public class Service { }
```

### Problème: Mauvais module assigné

**Solution**: Utiliser le package complet
```java
package com.nexusai.conversation.core.service;
// Sera placé dans conversation-core automatiquement
```

### Problème: Encodage incorrect

**Solution**: Spécifier l'encodage UTF-8
```java
generator.setEncoding(StandardCharsets.UTF_8);
```

### Problème: Permissions scripts

**Solution**: Rendre les scripts exécutables
```bash
chmod +x output/scripts/*.sh
```

---

## 🧪 Tests du Générateur

```java
@Test
public void testGenerateProject() throws Exception {
    // Créer un répertoire temporaire
    Path tempDir = Files.createTempDirectory("test-gen");
    
    // Générer le projet
    ProjectFileGenerator generator = 
        new ProjectFileGenerator(tempDir.toString());
    
    generator.parseAndGenerate("test-docs.md");
    
    // Vérifier que les fichiers existent
    assertTrue(Files.exists(tempDir.resolve("pom.xml")));
    assertTrue(Files.exists(tempDir.resolve("README.md")));
    
    // Cleanup
    FileUtils.deleteDirectory(tempDir.toFile());
}
```

---

## 📦 Build Complet du Projet Généré

Après génération, compiler le projet:

```bash
cd /output/nexusai-conversation-module

# Build complet
mvn clean install

# Tests
mvn test

# Package
mvn package

# Run
mvn spring-boot:run -pl conversation-api
```

---

## 🚀 Déploiement Rapide

Script complet pour générer et déployer:

```bash
#!/bin/bash

# 1. Générer le projet
java -jar project-generator.jar \
  docs/nexusai-complete-docs.md \
  /output/nexusai-conversation-module

cd /output/nexusai-conversation-module

# 2. Démarrer infrastructure
docker-compose up -d

# 3. Build
mvn clean package -DskipTests

# 4. Run
java -jar conversation-api/target/conversation-api-1.0.0.jar
```

---

## 💡 Conseils & Bonnes Pratiques

### 1. Documentation du Code Source
Toujours inclure le chemin complet dans les commentaires:
```java
// conversation-core/src/main/java/com/nexusai/core/Service.java
```

### 2. Conventions de Nommage
Respecter les conventions Maven:
- `src/main/java` - Code source
- `src/test/java` - Tests
- `src/main/resources` - Ressources

### 3. Validation Post-Génération
Vérifier que le build fonctionne:
```bash
mvn clean verify
```

### 4. Version Control
Initialiser Git après génération:
```bash
cd /output/project
git init
git add .
git commit -m "Initial commit - Generated project"
```

### 5. Configuration Environnement
Copier et configurer les variables:
```bash
cp .env.example .env
# Éditer .env avec vos clés API
```

---

## 📚 Exemples Complets

### Exemple 1: Génération Basique

```bash
java -jar project-generator.jar \
  nexusai-complete-docs.md \
  ./output
```

### Exemple 2: Avec Options Avancées

```java
ProjectFileGenerator generator = new ProjectFileGenerator("./output");

// Configurer
generator.setVerbose(true);
generator.setGenerateTests(true);
generator.setGenerateDocs(true);

// Parser plusieurs documents
generator.parseAndGenerate("module4-persistence.md");
generator.parseAndGenerate("module4-core.md");
generator.parseAndGenerate("module4-api.md");

// Générer structure complète
generator.generateMavenStructure();
generator.generateConfigFiles();
generator.generateReport();
```

### Exemple 3: CI/CD Integration

```yaml
# .github/workflows/generate-and-test.yml
name: Generate and Test

on: [push]

jobs:
  generate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
      
      - name: Generate Project
        run: |
          java -jar tools/project-generator.jar \
            docs/nexusai-complete-docs.md \
            ./generated
      
      - name: Build Generated Project
        run: |
          cd generated
          mvn clean package
      
      - name: Upload Artifact
        uses: actions/upload-artifact@v3
        with:
          name: generated-project
          path: generated/
```

---

## 🎯 Résumé

Le `ProjectFileGenerator` vous permet de:

✅ **Générer automatiquement** la structure complète du projet  
✅ **Parser** les fichiers de documentation Markdown  
✅ **Extraire** tout le code source (Java, XML, YAML, etc.)  
✅ **Placer** les fichiers dans la bonne arborescence Maven  
✅ **Créer** les répertoires nécessaires  
✅ **Générer** les fichiers de configuration  
✅ **Produire** un rapport détaillé  

**Gain de temps**: De 2-3 jours de setup manuel à **5 minutes** automatisées ! 🚀

---

*Guide créé le 2025-01-15*  
*Version 1.0.0*  
*NexusAI Project Generator*
