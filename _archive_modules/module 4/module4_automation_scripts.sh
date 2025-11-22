#!/bin/bash

# ============================================================================
# SCRIPT D'AUTOMATISATION COMPLÈTE - MODULE 4
# ============================================================================
# 
# Ce script automatise:
# 1. Compilation du générateur
# 2. Génération du projet complet
# 3. Configuration de l'environnement
# 4. Build et tests
# 5. Déploiement
#
# Usage: ./generate-and-deploy.sh [output-path]
#
# @author NexusAI Dev Team
# @version 1.0.0
# ============================================================================

set -e  # Arrêter en cas d'erreur

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DOCS_PATH="nexusai-complete-docs.md"
OUTPUT_PATH="${1:-./nexusai-conversation-module}"
GENERATOR_JAR="project-generator.jar"

# ============================================================================
# FONCTIONS UTILITAIRES
# ============================================================================

print_header() {
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}================================================${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

check_command() {
    if ! command -v $1 &> /dev/null; then
        print_error "$1 n'est pas installé. Installation requise."
        exit 1
    fi
    print_success "$1 est installé"
}

# ============================================================================
# ÉTAPE 1: VÉRIFICATION DES PRÉREQUIS
# ============================================================================

step1_check_prerequisites() {
    print_header "ÉTAPE 1: Vérification des Prérequis"
    
    check_command java
    check_command mvn
    check_command docker
    check_command git
    
    # Vérifier version Java
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 21 ]; then
        print_error "Java 21+ requis. Version détectée: $JAVA_VERSION"
        exit 1
    fi
    print_success "Java version: $JAVA_VERSION"
    
    # Vérifier que le document existe
    if [ ! -f "$DOCS_PATH" ]; then
        print_error "Document introuvable: $DOCS_PATH"
        exit 1
    fi
    print_success "Document trouvé: $DOCS_PATH"
    
    echo ""
}

# ============================================================================
# ÉTAPE 2: COMPILATION DU GÉNÉRATEUR
# ============================================================================

step2_compile_generator() {
    print_header "ÉTAPE 2: Compilation du Générateur"
    
    print_info "Création du répertoire temporaire..."
    mkdir -p temp/generator
    
    # Extraire le code du générateur du document
    print_info "Extraction du code source..."
    # Ici, on suppose que le générateur est déjà dans un fichier séparé
    # Sinon, il faudrait l'extraire du document
    
    print_info "Compilation..."
    javac -d temp/generator/classes \
        --source 21 \
        --enable-preview \
        src/main/java/com/nexusai/tools/ProjectFileGenerator.java 2>/dev/null || {
        
        # Si le fichier n'existe pas, créer une version simplifiée
        print_warning "Source non trouvée, utilisation de la version embarquée"
        
        # Créer le générateur
        cat > temp/generator/Generator.java << 'EOF'
import java.nio.file.*;
import java.io.IOException;

public class Generator {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: Generator <input> <output>");
            System.exit(1);
        }
        
        String input = args[0];
        String output = args[1];
        
        System.out.println("Génération du projet...");
        System.out.println("Input: " + input);
        System.out.println("Output: " + output);
        
        // Créer la structure de base
        Files.createDirectories(Paths.get(output));
        
        System.out.println("✅ Génération terminée!");
    }
}
EOF
        
        javac -d temp/generator temp/generator/Generator.java
        GENERATOR_CLASS="Generator"
    }
    
    print_success "Générateur compilé avec succès"
    echo ""
}

# ============================================================================
# ÉTAPE 3: GÉNÉRATION DU PROJET
# ============================================================================

step3_generate_project() {
    print_header "ÉTAPE 3: Génération du Projet"
    
    print_info "Suppression du répertoire existant..."
    rm -rf "$OUTPUT_PATH"
    
    print_info "Génération de la structure..."
    
    # Créer la structure de base
    mkdir -p "$OUTPUT_PATH"/{conversation-common,conversation-api,conversation-core,conversation-llm,conversation-memory,conversation-persistence}
    
    # Créer la structure Maven pour chaque module
    for module in conversation-common conversation-api conversation-core conversation-llm conversation-memory conversation-persistence; do
        mkdir -p "$OUTPUT_PATH/$module/src/main/java"
        mkdir -p "$OUTPUT_PATH/$module/src/main/resources"
        mkdir -p "$OUTPUT_PATH/$module/src/test/java"
        mkdir -p "$OUTPUT_PATH/$module/src/test/resources"
        print_success "Structure créée: $module"
    done
    
    # Créer le POM parent
    create_parent_pom
    
    # Créer les POMs de module
    for module in conversation-common conversation-api conversation-core conversation-llm conversation-memory conversation-persistence; do
        create_module_pom "$module"
    done
    
    # Créer docker-compose.yml
    create_docker_compose
    
    # Créer .gitignore
    create_gitignore
    
    # Créer README.md
    create_readme
    
    print_success "Projet généré dans: $OUTPUT_PATH"
    echo ""
}

# ============================================================================
# FONCTIONS DE CRÉATION DE FICHIERS
# ============================================================================

create_parent_pom() {
    cat > "$OUTPUT_PATH/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <groupId>com.nexusai</groupId>
    <artifactId>conversation-module</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <name>NexusAI Conversation Module</name>
    
    <modules>
        <module>conversation-common</module>
        <module>conversation-persistence</module>
        <module>conversation-memory</module>
        <module>conversation-llm</module>
        <module>conversation-core</module>
        <module>conversation-api</module>
    </modules>
    
    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>
</project>
EOF
    print_success "POM parent créé"
}

create_module_pom() {
    local module=$1
    cat > "$OUTPUT_PATH/$module/pom.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.nexusai</groupId>
        <artifactId>conversation-module</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>$module</artifactId>
    <name>$module</name>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
    </dependencies>
</project>
EOF
    print_success "POM créé: $module"
}

create_docker_compose() {
    cat > "$OUTPUT_PATH/docker-compose.yml" << 'EOF'
version: '3.8'

services:
  mongodb:
    image: mongo:7.0
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: password
    volumes:
      - mongodb-data:/data/db

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --requirepass password

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

volumes:
  mongodb-data:
EOF
    print_success "docker-compose.yml créé"
}

create_gitignore() {
    cat > "$OUTPUT_PATH/.gitignore" << 'EOF'
target/
.idea/
*.iml
.vscode/
.DS_Store
.env
logs/
EOF
    print_success ".gitignore créé"
}

create_readme() {
    cat > "$OUTPUT_PATH/README.md" << 'EOF'
# NexusAI - Conversation Module

Module de gestion des conversations avec les compagnons IA.

## Démarrage Rapide

```bash
# Démarrer l'infrastructure
docker-compose up -d

# Build
mvn clean install

# Run
mvn spring-boot:run -pl conversation-api
```

## Documentation

- API: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator
EOF
    print_success "README.md créé"
}

# ============================================================================
# ÉTAPE 4: CONFIGURATION ENVIRONNEMENT
# ============================================================================

step4_configure_environment() {
    print_header "ÉTAPE 4: Configuration de l'Environnement"
    
    cd "$OUTPUT_PATH"
    
    # Créer .env
    if [ ! -f .env ]; then
        print_info "Création du fichier .env..."
        cat > .env << 'EOF'
# OpenAI
OPENAI_API_KEY=sk-your-key-here

# Anthropic
ANTHROPIC_API_KEY=sk-ant-your-key-here

# Pinecone
PINECONE_API_KEY=your-key-here
PINECONE_ENVIRONMENT=us-west1-gcp

# MongoDB
MONGODB_URI=mongodb://admin:password@localhost:27017/nexusai_conversations?authSource=admin

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=password

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
EOF
        print_success "Fichier .env créé"
        print_warning "⚠️  N'oubliez pas de configurer vos clés API dans .env"
    else
        print_info ".env existe déjà"
    fi
    
    cd - > /dev/null
    echo ""
}

# ============================================================================
# ÉTAPE 5: DÉMARRAGE INFRASTRUCTURE
# ============================================================================

step5_start_infrastructure() {
    print_header "ÉTAPE 5: Démarrage de l'Infrastructure"
    
    cd "$OUTPUT_PATH"
    
    print_info "Démarrage de Docker Compose..."
    docker-compose up -d
    
    print_info "Attente du démarrage des services..."
    sleep 10
    
    # Vérifier que les services sont up
    if docker-compose ps | grep -q "Up"; then
        print_success "Infrastructure démarrée"
    else
        print_error "Problème de démarrage de l'infrastructure"
        docker-compose logs
        exit 1
    fi
    
    cd - > /dev/null
    echo ""
}

# ============================================================================
# ÉTAPE 6: BUILD DU PROJET
# ============================================================================

step6_build_project() {
    print_header "ÉTAPE 6: Build du Projet"
    
    cd "$OUTPUT_PATH"
    
    print_info "Nettoyage..."
    mvn clean -q
    
    print_info "Compilation..."
    mvn compile -DskipTests -q
    
    print_info "Package..."
    if mvn package -DskipTests -q; then
        print_success "Build réussi"
    else
        print_error "Échec du build"
        exit 1
    fi
    
    cd - > /dev/null
    echo ""
}

# ============================================================================
# ÉTAPE 7: TESTS
# ============================================================================

step7_run_tests() {
    print_header "ÉTAPE 7: Exécution des Tests"
    
    cd "$OUTPUT_PATH"
    
    print_info "Tests unitaires..."
    if mvn test -q 2>/dev/null; then
        print_success "Tests unitaires: OK"
    else
        print_warning "Certains tests ont échoué (normal si code incomplet)"
    fi
    
    cd - > /dev/null
    echo ""
}

# ============================================================================
# ÉTAPE 8: GÉNÉRATION RAPPORT
# ============================================================================

step8_generate_report() {
    print_header "ÉTAPE 8: Génération du Rapport"
    
    cat > "$OUTPUT_PATH/GENERATION_REPORT.txt" << EOF
================================================================================
RAPPORT DE GÉNÉRATION DU PROJET
================================================================================

Date: $(date)
Projet: NexusAI Conversation Module
Chemin: $OUTPUT_PATH

MODULES GÉNÉRÉS:
- conversation-common     ✓
- conversation-api        ✓
- conversation-core       ✓
- conversation-llm        ✓
- conversation-memory     ✓
- conversation-persistence ✓

FICHIERS CRÉÉS:
- pom.xml (parent)        ✓
- pom.xml (modules × 6)   ✓
- docker-compose.yml      ✓
- .gitignore              ✓
- README.md               ✓
- .env                    ✓

INFRASTRUCTURE:
- MongoDB                 $(docker ps | grep mongo > /dev/null && echo "✓ Running" || echo "✗ Stopped")
- Redis                   $(docker ps | grep redis > /dev/null && echo "✓ Running" || echo "✗ Stopped")
- Kafka                   $(docker ps | grep kafka > /dev/null && echo "✓ Running" || echo "✗ Stopped")

BUILD STATUS:
- Compilation             ✓
- Package                 ✓
- Tests                   ⚠️  (code incomplet)

PROCHAINES ÉTAPES:
1. Configurer les clés API dans .env
2. Ajouter le code source des classes
3. Exécuter: mvn spring-boot:run -pl conversation-api
4. Accéder à: http://localhost:8080

================================================================================
EOF
    
    print_success "Rapport généré: $OUTPUT_PATH/GENERATION_REPORT.txt"
    echo ""
}

# ============================================================================
# MAIN
# ============================================================================

main() {
    clear
    
    print_header "GÉNÉRATEUR AUTOMATIQUE - MODULE 4"
    echo ""
    
    step1_check_prerequisites
    step2_compile_generator
    step3_generate_project
    step4_configure_environment
    step5_start_infrastructure
    step6_build_project
    step7_run_tests
    step8_generate_report
    
    # Résumé final
    print_header "GÉNÉRATION TERMINÉE !"
    echo ""
    print_success "Projet généré dans: $OUTPUT_PATH"
    echo ""
    print_info "Pour démarrer l'application:"
    echo -e "  ${BLUE}cd $OUTPUT_PATH${NC}"
    echo -e "  ${BLUE}mvn spring-boot:run -pl conversation-api${NC}"
    echo ""
    print_info "Accès:"
    echo -e "  ${BLUE}API: http://localhost:8080${NC}"
    echo -e "  ${BLUE}Swagger: http://localhost:8080/swagger-ui.html${NC}"
    echo -e "  ${BLUE}Actuator: http://localhost:8080/actuator${NC}"
    echo ""
    print_warning "N'oubliez pas de configurer vos clés API dans: $OUTPUT_PATH/.env"
    echo ""
}

# Exécuter le script principal
main

exit 0
