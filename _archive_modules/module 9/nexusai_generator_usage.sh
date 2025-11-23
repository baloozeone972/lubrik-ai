#!/bin/bash

# =============================================================================
# generate-project.sh - Script de génération de l'arborescence NexusAI Module 9
# =============================================================================

set -e  # Arrêter en cas d'erreur

# Couleurs pour les logs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonctions utilitaires
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# =============================================================================
# Configuration
# =============================================================================

PROJECT_NAME="nexus-moderation"
OUTPUT_DIR="${1:-./nexus-moderation}"
DOC_DIR="./documentation"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║        NexusAI Module 9 - Project Generator                   ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# =============================================================================
# Étape 1: Vérification des prérequis
# =============================================================================

log_info "Checking prerequisites..."

# Vérifier Java
if ! command -v java &> /dev/null; then
    log_error "Java is not installed. Please install Java 21+"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    log_error "Java 21+ is required (found Java $JAVA_VERSION)"
    exit 1
fi

log_success "Java $JAVA_VERSION detected"

# Vérifier Maven
if ! command -v mvn &> /dev/null; then
    log_warning "Maven is not installed. You'll need it to build the project."
else
    log_success "Maven detected"
fi

# =============================================================================
# Étape 2: Compilation du générateur
# =============================================================================

log_info "Compiling ProjectFileGenerator..."

GENERATOR_SRC="./tools/ProjectFileGenerator.java"
GENERATOR_CLASS="./tools/ProjectFileGenerator.class"

# Créer le répertoire tools si nécessaire
mkdir -p tools

# Copier la classe si elle n'existe pas
if [ ! -f "$GENERATOR_SRC" ]; then
    log_error "ProjectFileGenerator.java not found at $GENERATOR_SRC"
    log_info "Please save the ProjectFileGenerator.java class in the ./tools directory"
    exit 1
fi

# Compiler
javac "$GENERATOR_SRC"
log_success "Generator compiled"

# =============================================================================
# Étape 3: Préparation des fichiers de documentation
# =============================================================================

log_info "Preparing documentation files..."

# Créer le répertoire de documentation si nécessaire
mkdir -p "$DOC_DIR"

# Liste des fichiers de documentation attendus
DOC_FILES=(
    "module9-code.md"
    "module9-config.md"
    "module9-tests.md"
    "module9-deploy.md"
)

FOUND_DOCS=0
for doc in "${DOC_FILES[@]}"; do
    if [ -f "$DOC_DIR/$doc" ]; then
        log_success "Found: $doc"
        FOUND_DOCS=$((FOUND_DOCS + 1))
    else
        log_warning "Missing: $doc"
    fi
done

if [ $FOUND_DOCS -eq 0 ]; then
    log_error "No documentation files found in $DOC_DIR"
    log_info "Please place your documentation markdown files in $DOC_DIR"
    exit 1
fi

# =============================================================================
# Étape 4: Génération du projet
# =============================================================================

log_info "Generating project structure in: $OUTPUT_DIR"

# Créer le répertoire de sortie
mkdir -p "$OUTPUT_DIR"

# Exécuter le générateur pour chaque fichier de documentation
for doc in "${DOC_FILES[@]}"; do
    if [ -f "$DOC_DIR/$doc" ]; then
        log_info "Processing: $doc"
        java -cp ./tools com.nexusai.tools.ProjectFileGenerator "$OUTPUT_DIR" "$DOC_DIR/$doc"
    fi
done

log_success "Project structure generated"

# =============================================================================
# Étape 5: Création des fichiers additionnels
# =============================================================================

log_info "Creating additional files..."

# Créer pom.xml si absent
if [ ! -f "$OUTPUT_DIR/pom.xml" ]; then
    log_info "Creating pom.xml..."
    cat > "$OUTPUT_DIR/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <groupId>com.nexusai</groupId>
    <artifactId>nexus-moderation</artifactId>
    <version>1.0.0</version>
    <name>NexusAI Moderation System</name>
    
    <properties>
        <java.version>21</java.version>
    </properties>
    
    <dependencies>
        <!-- Will be added by generator -->
    </dependencies>
</project>
EOF
    log_success "Created pom.xml"
fi

# Créer .gitignore
cat > "$OUTPUT_DIR/.gitignore" << 'EOF'
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml

# IDE
.idea/
*.iml
.vscode/
*.swp
*.swo
.DS_Store

# Logs
logs/
*.log

# Environment
.env
.env.local

# Build
build/
dist/

# Test
*.class
EOF
log_success "Created .gitignore"

# Créer README.md si absent
if [ ! -f "$OUTPUT_DIR/README.md" ]; then
    cat > "$OUTPUT_DIR/README.md" << 'EOF'
# NexusAI - Module 9 : Moderation System

## Quick Start

```bash
# Start dependencies
docker-compose up -d

# Build
mvn clean install

# Run
mvn spring-boot:run
```

See full documentation in `docs/` directory.
EOF
    log_success "Created README.md"
fi

# Créer Dockerfile
cat > "$OUTPUT_DIR/Dockerfile" << 'EOF'
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/nexus-moderation-1.0.0.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
log_success "Created Dockerfile"

# Créer docker-compose.yml
cat > "$OUTPUT_DIR/docker-compose.yml" << 'EOF'
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: nexusai
      POSTGRES_USER: nexusai
      POSTGRES_PASSWORD: nexusai123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper
  
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

volumes:
  postgres_data:
  redis_data:
EOF
log_success "Created docker-compose.yml"

# Créer .env.example
cat > "$OUTPUT_DIR/.env.example" << 'EOF'
# Database
DB_URL=jdbc:postgresql://localhost:5432/nexusai
DB_USERNAME=nexusai
DB_PASSWORD=nexusai123

# Redis
REDIS_HOST=localhost
REDIS_PASSWORD=

# Kafka
KAFKA_BROKERS=localhost:9092

# External APIs
OPENAI_API_KEY=sk-...
AWS_ACCESS_KEY=AKIA...
AWS_SECRET_KEY=...
PHOTODNA_API_KEY=...

# Services
USER_SERVICE_URL=http://localhost:8080
EOF
log_success "Created .env.example"

# =============================================================================
# Étape 6: Vérification de la structure
# =============================================================================

log_info "Verifying project structure..."

EXPECTED_DIRS=(
    "src/main/java/com/nexusai/moderation"
    "src/main/resources"
    "src/test/java/com/nexusai/moderation"
    "docs"
)

for dir in "${EXPECTED_DIRS[@]}"; do
    if [ -d "$OUTPUT_DIR/$dir" ]; then
        log_success "✓ $dir"
    else
        log_warning "✗ $dir (missing)"
    fi
done

# =============================================================================
# Étape 7: Génération du rapport
# =============================================================================

log_info "Generating report..."

REPORT_FILE="$OUTPUT_DIR/GENERATION_REPORT.txt"

cat > "$REPORT_FILE" << EOF
╔════════════════════════════════════════════════════════════════╗
║     NexusAI Module 9 - Generation Report                      ║
╚════════════════════════════════════════════════════════════════╝

Generated: $(date)
Output Directory: $OUTPUT_DIR

Files Generated:
$(find "$OUTPUT_DIR" -type f | wc -l) files
$(find "$OUTPUT_DIR" -type d | wc -l) directories

Structure:
$(tree -L 3 "$OUTPUT_DIR" 2>/dev/null || echo "Install 'tree' command for detailed view")

Next Steps:
1. Review the generated files
2. Set up your .env file (copy from .env.example)
3. Install dependencies: mvn clean install
4. Start services: docker-compose up -d
5. Run the application: mvn spring-boot:run

Documentation:
- README.md - Quick start guide
- docs/ - Detailed documentation
- CONTRIBUTING.md - Contribution guide

EOF

log_success "Report saved to: $REPORT_FILE"

# =============================================================================
# Étape 8: Résumé final
# =============================================================================

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                    Generation Complete! 🎉                     ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
log_success "Project generated at: $OUTPUT_DIR"
log_info "Next steps:"
echo "  1. cd $OUTPUT_DIR"
echo "  2. cp .env.example .env"
echo "  3. Edit .env with your API keys"
echo "  4. docker-compose up -d"
echo "  5. mvn clean install"
echo "  6. mvn spring-boot:run"
echo ""
log_info "Documentation:"
echo "  - Full report: $REPORT_FILE"
echo "  - README: $OUTPUT_DIR/README.md"
echo "  - Docs: $OUTPUT_DIR/docs/"
echo ""
log_success "Happy coding! 🚀"

# =============================================================================
# Usage Guide (if no arguments)
# =============================================================================

if [ "$#" -eq 0 ]; then
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "Usage: ./generate-project.sh [output-directory]"
    echo ""
    echo "Examples:"
    echo "  ./generate-project.sh                    # Generate in ./nexus-moderation"
    echo "  ./generate-project.sh /path/to/output   # Generate in custom path"
    echo ""
    echo "Documentation files should be in ./documentation/"
    echo "Expected files:"
    echo "  - module9-code.md"
    echo "  - module9-config.md"
    echo "  - module9-tests.md"
    echo "  - module9-deploy.md"
    echo "════════════════════════════════════════════════════════════════"
fi
