#!/bin/bash

################################################################################
# NexusAI Audio Module - Script d'Installation Automatique
# 
# Ce script automatise complètement la génération et l'installation du
# Module 6 Audio de NexusAI.
#
# Usage:
#   ./setup-nexusai-audio.sh [OPTIONS]
#
# Options:
#   -d, --doc FILE        Fichier de documentation source
#   -o, --output DIR      Dossier de sortie
#   -h, --help            Afficher l'aide
#   --skip-docker         Ne pas démarrer Docker
#   --skip-build          Ne pas compiler le projet
#   --clean               Nettoyer avant installation
#
# Exemple:
#   ./setup-nexusai-audio.sh \
#       --doc nexusai-audio-complete.md \
#       --output ~/projects/nexus-audio
#
# Auteur: NexusAI Team
# Version: 1.0.0
# Date: 20 Octobre 2025
################################################################################

set -e  # Arrêter en cas d'erreur

# ==============================================================================
# COULEURS
# ==============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ==============================================================================
# FONCTIONS UTILITAIRES
# ==============================================================================

print_header() {
    echo ""
    echo -e "${CYAN}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}  $1"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_step() {
    echo -e "${MAGENTA}→${NC} $1"
}

check_command() {
    if ! command -v $1 &> /dev/null; then
        print_error "$1 n'est pas installé"
        return 1
    else
        print_success "$1 est installé"
        return 0
    fi
}

# ==============================================================================
# VARIABLES PAR DÉFAUT
# ==============================================================================

DOC_FILE=""
OUTPUT_DIR=""
SKIP_DOCKER=false
SKIP_BUILD=false
CLEAN_BEFORE=false
GENERATOR_DIR="nexusai-generator"

# ==============================================================================
# PARSING DES ARGUMENTS
# ==============================================================================

while [[ $# -gt 0 ]]; do
    case $1 in
        -d|--doc)
            DOC_FILE="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        --skip-docker)
            SKIP_DOCKER=true
            shift
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --clean)
            CLEAN_BEFORE=true
            shift
            ;;
        -h|--help)
            cat << EOF
Usage: ./setup-nexusai-audio.sh [OPTIONS]

Options:
  -d, --doc FILE        Fichier de documentation source
  -o, --output DIR      Dossier de sortie
  --skip-docker         Ne pas démarrer Docker
  --skip-build          Ne pas compiler le projet
  --clean               Nettoyer avant installation
  -h, --help            Afficher cette aide

Exemple:
  ./setup-nexusai-audio.sh \\
      --doc nexusai-audio-complete.md \\
      --output ~/projects/nexus-audio
EOF
            exit 0
            ;;
        *)
            print_error "Option inconnue: $1"
            exit 1
            ;;
    esac
done

# ==============================================================================
# VÉRIFICATIONS INITIALES
# ==============================================================================

print_header "NexusAI Audio Module - Installation Automatique"

print_step "Vérification des prérequis..."
echo ""

PREREQUISITES_OK=true

if ! check_command java; then
    print_error "Java n'est pas installé. Version 21+ requise."
    PREREQUISITES_OK=false
fi

if ! check_command mvn; then
    print_error "Maven n'est pas installé. Version 3.9+ requise."
    PREREQUISITES_OK=false
fi

if ! check_command docker && [ "$SKIP_DOCKER" = false ]; then
    print_warning "Docker n'est pas installé. Vous ne pourrez pas démarrer les services."
    print_info "Utilisez --skip-docker pour ignorer cette vérification."
fi

if [ "$PREREQUISITES_OK" = false ]; then
    echo ""
    print_error "Certains prérequis ne sont pas satisfaits."
    exit 1
fi

echo ""
print_success "Tous les prérequis sont satisfaits"

# Vérifier la version de Java
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo ""
    print_error "Java 21+ est requis (version actuelle: $JAVA_VERSION)"
    exit 1
fi

print_success "Java version $JAVA_VERSION détectée"

# ==============================================================================
# VALIDATION DES PARAMÈTRES
# ==============================================================================

echo ""
print_step "Validation des paramètres..."
echo ""

if [ -z "$DOC_FILE" ]; then
    print_info "Fichier de documentation non spécifié."
    print_info "Recherche automatique..."
    
    # Chercher le fichier dans le répertoire courant
    if [ -f "nexusai-audio-complete.md" ]; then
        DOC_FILE="nexusai-audio-complete.md"
        print_success "Fichier trouvé: $DOC_FILE"
    else
        print_error "Fichier de documentation introuvable."
        print_info "Utilisez -d pour spécifier le fichier."
        exit 1
    fi
else
    if [ ! -f "$DOC_FILE" ]; then
        print_error "Fichier introuvable: $DOC_FILE"
        exit 1
    fi
    print_success "Fichier de documentation: $DOC_FILE"
fi

if [ -z "$OUTPUT_DIR" ]; then
    OUTPUT_DIR="./nexus-audio-generated"
    print_info "Dossier de sortie non spécifié."
    print_info "Utilisation du dossier par défaut: $OUTPUT_DIR"
fi

# Convertir en chemin absolu
OUTPUT_DIR=$(cd "$(dirname "$OUTPUT_DIR")" 2>/dev/null && pwd)/$(basename "$OUTPUT_DIR") || OUTPUT_DIR="$OUTPUT_DIR"

print_success "Dossier de sortie: $OUTPUT_DIR"

# ==============================================================================
# NETTOYAGE (SI DEMANDÉ)
# ==============================================================================

if [ "$CLEAN_BEFORE" = true ]; then
    echo ""
    print_step "Nettoyage du dossier de sortie..."
    
    if [ -d "$OUTPUT_DIR" ]; then
        print_warning "Le dossier $OUTPUT_DIR existe déjà."
        read -p "Voulez-vous le supprimer ? (y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            rm -rf "$OUTPUT_DIR"
            print_success "Dossier supprimé"
        else
            print_info "Conservation du dossier existant"
        fi
    fi
fi

# ==============================================================================
# CRÉATION DU GÉNÉRATEUR
# ==============================================================================

echo ""
print_header "Étape 1/6 : Compilation du générateur"

if [ ! -d "$GENERATOR_DIR" ]; then
    print_step "Création du dossier du générateur..."
    mkdir -p "$GENERATOR_DIR/src/main/java/com/nexusai/tools"
    print_success "Dossier créé"
fi

# Vérifier si ProjectGenerator.java existe
if [ ! -f "$GENERATOR_DIR/src/main/java/com/nexusai/tools/ProjectGenerator.java" ]; then
    print_error "ProjectGenerator.java introuvable dans $GENERATOR_DIR"
    print_info "Assurez-vous que le fichier existe à l'emplacement correct."
    exit 1
fi

# Vérifier si pom.xml existe
if [ ! -f "$GENERATOR_DIR/pom.xml" ]; then
    print_step "Création du pom.xml..."
    
    cat > "$GENERATOR_DIR/pom.xml" << 'EOF'
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
        </plugins>
    </build>
</project>
EOF
    
    print_success "pom.xml créé"
fi

print_step "Compilation du générateur..."
cd "$GENERATOR_DIR"
mvn clean compile -q
if [ $? -eq 0 ]; then
    print_success "Générateur compilé avec succès"
else
    print_error "Erreur lors de la compilation du générateur"
    exit 1
fi
cd ..

# ==============================================================================
# GÉNÉRATION DU PROJET
# ==============================================================================

echo ""
print_header "Étape 2/6 : Génération du projet"

print_step "Exécution du générateur..."
echo ""

java -cp "$GENERATOR_DIR/target/classes" \
    com.nexusai.tools.ProjectGenerator \
    "$DOC_FILE" \
    "$OUTPUT_DIR"

if [ $? -eq 0 ]; then
    echo ""
    print_success "Projet généré avec succès dans $OUTPUT_DIR"
else
    print_error "Erreur lors de la génération du projet"
    exit 1
fi

# ==============================================================================
# CONFIGURATION DES API KEYS
# ==============================================================================

echo ""
print_header "Étape 3/6 : Configuration des API keys"

cd "$OUTPUT_DIR"

# Créer le fichier .env si pas déjà présent
if [ ! -f ".env" ]; then
    print_step "Création du fichier .env..."
    
    cat > .env << 'EOF'
# OpenAI API Key (requis pour Whisper STT)
OPENAI_API_KEY=sk-your-openai-api-key

# ElevenLabs API Key (requis pour TTS)
ELEVENLABS_API_KEY=your-elevenlabs-api-key
EOF
    
    print_success "Fichier .env créé"
    print_warning "N'oubliez pas de configurer vos API keys dans .env"
else
    print_info "Fichier .env existe déjà"
fi

# Vérifier si les clés sont configurées dans l'environnement
if [ -n "$OPENAI_API_KEY" ]; then
    print_success "OPENAI_API_KEY trouvée dans l'environnement"
else
    print_warning "OPENAI_API_KEY non configurée"
    print_info "Configurez-la avec: export OPENAI_API_KEY=sk-..."
fi

if [ -n "$ELEVENLABS_API_KEY" ]; then
    print_success "ELEVENLABS_API_KEY trouvée dans l'environnement"
else
    print_warning "ELEVENLABS_API_KEY non configurée"
    print_info "Configurez-la avec: export ELEVENLABS_API_KEY=..."
fi

# ==============================================================================
# DÉMARRAGE DOCKER
# ==============================================================================

if [ "$SKIP_DOCKER" = false ]; then
    echo ""
    print_header "Étape 4/6 : Démarrage des services Docker"
    
    if command -v docker &> /dev/null && command -v docker-compose &> /dev/null; then
        print_step "Démarrage de Docker Compose..."
        
        docker-compose up -d
        
        if [ $? -eq 0 ]; then
            print_success "Services Docker démarrés"
            
            print_step "Attente du démarrage des services (30s)..."
            for i in {1..30}; do
                echo -ne "${BLUE}⏳${NC} Attente... $i/30\r"
                sleep 1
            done
            echo ""
            print_success "Services prêts"
            
            # Vérifier les services
            echo ""
            print_step "Vérification des services..."
            docker-compose ps
            
        else
            print_error "Erreur lors du démarrage de Docker"
            print_warning "Vous devrez démarrer les services manuellement"
        fi
    else
        print_warning "Docker ou Docker Compose non installé"
        print_info "Installez Docker pour utiliser les services"
    fi
else
    echo ""
    print_header "Étape 4/6 : Services Docker (ignoré)"
    print_info "Démarrage Docker ignoré (--skip-docker)"
fi

# ==============================================================================
# COMPILATION DU PROJET
# ==============================================================================

if [ "$SKIP_BUILD" = false ]; then
    echo ""
    print_header "Étape 5/6 : Compilation du projet Maven"
    
    print_step "Compilation en cours (cela peut prendre quelques minutes)..."
    
    mvn clean install -DskipTests
    
    if [ $? -eq 0 ]; then
        print_success "Projet compilé avec succès"
    else
        print_error "Erreur lors de la compilation"
        print_warning "Vous devrez compiler manuellement avec: mvn clean install"
    fi
else
    echo ""
    print_header "Étape 5/6 : Compilation Maven (ignorée)"
    print_info "Compilation ignorée (--skip-build)"
fi

# ==============================================================================
# RÉSUMÉ ET INSTRUCTIONS
# ==============================================================================

echo ""
print_header "Étape 6/6 : Résumé et prochaines étapes"

print_success "Installation terminée avec succès !"
echo ""

echo -e "${CYAN}📁 Projet généré dans:${NC}"
echo "   $OUTPUT_DIR"
echo ""

echo -e "${CYAN}🔧 Configuration:${NC}"
if [ -f ".env" ]; then
    echo "   ✓ Fichier .env créé"
else
    echo "   ✗ Fichier .env non créé"
fi

if [ -n "$OPENAI_API_KEY" ]; then
    echo "   ✓ OPENAI_API_KEY configurée"
else
    echo "   ⚠ OPENAI_API_KEY non configurée"
fi

if [ -n "$ELEVENLABS_API_KEY" ]; then
    echo "   ✓ ELEVENLABS_API_KEY configurée"
else
    echo "   ⚠ ELEVENLABS_API_KEY non configurée"
fi
echo ""

echo -e "${CYAN}🐳 Services Docker:${NC}"
if docker-compose ps | grep -q "Up"; then
    echo "   ✓ Services démarrés"
    docker-compose ps | tail -n +3 | awk '{print "   - " $1 ": " $3}'
else
    echo "   ⚠ Services non démarrés"
fi
echo ""

echo -e "${CYAN}📦 Compilation Maven:${NC}"
if [ -f "nexus-audio-api/target/nexus-audio-api-1.0.0-SNAPSHOT.jar" ]; then
    echo "   ✓ Projet compilé"
else
    echo "   ⚠ Projet non compilé"
fi
echo ""

print_header "Prochaines étapes"

echo -e "${GREEN}1.${NC} Accéder au projet:"
echo "   cd $OUTPUT_DIR"
echo ""

if [ -z "$OPENAI_API_KEY" ] || [ -z "$ELEVENLABS_API_KEY" ]; then
    echo -e "${GREEN}2.${NC} Configurer les API keys:"
    echo "   export OPENAI_API_KEY=sk-..."
    echo "   export ELEVENLABS_API_KEY=..."
    echo ""
fi

if [ "$SKIP_DOCKER" = true ]; then
    echo -e "${GREEN}3.${NC} Démarrer les services Docker:"
    echo "   docker-compose up -d"
    echo ""
fi

if [ "$SKIP_BUILD" = true ]; then
    echo -e "${GREEN}4.${NC} Compiler le projet:"
    echo "   mvn clean install"
    echo ""
fi

echo -e "${GREEN}5.${NC} Lancer l'application:"
echo "   mvn spring-boot:run -pl nexus-audio-api"
echo ""

echo -e "${GREEN}6.${NC} Tester l'API:"
echo "   curl http://localhost:8083/actuator/health"
echo "   curl http://localhost:8083/swagger-ui.html"
echo ""

print_header "Documentation"

echo "📚 Documentation disponible:"
echo "   • README.md          - Guide de démarrage"
echo "   • Swagger UI         - http://localhost:8083/swagger-ui.html"
echo "   • Actuator Health    - http://localhost:8083/actuator/health"
echo "   • MinIO Console      - http://localhost:9001 (nexusai/nexusai123)"
echo ""

print_success "✅ Installation complète !"
echo ""

# ==============================================================================
# FIN
# ==============================================================================
