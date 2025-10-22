#!/bin/bash
# ============================================================================
# NEXUSAI VIDEO GENERATION - QUICK START SCRIPT
# ============================================================================
# Ce script automatise le démarrage complet du module de génération vidéo
# 
# Usage: ./quick-start.sh [environment]
#   environment: dev|staging|production (défaut: dev)
#
# Auteur: NexusAI Team
# Version: 1.0
# ============================================================================

set -e  # Arrêter en cas d'erreur

# Couleurs pour output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT=${1:-dev}
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Fonctions utilitaires
print_header() {
    echo -e "${BLUE}"
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║          NEXUSAI VIDEO GENERATION - QUICK START           ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

print_step() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

# Vérification des prérequis
check_prerequisites() {
    echo -e "${BLUE}Vérification des prérequis...${NC}"
    
    local all_ok=true
    
    # Java 21
    if command -v java &> /dev/null; then
        java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [ "$java_version" -ge 21 ]; then
            print_step "Java $java_version installé"
        else
            print_error "Java 21+ requis (trouvé: Java $java_version)"
            all_ok=false
        fi
    else
        print_error "Java non installé"
        all_ok=false
    fi
    
    # Maven
    if command -v mvn &> /dev/null; then
        print_step "Maven installé"
    else
        print_error "Maven non installé"
        all_ok=false
    fi
    
    # Docker
    if command -v docker &> /dev/null; then
        print_step "Docker installé"
    else
        print_error "Docker non installé"
        all_ok=false
    fi
    
    # Docker Compose
    if command -v docker-compose &> /dev/null; then
        print_step "Docker Compose installé"
    else
        print_error "Docker Compose non installé"
        all_ok=false
    fi
    
    # Python 3.11+
    if command -v python3 &> /dev/null; then
        python_version=$(python3 --version | awk '{print $2}' | cut -d'.' -f1,2)
        if [ "$(echo "$python_version >= 3.11" | bc)" -eq 1 ]; then
            print_step "Python $python_version installé"
        else
            print_warning "Python 3.11+ recommandé (trouvé: Python $python_version)"
        fi
    else
        print_error "Python 3 non installé"
        all_ok=false
    fi
    
    # FFmpeg
    if command -v ffmpeg &> /dev/null; then
        print_step "FFmpeg installé"
    else
        print_warning "FFmpeg non installé (requis pour les workers)"
    fi
    
    if [ "$all_ok" = false ]; then
        print_error "Certains prérequis sont manquants. Installation interrompue."
        exit 1
    fi
    
    echo ""
}

# Configuration de l'environnement
setup_environment() {
    echo -e "${BLUE}Configuration de l'environnement: $ENVIRONMENT${NC}"
    
    # Création du fichier .env s'il n'existe pas
    if [ ! -f ".env" ]; then
        print_warning "Fichier .env non trouvé. Création depuis .env.example..."
        
        if [ -f ".env.example" ]; then
            cp .env.example .env
            print_step "Fichier .env créé"
            print_warning "⚠️  Veuillez configurer vos clés API dans le fichier .env"
            echo ""
            echo "Clés requises:"
            echo "  - OPENAI_API_KEY"
            echo "  - ELEVENLABS_API_KEY"
            echo "  - AWS_ACCESS_KEY_ID"
            echo "  - AWS_SECRET_ACCESS_KEY"
            echo ""
            read -p "Appuyez sur Entrée une fois la configuration terminée..."
        else
            print_error ".env.example non trouvé"
            exit 1
        fi
    else
        print_step "Fichier .env trouvé"
    fi
    
    # Sourcer les variables d'environnement
    set -a
    source .env
    set +a
    
    echo ""
}

# Création de la base de données
setup_database() {
    echo -e "${BLUE}Configuration de la base de données...${NC}"
    
    # Démarrer PostgreSQL si pas déjà lancé
    if ! docker ps | grep -q nexusai-postgres; then
        print_step "Démarrage de PostgreSQL..."
        docker-compose up -d postgres
        sleep 5  # Attendre que PostgreSQL soit prêt
    fi
    
    # Exécuter les migrations
    print_step "Exécution des migrations SQL..."
    docker-compose exec -T postgres psql -U nexusai -d nexusai < sql/V1_0__create_video_tables.sql
    
    print_step "Base de données configurée"
    echo ""
}

# Build du projet
build_project() {
    echo -e "${BLUE}Build du projet...${NC}"
    
    # Build service Java
    print_step "Build du service Java..."
    cd nexus-video-generation
    mvn clean package -DskipTests
    cd ..
    
    # Build images Docker
    print_step "Build des images Docker..."
    docker-compose build
    
    print_step "Build terminé"
    echo ""
}

# Démarrage des services
start_services() {
    echo -e "${BLUE}Démarrage des services...${NC}"
    
    # Infrastructure
    print_step "Démarrage de l'infrastructure (Kafka, Redis)..."
    docker-compose up -d kafka redis
    sleep 10  # Attendre que Kafka soit prêt
    
    # Service vidéo
    print_step "Démarrage du service vidéo..."
    docker-compose up -d video-service
    sleep 5
    
    # Workers
    print_step "Démarrage des workers Python (3 instances)..."
    docker-compose up -d video-worker-1 video-worker-2 video-worker-3
    
    echo ""
    print_step "Tous les services sont démarrés !"
    echo ""
}

# Vérification de la santé des services
health_check() {
    echo -e "${BLUE}Vérification de la santé des services...${NC}"
    
    # Attendre que le service soit prêt
    print_step "Attente du démarrage complet (30 secondes)..."
    sleep 30
    
    # Check API
    if curl -f -s http://localhost:8084/actuator/health > /dev/null; then
        print_step "API REST opérationnelle"
    else
        print_error "API REST ne répond pas"
    fi
    
    # Check workers
    worker_count=$(docker ps | grep -c video-worker || true)
    if [ "$worker_count" -eq 3 ]; then
        print_step "$worker_count workers actifs"
    else
        print_warning "Seulement $worker_count workers actifs (3 attendus)"
    fi
    
    echo ""
}

# Affichage des informations finales
display_info() {
    echo -e "${GREEN}"
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║                  INSTALLATION TERMINÉE !                   ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo ""
    echo "📊 URLs importantes:"
    echo "  - API REST:      http://localhost:8084"
    echo "  - Swagger UI:    http://localhost:8084/swagger-ui.html"
    echo "  - Health Check:  http://localhost:8084/actuator/health"
    echo "  - Metrics:       http://localhost:8084/actuator/prometheus"
    echo ""
    echo "🔧 Commandes utiles:"
    echo "  - Voir les logs:        docker-compose logs -f"
    echo "  - Arrêter:              docker-compose down"
    echo "  - Redémarrer:           docker-compose restart"
    echo "  - Status:               docker-compose ps"
    echo ""
    echo "📝 Exemple de requête:"
    echo '  curl -X POST http://localhost:8084/api/v1/videos/generate \'
    echo '    -H "Content-Type: application/json" \'
    echo '    -H "Authorization: Bearer YOUR_TOKEN" \'
    echo '    -d '"'"'{'
    echo '      "prompt": "Une vidéo test",'
    echo '      "durationSeconds": 60,'
    echo '      "quality": "STANDARD"'
    echo '    }'"'"
    echo ""
    echo "📚 Documentation complète: README.md"
    echo ""
}

# Fonction principale
main() {
    print_header
    
    check_prerequisites
    setup_environment
    setup_database
    build_project
    start_services
    health_check
    display_info
}

# Exécution
main

# ============================================================================
# SCRIPT DE MONITORING
# Fichier: scripts/monitor.sh
# ============================================================================
#!/bin/bash

# Script de monitoring des services vidéo

# Afficher le statut de tous les services
show_status() {
    echo "═══════════════════════════════════════════════════"
    echo "  STATUS DES SERVICES VIDEO GENERATION"
    echo "═══════════════════════════════════════════════════"
    echo ""
    
    docker-compose ps
    
    echo ""
    echo "═══════════════════════════════════════════════════"
    echo "  STATISTIQUES KAFKA"
    echo "═══════════════════════════════════════════════════"
    echo ""
    
    # Messages en attente dans le topic requests
    docker-compose exec kafka kafka-run-class kafka.tools.GetOffsetShell \
        --broker-list localhost:9092 \
        --topic video.generation.requests \
        2>/dev/null || echo "Topic non créé"
}

# Afficher les métriques en temps réel
show_metrics() {
    echo "Récupération des métriques..."
    curl -s http://localhost:8084/actuator/metrics | jq .
}

# Afficher la file d'attente
show_queue() {
    echo "═══════════════════════════════════════════════════"
    echo "  FILE D'ATTENTE VIDÉO"
    echo "═══════════════════════════════════════════════════"
    echo ""
    
    curl -s http://localhost:8084/api/v1/videos/queue-status | jq .
}

# Afficher les logs récents
show_logs() {
    local service=${1:-video-service}
    
    echo "Logs récents pour: $service"
    echo "═══════════════════════════════════════════════════"
    docker-compose logs --tail=50 -f "$service"
}

# Menu principal
case "$1" in
    status)
        show_status
        ;;
    metrics)
        show_metrics
        ;;
    queue)
        show_queue
        ;;
    logs)
        show_logs "$2"
        ;;
    *)
        echo "Usage: $0 {status|metrics|queue|logs [service]}"
        echo ""
        echo "Exemples:"
        echo "  $0 status          # Afficher le statut de tous les services"
        echo "  $0 metrics         # Afficher les métriques Prometheus"
        echo "  $0 queue           # Afficher l'état de la file d'attente"
        echo "  $0 logs            # Afficher les logs du service vidéo"
        echo "  $0 logs worker-1   # Afficher les logs d'un worker spécifique"
        exit 1
        ;;
esac

# ============================================================================
# SCRIPT DE NETTOYAGE
# Fichier: scripts/cleanup.sh
# ============================================================================
#!/bin/bash

# Script de nettoyage des ressources

echo "⚠️  ATTENTION: Ce script va supprimer:"
echo "  - Tous les containers Docker du projet"
echo "  - Tous les volumes Docker du projet"
echo "  - Les fichiers temporaires"
echo ""
read -p "Êtes-vous sûr ? (y/N) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Nettoyage en cours..."
    
    # Arrêter tous les containers
    echo "Arrêt des containers..."
    docker-compose down
    
    # Supprimer les volumes
    echo "Suppression des volumes..."
    docker-compose down -v
    
    # Nettoyer les images inutilisées
    echo "Nettoyage des images Docker..."
    docker image prune -f
    
    # Nettoyer les fichiers temporaires
    echo "Nettoyage des fichiers temporaires..."
    rm -rf nexus-video-generation/target/
    rm -rf video-worker/__pycache__/
    
    echo "✓ Nettoyage terminé !"
else
    echo "Nettoyage annulé."
fi

# ============================================================================
# SCRIPT DE TEST DE CHARGE
# Fichier: scripts/load-test.sh
# ============================================================================
#!/bin/bash

# Script de test de charge pour l'API vidéo

API_URL=${1:-http://localhost:8084}
NUM_REQUESTS=${2:-10}
CONCURRENT=${3:-3}

echo "═══════════════════════════════════════════════════"
echo "  TEST DE CHARGE - VIDEO GENERATION API"
echo "═══════════════════════════════════════════════════"
echo ""
echo "  URL: $API_URL"
echo "  Nombre de requêtes: $NUM_REQUESTS"
echo "  Requêtes concurrentes: $CONCURRENT"
echo ""
echo "═══════════════════════════════════════════════════"
echo ""

# Fonction pour créer une vidéo
create_video() {
    local id=$1
    
    curl -s -w "\n%{http_code} %{time_total}s\n" \
        -X POST "$API_URL/api/v1/videos/generate" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer test-token" \
        -d "{
            \"prompt\": \"Test video $id\",
            \"durationSeconds\": 60,
            \"quality\": \"STANDARD\"
        }" &
}

# Lancer les requêtes
start_time=$(date +%s)

for i in $(seq 1 $NUM_REQUESTS); do
    create_video $i
    
    # Limiter le nombre de requêtes concurrentes
    if [ $((i % CONCURRENT)) -eq 0 ]; then
        wait
    fi
done

wait

end_time=$(date +%s)
duration=$((end_time - start_time))

echo ""
echo "═══════════════════════════════════════════════════"
echo "  RÉSULTATS"
echo "═══════════════════════════════════════════════════"
echo "  Temps total: ${duration}s"
echo "  Requêtes/seconde: $(echo "scale=2; $NUM_REQUESTS / $duration" | bc)"
echo ""

# ============================================================================
# SCRIPT DE BACKUP
# Fichier: scripts/backup.sh
# ============================================================================
#!/bin/bash

# Script de backup de la base de données

BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/video_db_$TIMESTAMP.sql"

echo "Création d'un backup de la base de données..."

# Créer le répertoire de backup si nécessaire
mkdir -p "$BACKUP_DIR"

# Effectuer le backup
docker-compose exec -T postgres pg_dump -U nexusai nexusai > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "✓ Backup créé avec succès: $BACKUP_FILE"
    
    # Compresser le backup
    gzip "$BACKUP_FILE"
    echo "✓ Backup compressé: $BACKUP_FILE.gz"
    
    # Garder seulement les 7 derniers backups
    ls -t "$BACKUP_DIR"/*.sql.gz | tail -n +8 | xargs -r rm
    echo "✓ Anciens backups nettoyés"
else
    echo "✗ Erreur lors du backup"
    exit 1
fi

# ============================================================================
# MAKEFILE POUR COMMANDES RAPIDES
# Fichier: Makefile
# ============================================================================

.PHONY: help install start stop restart logs test clean backup monitor

help:
	@echo "Commandes disponibles:"
	@echo "  make install   - Installation complète"
	@echo "  make start     - Démarrer tous les services"
	@echo "  make stop      - Arrêter tous les services"
	@echo "  make restart   - Redémarrer tous les services"
	@echo "  make logs      - Voir les logs en temps réel"
	@echo "  make test      - Lancer les tests"
	@echo "  make clean     - Nettoyer les ressources"
	@echo "  make backup    - Créer un backup de la DB"
	@echo "  make monitor   - Afficher le monitoring"

install:
	@./scripts/quick-start.sh

start:
	@docker-compose up -d
	@echo "✓ Services démarrés"

stop:
	@docker-compose down
	@echo "✓ Services arrêtés"

restart:
	@docker-compose restart
	@echo "✓ Services redémarrés"

logs:
	@docker-compose logs -f

test:
	@cd nexus-video-generation && mvn test

clean:
	@./scripts/cleanup.sh

backup:
	@./scripts/backup.sh

monitor:
	@./scripts/monitor.sh status

queue:
	@./scripts/monitor.sh queue
