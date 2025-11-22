#!/bin/bash

# ════════════════════════════════════════════════════════════════════════
# NEXUSAI MODULE GENERATOR - Script de génération (Linux/Mac)
# ════════════════════════════════════════════════════════════════════════

set -e  # Arrêter en cas d'erreur

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonction d'affichage
print_header() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║           NEXUSAI MODULE GENERATOR v1.0                    ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_step() {
    echo -e "${GREEN}▶ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ ERREUR: $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ ATTENTION: $1${NC}"
}

# Vérifier Java
check_java() {
    if ! command -v java &> /dev/null; then
        print_error "Java n'est pas installé ou n'est pas dans le PATH"
        echo "Installez Java 21+ : https://adoptium.net/"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 21 ]; then
        print_warning "Java $JAVA_VERSION détecté. Java 21+ recommandé."
    fi
}

# Vérifier Maven (optionnel)
check_maven() {
    if command -v mvn &> /dev/null; then
        return 0
    else
        print_warning "Maven non trouvé. Compilation directe avec javac."
        return 1
    fi
}

# Compiler avec Maven
compile_maven() {
    print_step "Compilation avec Maven..."
    mvn clean compile
    mvn package
    JAR_FILE="target/nexusai-generator-1.0.0-jar-with-dependencies.jar"
}

# Compiler avec javac
compile_javac() {
    print_step "Compilation avec javac..."
    mkdir -p out
    javac -d out NexusAIModuleParser.java
    JAR_FILE=""
}

# Programme principal
main() {
    print_header
    
    # Vérifications
    check_java
    
    # Paramètres
    INPUT_FILE="${1:-nexusai-module.md}"
    OUTPUT_DIR="${2:-./nexus-ai-generated}"
    MODE="${3:-}"
    
    # Vérifier le fichier d'entrée
    if [ ! -f "$INPUT_FILE" ]; then
        print_error "Fichier introuvable: $INPUT_FILE"
        echo ""
        echo "Usage: ./generate.sh <fichier-markdown> [répertoire-sortie] [options]"
        echo ""
        echo "Options:"
        echo "  --dry-run    Simulation sans création de fichiers"
        echo "  --tree       Génération de l'arbre uniquement"
        echo "  --validate   Validation après génération"
        echo ""
        exit 1
    fi
    
    print_step "Fichier source : $INPUT_FILE"
    print_step "Répertoire de sortie : $OUTPUT_DIR"
    
    # Compilation
    if check_maven; then
        compile_maven
    else
        compile_javac
    fi
    
    # Exécution
    print_step "Génération de la structure..."
    echo ""
    
    if [ -n "$JAR_FILE" ] && [ -f "$JAR_FILE" ]; then
        # Exécution avec JAR
        java -jar "$JAR_FILE" $MODE "$INPUT_FILE" "$OUTPUT_DIR"
    else
        # Exécution avec classes compilées
        java -cp out com.nexusai.generator.NexusAIModuleParser $MODE "$INPUT_FILE" "$OUTPUT_DIR"
    fi
    
    EXIT_CODE=$?
    
    if [ $EXIT_CODE -eq 0 ]; then
        echo ""
        echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║              ✓ GÉNÉRATION RÉUSSIE !                        ║${NC}"
        echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
        echo ""
        echo -e "${BLUE}📍 Projet généré dans : $OUTPUT_DIR${NC}"
        echo ""
        echo -e "${YELLOW}🚀 Prochaines étapes :${NC}"
        echo "   cd $OUTPUT_DIR"
        echo "   docker-compose up -d"
        echo "   mvn clean install"
        echo "   cd nexus-auth && mvn spring-boot:run"
        echo ""
    else
        echo ""
        print_error "La génération a échoué (code: $EXIT_CODE)"
        exit $EXIT_CODE
    fi
}

# Lancer le programme
main "$@"