# 🚀 GUIDE D'UTILISATION - GÉNÉRATEUR DE PROJET NEXUSAI

Ce guide explique comment utiliser le **ProjectGenerator** pour générer automatiquement l'arborescence complète du Module 6 Audio.

---

## 📋 TABLE DES MATIÈRES

1. [Prérequis](#prérequis)
2. [Installation](#installation)
3. [Utilisation](#utilisation)
4. [Formats supportés](#formats-supportés)
5. [Exemples](#exemples)
6. [Dépannage](#dépannage)

---

## ✅ PRÉREQUIS

### Logiciels requis

- **Java 21** ou supérieur
- **Maven 3.9+**
- **Git**

### Vérification

```bash
java -version
# openjdk version "21.0.1"

mvn -version
# Apache Maven 3.9.5
```

---

## 📥 INSTALLATION

### Étape 1 : Compiler le générateur

```bash
# Créer le dossier du générateur
mkdir -p nexusai-generator/src/main/java/com/nexusai/tools

# Copier ProjectGenerator.java
# (Le fichier a été créé dans l'artifact précédent)

# Créer le POM
cat > nexusai-generator/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.nexusai</groupId>
    <artifactId>nexusai-generator</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <mainClass>com.nexusai.tools.ProjectGenerator</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

# Compiler
cd nexusai-generator
mvn clean compile
```

### Étape 2 : Préparer le fichier de documentation

Le fichier `nexusai-audio-complete.md` contient tous les fichiers du projet dans un format parsable.

```bash
# Copier le fichier de documentation
cp nexusai-audio-complete.md ~/documents/
```

---

## 🎯 UTILISATION

### Méthode 1 : Ligne de commande

```bash
# Syntaxe
java -cp target/classes com.nexusai.tools.ProjectGenerator \
    <chemin-fichier-documentation> \
    <chemin-sortie>

# Exemple
java -cp target/classes com.nexusai.tools.ProjectGenerator \
    ~/documents/nexusai-audio-complete.md \
    ~/projects/nexus-audio
```

### Méthode 2 : Maven Exec

```bash
mvn exec:java \
    -Dexec.mainClass="com.nexusai.tools.ProjectGenerator" \
    -Dexec.args="~/documents/nexusai-audio-complete.md ~/projects/nexus-audio"
```

### Méthode 3 : Script Shell

Créer un script `generate.sh` :

```bash
#!/bin/bash
# generate.sh

DOC_FILE="$1"
OUTPUT_DIR="$2"

if [ -z "$DOC_FILE" ] || [ -z "$OUTPUT_DIR" ]; then
    echo "Usage: ./generate.sh <doc-file> <output-dir>"
    exit 1
fi

echo "📦 Génération du projet NexusAI Audio..."
echo "📄 Documentation : $DOC_FILE"
echo "📁 Sortie        : $OUTPUT_DIR"
echo ""

java -cp target/classes com.nexusai.tools.ProjectGenerator \
    "$DOC_FILE" \
    "$OUTPUT_DIR"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Génération terminée !"
    echo ""
    echo "Prochaines étapes :"
    echo "  cd $OUTPUT_DIR"
    echo "  docker-compose up -d"
    echo "  mvn clean install"
else
    echo "❌ Erreur lors de la génération"
    exit 1
fi
```

Rendre exécutable :

```bash
chmod +x generate.sh

# Utilisation
./generate.sh nexusai-audio-complete.md ~/projects/nexus-audio
```

---

## 📝 FORMATS SUPPORTÉS

Le générateur reconnaît automatiquement les formats suivants :

### 1. Fichiers Java

```markdown
```java
// nexus-audio-api/src/main/java/com/nexusai/Example.java
package com.nexusai;

public class Example {
    // ...
}
\```
```

**Détection automatique** : Si le package est présent, le chemin est déduit automatiquement.

### 2. Fichiers XML (POM)

```markdown
```xml
<!-- pom.xml -->
<?xml version="1.0"?>
<project>
    <artifactId>nexus-audio</artifactId>
    <!-- ... -->
</project>
\```
```

### 3. Fichiers YAML

```markdown
```yaml
# application.yml
spring:
  application:
    name: my-app
\```
```

### 4. Fichiers SQL

```markdown
```sql
-- V1__create_tables.sql
CREATE TABLE users (
    id UUID PRIMARY KEY
);
\```
```

### 5. Autres fichiers

- **Makefile** (détection automatique)
- **Dockerfile** (détection automatique)
- **README.md** (détection automatique)
- **Shell scripts** (commence par `#!/bin/bash`)
- **.gitignore**

---

## 🎬 EXEMPLE COMPLET

### Scénario : Génération du Module Audio

```bash
# 1. Positionner dans le dossier du générateur
cd nexusai-generator

# 2. Compiler si ce n'est pas déjà fait
mvn clean compile

# 3. Créer le dossier de sortie
mkdir -p ~/projects/nexus-audio

# 4. Générer le projet
java -cp target/classes com.nexusai.tools.ProjectGenerator \
    ../nexusai-audio-complete.md \
    ~/projects/nexus-audio

# Sortie attendue :
# ╔════════════════════════════════════════════════════════╗
# ║   NexusAI Audio Module - Générateur de Projet        ║
# ╚════════════════════════════════════════════════════════╝
# 
# 📖 Lecture du fichier de documentation...
# 🔍 Extraction des fichiers...
#    ✓ 85 fichiers détectés
# 
# 📁 Génération de l'arborescence...
#    ✓ 42 dossiers créés
# 📝 Écriture des fichiers...
#    ✓ 10/85 fichiers écrits...
#    ✓ 20/85 fichiers écrits...
#    ...
#    ✓ 85 fichiers écrits au total
# 
# ═══════════════════════════════════════════════════════
#                     RÉSUMÉ
# ═══════════════════════════════════════════════════════
# 
# Fichiers générés par type :
#   • JAVA            :  45 fichiers
#   • XML             :  10 fichiers
#   • YAML            :   5 fichiers
#   • SQL             :   3 fichiers
#   • MARKDOWN        :   8 fichiers
#   • MAKEFILE        :   1 fichiers
#   • DOCKERFILE      :   1 fichiers
#   • UNKNOWN         :  12 fichiers
# 
# Projet généré dans : ~/projects/nexus-audio
# 
# ═══════════════════════════════════════════════════════
# ✅ Génération terminée avec succès !
# ═══════════════════════════════════════════════════════
# 
# Prochaines étapes :
#   1. cd ~/projects/nexus-audio
#   2. docker-compose up -d
#   3. mvn clean install
#   4. mvn spring-boot:run -pl nexus-audio-api

# 5. Vérifier l'arborescence créée
cd ~/projects/nexus-audio
tree -L 3

# Arborescence attendue :
# nexus-audio/
# ├── pom.xml
# ├── README.md
# ├── Makefile
# ├── Dockerfile
# ├── docker-compose.yml
# ├── .gitignore
# │
# ├── nexus-audio-api/
# │   ├── pom.xml
# │   └── src/
# │       └── main/
# │           ├── java/com/nexusai/audio/api/
# │           │   ├── AudioApplication.java
# │           │   ├── controller/
# │           │   ├── dto/
# │           │   ├── config/
# │           │   └── exception/
# │           └── resources/
# │               └── application.yml
# │
# ├── nexus-audio-core/
# │   ├── pom.xml
# │   └── src/main/java/com/nexusai/audio/core/
# │       ├── domain/
# │       ├── service/
# │       └── exception/
# │
# ├── nexus-audio-persistence/
# │   ├── pom.xml
# │   └── src/main/
# │       ├── java/com/nexusai/audio/persistence/
# │       │   ├── entity/
# │       │   ├── repository/
# │       │   └── mapper/
# │       └── resources/db/migration/
# │           └── V1__create_voice_tables.sql
# │
# ├── nexus-audio-stt/
# ├── nexus-audio-tts/
# ├── nexus-audio-webrtc/
# ├── nexus-audio-storage/
# └── nexus-audio-emotion/

# 6. Démarrer le projet
cd ~/projects/nexus-audio

# Configurer les API keys
export OPENAI_API_KEY=sk-...
export ELEVENLABS_API_KEY=...

# Lancer les services
docker-compose up -d

# Attendre que les services soient prêts (30 secondes)
sleep 30

# Compiler
mvn clean install

# Lancer l'application
mvn spring-boot:run -pl nexus-audio-api

# Dans un autre terminal, tester l'API
curl http://localhost:8083/actuator/health
# {"status":"UP"}

curl http://localhost:8083/swagger-ui.html
# Ouvre la documentation Swagger
```

---

## 🔧 DÉPANNAGE

### Problème 1 : Fichiers non détectés

**Symptôme** : Certains fichiers ne sont pas générés

**Solution** : Vérifier que les commentaires de chemin sont corrects

```markdown
❌ Incorrect :
```java
//MyClass.java (pas de chemin complet)

✅ Correct :
```java
// nexus-audio-api/src/main/java/com/nexusai/MyClass.java
```

### Problème 2 : Erreur de compilation

**Symptôme** : `javac: error: invalid target release: 21`

**Solution** : Vérifier la version de Java

```bash
java -version
# Doit être 21 ou supérieur

# Si inférieur, installer Java 21
# Ubuntu/Debian
sudo apt install openjdk-21-jdk

# macOS
brew install openjdk@21
```

### Problème 3 : Chemins Windows vs Linux

**Symptôme** : Erreurs de chemin sous Windows

**Solution** : Utiliser des chemins absolus ou relatifs corrects

```bash
# Windows (PowerShell)
java -cp target\classes com.nexusai.tools.ProjectGenerator `
    C:\Users\me\nexusai-audio-complete.md `
    C:\projects\nexus-audio

# Windows (Git Bash) - préféré
java -cp target/classes com.nexusai.tools.ProjectGenerator \
    /c/Users/me/nexusai-audio-complete.md \
    /c/projects/nexus-audio
```

### Problème 4 : Dossiers manquants

**Symptôme** : Certains dossiers ne sont pas créés

**Solution** : Le générateur crée automatiquement les dossiers parents. Si le problème persiste :

```bash
# Créer manuellement la structure de base
mkdir -p ~/projects/nexus-audio
cd ~/projects/nexus-audio

# Régénérer
java -cp ../nexusai-generator/target/classes \
    com.nexusai.tools.ProjectGenerator \
    ../nexusai-audio-complete.md \
    .
```

---

## 🎓 PERSONNALISATION

### Modifier le générateur

Pour ajouter le support d'un nouveau type de fichier :

```java
// Dans ProjectGenerator.java

// 1. Ajouter un pattern
private static final Pattern TYPESCRIPT_FILE_PATTERN = 
    Pattern.compile("^//\\s*(.+\\.ts)\\s*$", Pattern.MULTILINE);

// 2. Ajouter un FileType
private enum FileType {
    // ...
    TYPESCRIPT,
    // ...
}

// 3. Ajouter le parsing dans parseDocumentation()
case "typescript", "ts":
    parseTypescriptBlock(content);
    break;

// 4. Implémenter la méthode
private void parseTypescriptBlock(String content) {
    Matcher matcher = TYPESCRIPT_FILE_PATTERN.matcher(content);
    if (matcher.find()) {
        String filePath = matcher.group(1).trim();
        String fileContent = content.substring(matcher.end()).trim();
        filesToGenerate.put(filePath, 
            new FileInfo(filePath, fileContent, FileType.TYPESCRIPT));
    }
}
```

### Créer un nouveau template

Pour créer un template pour un autre module :

1. **Créer un nouveau fichier Markdown** avec tous les fichiers
2. **Utiliser le même format** que `nexusai-audio-complete.md`
3. **Respecter les conventions** de commentaires

Exemple pour un module frontend :

```markdown
# NEXUSAI FRONTEND MODULE

## Fichiers TypeScript

```typescript
// src/components/App.tsx
import React from 'react';

export const App = () => {
  return <div>Hello World</div>;
};
\```

## Fichiers de configuration

```json
// package.json
{
  "name": "nexusai-frontend",
  "version": "1.0.0"
}
\```
```

---

## 📚 RESSOURCES

### Documentation

- [Guide de développement](PLAN_DEVELOPPEMENT.md)
- [Architecture du module](ARCHITECTURE.md)
- [JavaDoc en ligne](http://localhost:8083/javadoc)

### Support

- **Email** : dev@nexusai.com
- **Slack** : #module-audio
- **Issues** : https://github.com/nexusai/nexus-audio/issues

---

## 📄 LICENCE

Copyright © 2025 NexusAI Team

---

**Dernière mise à jour** : 20 Octobre 2025  
**Version** : 1.0.0
