# 🔧 Guide d'Utilisation du Module File Parser

## 📋 Vue d'Ensemble

Le **ModuleFileParser** est un outil Java qui parse automatiquement les artifacts de code multi-langage et génère l'arborescence complète du projet.

### Capacités

✅ Parse les fichiers **Java**, **XML**, **YAML**, **Python**, **SQL**, **Markdown**, **Dockerfile**  
✅ Détecte automatiquement le **type** et le **chemin** de chaque fichier  
✅ Crée l'**arborescence Maven** complète  
✅ Génère des **rapports** détaillés  
✅ Support **stdin** pour copier-coller direct

---

## 🚀 Installation & Compilation

### Méthode 1 : Compilation Standalone

```bash
# 1. Créer le dossier
mkdir -p nexusai-tools/src/main/java/com/nexusai/tools

# 2. Copier le fichier ModuleFileParser.java
# (copier le contenu de l'artifact précédent)

# 3. Compiler
javac nexusai-tools/src/main/java/com/nexusai/tools/ModuleFileParser.java

# 4. Créer un JAR exécutable (optionnel)
cd nexusai-tools
jar cfe ModuleFileParser.jar com.nexusai.tools.ModuleFileParser \
    -C src/main/java .
```

### Méthode 2 : Maven Project

```xml
<!-- pom.xml -->
<project>
    <groupId>com.nexusai</groupId>
    <artifactId>module-file-parser</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.nexusai.tools.ModuleFileParser</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

```bash
# Compiler avec Maven
mvn clean package

# Le JAR sera dans target/module-file-parser-1.0.0.jar
```

---

## 📖 Utilisation

### Syntaxe de Base

```bash
java -cp . com.nexusai.tools.ModuleFileParser <outputPath> [artifactPath1] [artifactPath2] ...

# Ou avec le JAR
java -jar ModuleFileParser.jar <outputPath> [artifactPath1] [artifactPath2] ...
```

### Exemples d'Utilisation

#### 1. Parser un fichier artifact

```bash
java -jar ModuleFileParser.jar \
    ./nexus-image-generation \
    artifacts/nexus-image-gen-module.md
```

#### 2. Parser plusieurs artifacts

```bash
java -jar ModuleFileParser.jar \
    ./nexus-image-generation \
    artifacts/part1.md \
    artifacts/part2.md \
    artifacts/part3.md
```

#### 3. Parser avec wildcard

```bash
java -jar ModuleFileParser.jar \
    ./nexus-image-generation \
    artifacts/*.md
```

#### 4. Parser depuis stdin (copier-coller)

```bash
# Lancer sans artifact
java -jar ModuleFileParser.jar ./nexus-image-generation

# Ensuite coller votre contenu
# Terminer avec Ctrl+D (Unix) ou Ctrl+Z (Windows)
```

#### 5. Parser depuis un fichier texte

```bash
cat artifacts/code.txt | java -jar ModuleFileParser.jar ./nexus-image-generation
```

---

## 🎯 Exemple Concret : Module 5

### Étape 1 : Créer les artifacts

Sauvegardez les 4 artifacts créés précédemment :

```bash
mkdir -p artifacts

# Copier les contenus des artifacts dans :
artifacts/module5-structure.md       # Structure & DTOs
artifacts/module5-services.md        # Services & Controllers
artifacts/module5-config-worker.md   # Config & Worker Python
artifacts/module5-tests-sql.md       # Tests & SQL
```

### Étape 2 : Exécuter le parser

```bash
# Créer le dossier de sortie
mkdir -p nexus-image-generation

# Parser tous les artifacts
java -jar ModuleFileParser.jar \
    ./nexus-image-generation \
    artifacts/module5-*.md
```

### Étape 3 : Vérifier la structure générée

```bash
# Voir l'arborescence
tree nexus-image-generation

# Sortie attendue :
# nexus-image-generation/
# ├── pom.xml
# ├── nexus-image-domain/
# │   └── src/main/java/com/nexusai/image/domain/
# │       ├── entity/
# │       │   ├── GeneratedImage.java
# │       │   └── ImageAlbum.java
# │       ├── dto/
# │       │   ├── ImageGenerationRequest.java
# │       │   └── ImageGenerationResponse.java
# │       └── event/
# │           └── ImageGenerationRequestedEvent.java
# ├── nexus-image-infrastructure/
# │   └── src/main/java/com/nexusai/image/infrastructure/
# │       ├── repository/
# │       ├── storage/
# │       └── kafka/
# ├── nexus-image-core/
# ├── nexus-image-api/
# ├── nexus-image-worker/
# ├── scripts/
# ├── docker-compose.yml
# └── README.md
```

### Étape 4 : Consulter le rapport

```bash
# Le rapport est généré automatiquement
cat nexus-image-generation/PARSING_REPORT.md
```

---

## 📊 Rapport Généré

Le parser génère automatiquement un rapport détaillé :

```markdown
# NEXUSAI - MODULE 5 PARSING REPORT

Date: Mon Jan 20 15:30:00 CET 2025
Output path: ./nexus-image-generation

## Statistics

- Total files: 45
- Total lines: 3,842
- Total size: 185.3 KB

## Files by Type

- JAVA: 25 files
- YAML: 4 files
- SQL: 3 files
- PYTHON: 2 files
- XML: 2 files
- MARKDOWN: 5 files
- DOCKERFILE: 2 files
- TEXT: 2 files

## Generated Files

- `docker-compose.yml` (89 lines)
- `nexus-image-api/src/main/java/.../ImageGenerationController.java` (234 lines)
- `nexus-image-api/src/main/resources/application.yml` (67 lines)
- `nexus-image-core/src/main/java/.../ImageGenerationService.java` (189 lines)
- `nexus-image-domain/src/main/java/.../GeneratedImage.java` (78 lines)
- `nexus-image-worker/worker.py` (456 lines)
- `pom.xml` (145 lines)
- `README.md` (523 lines)
- `scripts/schema.sql` (234 lines)
...
```

---

## 🔍 Détection Automatique

### Le parser détecte automatiquement :

#### Fichiers Java
```java
// Détecte le package et la classe
package com.nexusai.image.domain.entity;

public class GeneratedImage {
    // ...
}

// Génère: nexus-image-domain/src/main/java/com/nexusai/image/domain/entity/GeneratedImage.java
```

#### Fichiers Python
```python
# Détecte le worker principal
if __name__ == "__main__":
    worker = ImageGenerationWorker()
    worker.run()

# Génère: nexus-image-worker/worker.py
```

#### Fichiers YAML
```yaml
spring:
  application:
    name: nexus-image-generation

# Génère: src/main/resources/application.yml
```

#### POM.xml
```xml
<artifactId>nexus-image-generation</artifactId>

# Génère: pom.xml
```

---

## 🛠️ Scripts Helper

### Script Bash (Linux/Mac)

```bash
#!/bin/bash
# parse-module5.sh

set -e

OUTPUT_DIR="./nexus-image-generation"
ARTIFACTS_DIR="./artifacts"
PARSER_JAR="./ModuleFileParser.jar"

echo "🚀 Starting Module 5 File Parser"
echo "=================================="
echo ""

# Vérifier que le JAR existe
if [ ! -f "$PARSER_JAR" ]; then
    echo "❌ Error: $PARSER_JAR not found"
    echo "Please compile the parser first:"
    echo "  mvn clean package"
    exit 1
fi

# Créer le dossier de sortie
mkdir -p "$OUTPUT_DIR"

# Parser les artifacts
echo "📖 Parsing artifacts from $ARTIFACTS_DIR..."
java -jar "$PARSER_JAR" "$OUTPUT_DIR" "$ARTIFACTS_DIR"/*.md

# Afficher le rapport
echo ""
echo "📊 Report:"
cat "$OUTPUT_DIR/PARSING_REPORT.md"

echo ""
echo "✅ Done! Files generated in $OUTPUT_DIR"
echo ""
echo "Next steps:"
echo "  1. cd $OUTPUT_DIR"
echo "  2. mvn clean install"
echo "  3. docker-compose up -d"
```

### Script PowerShell (Windows)

```powershell
# parse-module5.ps1

$ErrorActionPreference = "Stop"

$OutputDir = ".\nexus-image-generation"
$ArtifactsDir = ".\artifacts"
$ParserJar = ".\ModuleFileParser.jar"

Write-Host "🚀 Starting Module 5 File Parser" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Green
Write-Host ""

# Vérifier que le JAR existe
if (-not (Test-Path $ParserJar)) {
    Write-Host "❌ Error: $ParserJar not found" -ForegroundColor Red
    Write-Host "Please compile the parser first:"
    Write-Host "  mvn clean package"
    exit 1
}

# Créer le dossier de sortie
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

# Parser les artifacts
Write-Host "📖 Parsing artifacts from $ArtifactsDir..." -ForegroundColor Cyan
$artifacts = Get-ChildItem -Path $ArtifactsDir -Filter "*.md"
$artifactPaths = $artifacts | ForEach-Object { $_.FullName }

java -jar $ParserJar $OutputDir @artifactPaths

# Afficher le rapport
Write-Host ""
Write-Host "📊 Report:" -ForegroundColor Cyan
Get-Content "$OutputDir\PARSING_REPORT.md"

Write-Host ""
Write-Host "✅ Done! Files generated in $OutputDir" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. cd $OutputDir"
Write-Host "  2. mvn clean install"
Write-Host "  3. docker-compose up -d"
```

---

## 🎨 Personnalisation

### Modifier les Patterns de Détection

Si vous voulez personnaliser la détection, modifiez la méthode `detectFileType()` :

```java
private FileInfo detectFileType(String code, String hintLanguage) {
    
    // Ajouter votre pattern personnalisé
    if (code.contains("mon-pattern-special")) {
        return new FileInfo("chemin/custom/fichier.ext", "type");
    }
    
    // ... reste du code
}
```

### Ajouter un Nouveau Type de Fichier

```java
// Dans FILE_PATTERNS
Map.entry("kotlin", Pattern.compile("^package\\s+([a-z.]+).*?class\\s+(\\w+)", 
    Pattern.DOTALL)),
```

---

## ⚠️ Limitations & Workarounds

### 1. Chemins Ambigus

**Problème:** Le parser ne peut pas toujours deviner le chemin exact.

**Solution:** Ajouter un commentaire explicite :

```java
// nexus-image-api/src/main/java/com/nexusai/image/api/MyClass.java
package com.nexusai.image.api;

public class MyClass {
    // ...
}
```

### 2. Fichiers Non-Java

**Problème:** Les fichiers non-Java nécessitent des indices.

**Solution:** Utiliser un nom descriptif ou un commentaire :

```yaml
# application.yml
spring:
  application:
    name: my-app
```

### 3. Plusieurs Fichiers du Même Type

**Problème:** Risque d'écrasement.

**Solution:** Séparer dans différents artifacts ou ajouter des commentaires de chemin.

---

## 🧪 Tests

### Tester le Parser

```bash
# 1. Créer un artifact de test
cat > test-artifact.md << 'EOF'
```java
package com.example.test;

public class TestClass {
    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}
```
EOF

# 2. Parser
java -jar ModuleFileParser.jar ./test-output test-artifact.md

# 3. Vérifier
cat ./test-output/*/src/main/java/com/example/test/TestClass.java
```

---

## 📚 FAQ

### Q: Puis-je parser du code inline sans fichier ?

**R:** Oui, utilisez stdin :

```bash
echo 'package com.test; public class Test {}' | \
    java -jar ModuleFileParser.jar ./output
```

### Q: Comment parser uniquement certains types de fichiers ?

**R:** Utilisez `grep` ou filtrez les artifacts :

```bash
grep -A 50 "```java" artifacts/*.md | \
    java -jar ModuleFileParser.jar ./output
```

### Q: Le parser supporte-t-il d'autres langages ?

**R:** Actuellement : Java, Python, XML, YAML, SQL, Markdown, Dockerfile. 
Pour ajouter un langage, modifiez `FILE_PATTERNS` et `detectFileType()`.

### Q: Que faire si un fichier n'est pas détecté ?

**R:** Ajoutez un commentaire de chemin explicite en première ligne :

```
// mon-module/src/main/java/com/example/MyFile.java
```

---

## 🤝 Contribution

Pour améliorer le parser :

1. Fork le repository
2. Ajouter des patterns dans `FILE_PATTERNS`
3. Améliorer `detectFileType()` pour votre cas d'usage
4. Créer une Pull Request

---

## 📄 Licence

Copyright © 2025 NexusAI. Tous droits réservés.

---

## 📞 Support

- 📧 Email: dev@nexusai.com
- 💬 Slack: #nexusai-tools
- 🐛 Issues: GitHub Issues

---

<div align="center">

**Made with ❤️ by the NexusAI Team**

[⬆ Retour en haut](#-guide-dutilisation-du-module-file-parser)

</div>
