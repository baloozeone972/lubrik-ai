#!/bin/bash

# ══════════════════════════════════════════════════════════════
# start-nexusai.sh - Script de démarrage automatisé
# ══════════════════════════════════════════════════════════════

set -e  # Arrêter en cas d'erreur

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Fonctions utilitaires
print_header() {
    echo -e "${BLUE}"
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║              NEXUSAI - DÉMARRAGE AUTOMATISÉ                ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

print_step() {
    echo -e "${GREEN}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ ERREUR: $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ ATTENTION: $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ $1${NC}"
}

# Vérifier les prérequis
check_prerequisites() {
    print_step "Vérification des prérequis..."
    
    # Vérifier Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker n'est pas installé"
        echo "Installez Docker : https://docs.docker.com/get-docker/"
        exit 1
    fi
    print_success "Docker installé"
    
    # Vérifier Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose n'est pas installé"
        echo "Installez Docker Compose : https://docs.docker.com/compose/install/"
        exit 1
    fi
    print_success "Docker Compose installé"
    
    # Vérifier Java
    if ! command -v java &> /dev/null; then
        print_error "Java n'est pas installé"
        echo "Installez Java 21+ : https://adoptium.net/"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 21 ]; then
        print_warning "Java $JAVA_VERSION détecté. Java 21+ recommandé."
    else
        print_success "Java $JAVA_VERSION installé"
    fi
    
    # Vérifier Maven
    if ! command -v mvn &> /dev/null; then
        print_warning "Maven non trouvé. Installation recommandée."
    else
        print_success "Maven installé"
    fi
    
    echo ""
}

# Créer les répertoires nécessaires
create_directories() {
    print_step "Création des répertoires..."
    
    mkdir -p docker/init-scripts
    mkdir -p docker/prometheus
    mkdir -p docker/grafana/provisioning/datasources
    mkdir -p docker/grafana/dashboards
    mkdir -p logs
    mkdir -p nexus-auth/src/main/resources/db/migration
    
    print_success "Répertoires créés"
    echo ""
}

# Démarrer les services Docker
start_docker_services() {
    print_step "Démarrage des services Docker..."
    
    # Arrêter les anciens conteneurs
    docker-compose down 2>/dev/null || true
    
    # Démarrer les services
    docker-compose up -d
    
    print_success "Services Docker démarrés"
    echo ""
    
    # Attendre que PostgreSQL soit prêt
    print_step "Attente du démarrage de PostgreSQL..."
    
    for i in {1..30}; do
        if docker exec nexusai-postgres pg_isready -U nexusai -d nexusai_auth &>/dev/null; then
            print_success "PostgreSQL prêt"
            break
        fi
        
        if [ $i -eq 30 ]; then
            print_error "PostgreSQL n'a pas démarré dans les temps"
            exit 1
        fi
        
        echo -n "."
        sleep 1
    done
    
    echo ""
}

# Compiler le projet
compile_project() {
    print_step "Compilation du projet Maven..."
    
    if command -v mvn &> /dev/null; then
        mvn clean install -DskipTests
        print_success "Compilation réussie"
    else
        print_warning "Maven non disponible. Compilation manuelle requise."
    fi
    
    echo ""
}

# Démarrer l'application
start_application() {
    print_step "Démarrage de l'application NexusAI..."
    
    cd nexus-auth
    
    if command -v mvn &> /dev/null; then
        print_info "Démarrage de Spring Boot..."
        print_info "L'application sera accessible sur: http://localhost:8081"
        print_info "Swagger UI: http://localhost:8081/swagger-ui.html"
        echo ""
        
        mvn spring-boot:run
    else
        print_error "Maven requis pour démarrer l'application"
        exit 1
    fi
}

# Afficher les informations de connexion
show_connection_info() {
    echo ""
    echo -e "${MAGENTA}"
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║             SERVICES DÉMARRÉS AVEC SUCCÈS                  ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo ""
    echo -e "${CYAN}📍 URLs des services :${NC}"
    echo -e "   ${GREEN}➤${NC} Application Spring Boot : http://localhost:8081"
    echo -e "   ${GREEN}➤${NC} Swagger UI              : http://localhost:8081/swagger-ui.html"
    echo -e "   ${GREEN}➤${NC} PostgreSQL              : localhost:5432"
    echo -e "   ${GREEN}➤${NC} Redis                   : localhost:6379"
    echo -e "   ${GREEN}➤${NC} PgAdmin                 : http://localhost:5050"
    echo -e "   ${GREEN}➤${NC} Prometheus              : http://localhost:9090"
    echo -e "   ${GREEN}➤${NC} Grafana                 : http://localhost:3000"
    echo -e "   ${GREEN}➤${NC} Mailhog UI              : http://localhost:8025"
    echo ""
    echo -e "${CYAN}🔑 Identifiants par défaut :${NC}"
    echo -e "   ${YELLOW}PostgreSQL:${NC}"
    echo -e "     User     : nexusai"
    echo -e "     Password : nexusai_password"
    echo -e "     Database : nexusai_auth"
    echo ""
    echo -e "   ${YELLOW}PgAdmin:${NC}"
    echo -e "     Email    : admin@nexusai.com"
    echo -e "     Password : admin"
    echo ""
    echo -e "   ${YELLOW}Grafana:${NC}"
    echo -e "     User     : admin"
    echo -e "     Password : admin"
    echo ""
    echo -e "   ${YELLOW}API Admin:${NC}"
    echo -e "     Email    : admin@nexusai.com"
    echo -e "     Password : Admin@123"
    echo ""
    echo -e "${CYAN}📚 Documentation :${NC}"
    echo -e "   ${GREEN}➤${NC} README.md pour plus d'informations"
    echo -e "   ${GREEN}➤${NC} Swagger pour tester l'API"
    echo ""
    echo -e "${YELLOW}⚠ N'oubliez pas de changer les mots de passe en production !${NC}"
    echo ""
}

# Programme principal
main() {
    print_header
    
    check_prerequisites
    create_directories
    start_docker_services
    compile_project
    
    show_connection_info
    
    # Demander si on démarre l'application
    read -p "Démarrer l'application Spring Boot maintenant ? (o/N) " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Oo]$ ]]; then
        start_application
    else
        print_info "Pour démarrer l'application plus tard :"
        echo "  cd nexus-auth && mvn spring-boot:run"
        echo ""
        print_success "Configuration terminée !"
    fi
}

# Exécuter le programme
main "$@"

# ══════════════════════════════════════════════════════════════
# stop-nexusai.sh - Script d'arrêt
# ══════════════════════════════════════════════════════════════

#!/bin/bash

echo "Arrêt des services NexusAI..."

# Arrêter Docker Compose
docker-compose down

echo "✓ Services arrêtés avec succès"

# ══════════════════════════════════════════════════════════════
# clean-nexusai.sh - Script de nettoyage complet
# ══════════════════════════════════════════════════════════════

#!/bin/bash

echo "⚠️  ATTENTION : Ceci va supprimer TOUTES les données !"
read -p "Êtes-vous sûr ? (tapez 'oui' pour confirmer) " -r
echo

if [[ $REPLY == "oui" ]]; then
    echo "Nettoyage en cours..."
    
    # Arrêter et supprimer les conteneurs
    docker-compose down -v
    
    # Supprimer les volumes
    docker volume rm nexus-ai-parent_postgres_data 2>/dev/null || true
    docker volume rm nexus-ai-parent_redis_data 2>/dev/null || true
    docker volume rm nexus-ai-parent_pgadmin_data 2>/dev/null || true
    docker volume rm nexus-ai-parent_prometheus_data 2>/dev/null || true
    docker volume rm nexus-ai-parent_grafana_data 2>/dev/null || true
    
    # Nettoyer les builds Maven
    mvn clean
    
    echo "✓ Nettoyage terminé"
else
    echo "Nettoyage annulé"
fi

# ══════════════════════════════════════════════════════════════
# logs-nexusai.sh - Script pour voir les logs
# ══════════════════════════════════════════════════════════════

#!/bin/bash

# Choix du service
echo "Choisissez un service :"
echo "1) Application Spring Boot"
echo "2) PostgreSQL"
echo "3) Redis"
echo "4) Tous les services Docker"
read -p "Votre choix (1-4) : " choice

case $choice in
    1)
        tail -f logs/nexus-auth.log 2>/dev/null || \
        echo "Logs non disponibles. Application pas encore démarrée ?"
        ;;
    2)
        docker logs -f nexusai-postgres
        ;;
    3)
        docker logs -f nexusai-redis
        ;;
    4)
        docker-compose logs -f
        ;;
    *)
        echo "Choix invalide"
        ;;
esac

# ══════════════════════════════════════════════════════════════
# Rendre les scripts exécutables
# ══════════════════════════════════════════════════════════════

# chmod +x start-nexusai.sh
# chmod +x stop-nexusai.sh
# chmod +x clean-nexusai.sh
# chmod +x logs-nexusai.sh
