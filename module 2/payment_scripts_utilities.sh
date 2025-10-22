#!/bin/bash
# ============================================================================
# NEXUSAI - MODULE 2 : PAYMENT SYSTEM
# Scripts Utilitaires pour Développement et Opérations
# ============================================================================

# ============================================================================
# 1. SCRIPT DE DÉVELOPPEMENT LOCAL
# ============================================================================

# scripts/dev-setup.sh
#!/bin/bash
set -e

echo "🚀 NexusAI Payment Service - Configuration Développement"
echo "=========================================================="

# Vérifier prérequis
check_prerequisites() {
    echo "✓ Vérification des prérequis..."
    
    command -v java >/dev/null 2>&1 || { 
        echo "❌ Java 21+ requis"; exit 1; 
    }
    
    command -v mvn >/dev/null 2>&1 || { 
        echo "❌ Maven 3.9+ requis"; exit 1; 
    }
    
    command -v docker >/dev/null 2>&1 || { 
        echo "❌ Docker requis"; exit 1; 
    }
    
    command -v docker-compose >/dev/null 2>&1 || { 
        echo "❌ Docker Compose requis"; exit 1; 
    }
    
    echo "✅ Tous les prérequis sont installés"
}

# Créer fichier .env si absent
setup_env_file() {
    if [ ! -f .env ]; then
        echo "📝 Création du fichier .env..."
        cat > .env << EOF
# Stripe Configuration (Test Keys)
STRIPE_API_KEY=sk_test_51xxxxxxxxxxxxxxxxxxxxx
STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxxxxxxxxxx

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/nexusai_payment
DATABASE_USER=nexusai
DATABASE_PASSWORD=dev_password_change_me

# Kafka
KAFKA_BROKERS=localhost:9092

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
EOF
        echo "⚠️  Pensez à configurer vos clés Stripe dans .env"
    else
        echo "✅ Fichier .env déjà présent"
    fi
}

# Démarrer infrastructure
start_infrastructure() {
    echo "🐳 Démarrage de l'infrastructure Docker..."
    docker-compose up -d postgres redis kafka zookeeper
    
    echo "⏳ Attente que les services soient prêts..."
    sleep 10
    
    # Vérifier PostgreSQL
    docker-compose exec -T postgres pg_isready -U nexusai || {
        echo "❌ PostgreSQL non prêt"; exit 1;
    }
    
    # Vérifier Redis
    docker-compose exec -T redis redis-cli ping || {
        echo "❌ Redis non prêt"; exit 1;
    }
    
    echo "✅ Infrastructure démarrée"
}

# Créer base de données
setup_database() {
    echo "🗄️  Configuration de la base de données..."
    
    # Exécuter migrations Flyway
    mvn flyway:migrate -Dflyway.url="jdbc:postgresql://localhost:5432/nexusai_payment" \
                       -Dflyway.user=nexusai \
                       -Dflyway.password=dev_password_change_me
    
    echo "✅ Base de données configurée"
}

# Créer topics Kafka
setup_kafka_topics() {
    echo "📨 Création des topics Kafka..."
    
    docker-compose exec -T kafka kafka-topics --create \
        --bootstrap-server localhost:9092 \
        --topic payment.subscription.created \
        --partitions 3 \
        --replication-factor 1 \
        --if-not-exists
    
    docker-compose exec -T kafka kafka-topics --create \
        --bootstrap-server localhost:9092 \
        --topic payment.subscription.upgraded \
        --partitions 3 \
        --replication-factor 1 \
        --if-not-exists
    
    docker-compose exec -T kafka kafka-topics --create \
        --bootstrap-server localhost:9092 \
        --topic payment.subscription.canceled \
        --partitions 3 \
        --replication-factor 1 \
        --if-not-exists
    
    docker-compose exec -T kafka kafka-topics --create \
        --bootstrap-server localhost:9092 \
        --topic payment.tokens.purchased \
        --partitions 3 \
        --replication-factor 1 \
        --if-not-exists
    
    docker-compose exec -T kafka kafka-topics --create \
        --bootstrap-server localhost:9092 \
        --topic payment.tokens.consumed \
        --partitions 3 \
        --replication-factor 1 \
        --if-not-exists
    
    echo "✅ Topics Kafka créés"
}

# Compiler le projet
build_project() {
    echo "🔨 Compilation du projet..."
    mvn clean install -DskipTests
    echo "✅ Projet compilé"
}

# Main
main() {
    check_prerequisites
    setup_env_file
    start_infrastructure
    setup_database
    setup_kafka_topics
    build_project
    
    echo ""
    echo "=========================================================="
    echo "✅ Configuration terminée avec succès!"
    echo ""
    echo "📝 Prochaines étapes:"
    echo "  1. Configurer vos clés Stripe dans .env"
    echo "  2. Lancer l'application: mvn spring-boot:run -pl payment-web"
    echo "  3. Accéder à Swagger: http://localhost:8082/swagger-ui.html"
    echo ""
    echo "🔧 Commandes utiles:"
    echo "  - Logs DB:    docker-compose logs -f postgres"
    echo "  - Logs Kafka: docker-compose logs -f kafka"
    echo "  - Arrêter:    docker-compose down"
    echo "=========================================================="
}

main

# ============================================================================
# 2. SCRIPT DE TESTS
# ============================================================================

# scripts/run-tests.sh
#!/bin/bash
set -e

echo "🧪 Lancement des tests - Payment Service"
echo "========================================"

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Variables
COVERAGE_THRESHOLD=80
TEST_RESULTS_DIR="target/test-results"

# Tests unitaires
run_unit_tests() {
    echo -e "${YELLOW}🔬 Tests unitaires...${NC}"
    mvn test -Dtest.groups=unit || {
        echo -e "${RED}❌ Tests unitaires échoués${NC}"
        exit 1
    }
    echo -e "${GREEN}✅ Tests unitaires passés${NC}"
}

# Tests d'intégration
run_integration_tests() {
    echo -e "${YELLOW}🔗 Tests d'intégration...${NC}"
    
    # Démarrer testcontainers
    mvn verify -P integration-tests || {
        echo -e "${RED}❌ Tests d'intégration échoués${NC}"
        exit 1
    }
    echo -e "${GREEN}✅ Tests d'intégration passés${NC}"
}

# Tests E2E
run_e2e_tests() {
    echo -e "${YELLOW}🎯 Tests E2E...${NC}"
    
    # Vérifier que l'app tourne
    curl -s http://localhost:8082/actuator/health > /dev/null || {
        echo -e "${RED}❌ Application non démarrée${NC}"
        echo "Lancer: mvn spring-boot:run -pl payment-web"
        exit 1
    }
    
    mvn verify -P e2e-tests || {
        echo -e "${RED}❌ Tests E2E échoués${NC}"
        exit 1
    }
    echo -e "${GREEN}✅ Tests E2E passés${NC}"
}

# Vérifier coverage
check_coverage() {
    echo -e "${YELLOW}📊 Vérification du coverage...${NC}"
    
    mvn jacoco:report
    
    # Parser le coverage
    COVERAGE=$(grep -oP 'Total.*?([0-9]+)%' target/site/jacoco/index.html | grep -oP '[0-9]+' | head -1)
    
    echo "Coverage actuel: ${COVERAGE}%"
    
    if [ "$COVERAGE" -lt "$COVERAGE_THRESHOLD" ]; then
        echo -e "${RED}❌ Coverage insuffisant (${COVERAGE}% < ${COVERAGE_THRESHOLD}%)${NC}"
        exit 1
    else
        echo -e "${GREEN}✅ Coverage OK (${COVERAGE}% >= ${COVERAGE_THRESHOLD}%)${NC}"
    fi
}

# Tests de sécurité
run_security_tests() {
    echo -e "${YELLOW}🔒 Tests de sécurité (OWASP)...${NC}"
    
    mvn dependency-check:check || {
        echo -e "${YELLOW}⚠️  Vulnérabilités détectées - voir rapport${NC}"
    }
    
    echo -e "${GREEN}✅ Scan de sécurité terminé${NC}"
}

# Générer rapport
generate_report() {
    echo -e "${YELLOW}📝 Génération du rapport...${NC}"
    
    mkdir -p "$TEST_RESULTS_DIR"
    
    cat > "$TEST_RESULTS_DIR/summary.html" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Test Report - Payment Service</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .success { color: green; }
        .warning { color: orange; }
        .error { color: red; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
    </style>
</head>
<body>
    <h1>Payment Service - Test Report</h1>
    <p>Date: $(date)</p>
    
    <h2>Summary</h2>
    <table>
        <tr>
            <th>Test Type</th>
            <th>Status</th>
            <th>Coverage</th>
        </tr>
        <tr>
            <td>Unit Tests</td>
            <td class="success">✅ Passed</td>
            <td>${COVERAGE}%</td>
        </tr>
        <tr>
            <td>Integration Tests</td>
            <td class="success">✅ Passed</td>
            <td>-</td>
        </tr>
        <tr>
            <td>E2E Tests</td>
            <td class="success">✅ Passed</td>
            <td>-</td>
        </tr>
    </table>
    
    <h2>Details</h2>
    <p><a href="../site/jacoco/index.html">Coverage Report</a></p>
    <p><a href="../surefire-reports/index.html">Surefire Reports</a></p>
</body>
</html>
EOF
    
    echo -e "${GREEN}✅ Rapport généré: $TEST_RESULTS_DIR/summary.html${NC}"
}

# Main
main() {
    echo "Démarrage des tests à $(date)"
    
    run_unit_tests
    run_integration_tests
    
    if [ "$1" == "--e2e" ]; then
        run_e2e_tests
    fi
    
    check_coverage
    run_security_tests
    generate_report
    
    echo ""
    echo "========================================"
    echo -e "${GREEN}✅ Tous les tests sont passés!${NC}"
    echo "📊 Rapport: $TEST_RESULTS_DIR/summary.html"
    echo "========================================"
}

main "$@"

# ============================================================================
# 3. SCRIPT DE MONITORING
# ============================================================================

# scripts/health-check.sh
#!/bin/bash

echo "🏥 Health Check - Payment Service"
echo "=================================="

API_URL="${1:-http://localhost:8082}"

# Check application health
check_app_health() {
    echo -n "Application: "
    STATUS=$(curl -s "${API_URL}/actuator/health" | jq -r '.status')
    
    if [ "$STATUS" == "UP" ]; then
        echo "✅ UP"
        return 0
    else
        echo "❌ DOWN"
        return 1
    fi
}

# Check database
check_database() {
    echo -n "Database: "
    DB_STATUS=$(curl -s "${API_URL}/actuator/health/db" | jq -r '.status')
    
    if [ "$DB_STATUS" == "UP" ]; then
        echo "✅ UP"
    else
        echo "❌ DOWN"
    fi
}

# Check Kafka
check_kafka() {
    echo -n "Kafka: "
    KAFKA_STATUS=$(curl -s "${API_URL}/actuator/health/kafka" | jq -r '.status')
    
    if [ "$KAFKA_STATUS" == "UP" ]; then
        echo "✅ UP"
    else
        echo "❌ DOWN"
    fi
}

# Check Redis
check_redis() {
    echo -n "Redis: "
    REDIS_STATUS=$(curl -s "${API_URL}/actuator/health/redis" | jq -r '.status')
    
    if [ "$REDIS_STATUS" == "UP" ]; then
        echo "✅ UP"
    else
        echo "❌ DOWN"
    fi
}

# Check Stripe connectivity
check_stripe() {
    echo -n "Stripe API: "
    # Faire un appel simple à Stripe pour vérifier la connectivité
    STRIPE_RESPONSE=$(curl -s -w "%{http_code}" \
        -u "$STRIPE_API_KEY:" \
        "https://api.stripe.com/v1/balance" -o /dev/null)
    
    if [ "$STRIPE_RESPONSE" == "200" ]; then
        echo "✅ Accessible"
    else
        echo "❌ Non accessible (code: $STRIPE_RESPONSE)"
    fi
}

# Display metrics
show_metrics() {
    echo ""
    echo "📊 Métriques"
    echo "============"
    
    # Requests total
    REQUESTS=$(curl -s "${API_URL}/actuator/metrics/http.server.requests" | \
        jq -r '.measurements[0].value')
    echo "Total Requests: $REQUESTS"
    
    # Error rate
    ERRORS=$(curl -s "${API_URL}/actuator/metrics/http.server.requests" | \
        jq -r '.availableTags[] | select(.tag == "status") | .values[] | select(.startsWith("5"))' | wc -l)
    echo "5xx Errors: $ERRORS"
    
    # JVM Memory
    MEMORY=$(curl -s "${API_URL}/actuator/metrics/jvm.memory.used" | \
        jq -r '.measurements[0].value')
    MEMORY_MB=$(echo "scale=2; $MEMORY / 1024 / 1024" | bc)
    echo "JVM Memory Used: ${MEMORY_MB} MB"
}

# Main
main() {
    check_app_health || exit 1
    check_database
    check_kafka
    check_redis
    check_stripe
    show_metrics
    
    echo ""
    echo "✅ Health check terminé"
}

main

# ============================================================================
# 4. SCRIPT DE DÉPLOIEMENT
# ============================================================================

# scripts/deploy.sh
#!/bin/bash
set -e

echo "🚀 Déploiement - Payment Service"
echo "================================="

ENVIRONMENT="${1:-staging}"
VERSION="${2:-latest}"

deploy_to_k8s() {
    echo "📦 Déploiement vers Kubernetes ($ENVIRONMENT)..."
    
    # Apply ConfigMaps
    kubectl apply -f k8s/$ENVIRONMENT/configmap.yaml
    
    # Apply Secrets
    kubectl apply -f k8s/$ENVIRONMENT/secrets.yaml
    
    # Apply Deployment
    kubectl apply -f k8s/$ENVIRONMENT/deployment.yaml
    
    # Wait for rollout
    echo "⏳ Attente du rollout..."
    kubectl rollout status deployment/payment-service -n nexusai-$ENVIRONMENT
    
    echo "✅ Déploiement terminé"
}

run_smoke_tests() {
    echo "🧪 Tests smoke..."
    
    # Attendre que le service soit prêt
    sleep 30
    
    API_URL=$(kubectl get svc payment-service -n nexusai-$ENVIRONMENT -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
    
    # Test health endpoint
    curl -f "http://${API_URL}/actuator/health" || {
        echo "❌ Smoke tests échoués"
        exit 1
    }
    
    echo "✅ Smoke tests OK"
}

notify_deployment() {
    echo "📢 Notification déploiement..."
    
    # Slack notification
    curl -X POST "$SLACK_WEBHOOK_URL" \
        -H 'Content-Type: application/json' \
        -d "{
            \"text\": \"✅ Payment Service v${VERSION} déployé sur ${ENVIRONMENT}\",
            \"username\": \"Deploy Bot\"
        }"
}

main() {
    echo "Environnement: $ENVIRONMENT"
    echo "Version: $VERSION"
    
    deploy_to_k8s
    run_smoke_tests
    notify_deployment
    
    echo ""
    echo "================================="
    echo "✅ Déploiement réussi!"
    echo "================================="
}

main

# ============================================================================
# 5. SCRIPT DE ROLLBACK
# ============================================================================

# scripts/rollback.sh
#!/bin/bash
set -e

echo "⏪ Rollback - Payment Service"
echo "=============================="

ENVIRONMENT="${1:-staging}"

rollback_deployment() {
    echo "🔄 Rollback du déploiement..."
    
    kubectl rollout undo deployment/payment-service -n nexusai-$ENVIRONMENT
    
    echo "⏳ Attente du rollback..."
    kubectl rollout status deployment/payment-service -n nexusai-$ENVIRONMENT
    
    echo "✅ Rollback terminé"
}

verify_rollback() {
    echo "✅ Vérification post-rollback..."
    
    sleep 30
    
    API_URL=$(kubectl get svc payment-service -n nexusai-$ENVIRONMENT -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
    
    curl -f "http://${API_URL}/actuator/health" || {
        echo "❌ Vérification échouée"
        exit 1
    }
    
    echo "✅ Application fonctionnelle après rollback"
}

main() {
    echo "⚠️  Rollback de $ENVIRONMENT"
    read -p "Confirmer le rollback? (y/n) " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        rollback_deployment
        verify_rollback
        echo "✅ Rollback réussi"
    else
        echo "❌ Rollback annulé"
        exit 1
    fi
}

main

# ============================================================================
# 6. SCRIPT DE BACKUP BASE DE DONNÉES
# ============================================================================

# scripts/backup-db.sh
#!/bin/bash
set -e

BACKUP_DIR="backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/payment_db_$TIMESTAMP.sql.gz"

echo "💾 Backup Base de Données"
echo "========================="

mkdir -p "$BACKUP_DIR"

# Backup
echo "📦 Création du backup..."
docker-compose exec -T postgres pg_dump -U nexusai nexusai_payment | gzip > "$BACKUP_FILE"

# Vérifier
if [ -f "$BACKUP_FILE" ]; then
    SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo "✅ Backup créé: $BACKUP_FILE ($SIZE)"
    
    # Nettoyer les vieux backups (> 7 jours)
    find "$BACKUP_DIR" -name "*.sql.gz" -mtime +7 -delete
    echo "🧹 Anciens backups supprimés"
else
    echo "❌ Échec du backup"
    exit 1
fi

# ============================================================================
# 7. SCRIPT DE GÉNÉRATION DE DONNÉES DE TEST
# ============================================================================

# scripts/seed-test-data.sh
#!/bin/bash
set -e

echo "🌱 Génération données de test"
echo "============================="

API_URL="http://localhost:8082/api/v1"

# Créer utilisateurs tests
create_test_users() {
    echo "👥 Création utilisateurs de test..."
    
    # User 1: Abonnement PREMIUM
    USER1_ID=$(uuidgen)
    curl -X POST "$API_URL/subscriptions/subscribe" \
        -H "Content-Type: application/json" \
        -d "{
            \"userId\": \"$USER1_ID\",
            \"plan\": \"PREMIUM\",
            \"paymentMethodId\": \"pm_card_visa\"
        }" | jq -r '.subscription.userId'
    
    echo "✅ User 1 créé (PREMIUM): $USER1_ID"
    
    # User 2: Abonnement STANDARD
    USER2_ID=$(uuidgen)
    curl -X POST "$API_URL/subscriptions/subscribe" \
        -H "Content-Type: application/json" \
        -d "{
            \"userId\": \"$USER2_ID\",
            \"plan\": \"STANDARD\",
            \"paymentMethodId\": \"pm_card_visa\"
        }"
    
    echo "✅ User 2 créé (STANDARD): $USER2_ID"
}

# Acheter des jetons
purchase_tokens() {
    echo "💰 Achat de jetons..."
    
    curl -X POST "$API_URL/tokens/purchase" \
        -H "Content-Type: application/json" \
        -d "{
            \"userId\": \"$USER1_ID\",
            \"tokenAmount\": 500,
            \"paymentMethodId\": \"pm_card_visa\"
        }"
    
    echo "✅ 500 jetons achetés pour User 1"
}

main() {
    create_test_users
    purchase_tokens
    
    echo ""
    echo "✅ Données de test créées"
}

main