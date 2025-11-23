#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# SCRIPT D'ORGANISATION AUTOMATIQUE DES FICHIERS
# Module 10 : Analytics & Monitoring
# 
# Usage: ./organize-files.sh [output-directory]
# ═══════════════════════════════════════════════════════════════

set -e

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
OUTPUT_DIR="${1:-./nexusai-analytics}"
SOURCE_DIR="./source-files"

echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}MODULE 10 - ORGANISATION AUTOMATIQUE DES FICHIERS${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Vérifier Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java n'est pas installé !${NC}"
    echo "   Installez Java 21+ et réessayez."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${YELLOW}⚠️  Java $JAVA_VERSION détecté. Java 21+ est recommandé.${NC}"
fi

echo -e "${GREEN}✓ Java $JAVA_VERSION détecté${NC}"
echo ""

# Créer le répertoire de sortie
echo -e "${BLUE}📁 Création du répertoire de sortie...${NC}"
mkdir -p "$OUTPUT_DIR"
echo -e "${GREEN}   ✓ $OUTPUT_DIR${NC}"
echo ""

# Compiler l'utilitaire Java si nécessaire
if [ ! -f "FileOrganizerUtility.class" ]; then
    echo -e "${BLUE}☕ Compilation de l'utilitaire Java...${NC}"
    
    cat > FileOrganizerUtility.java << 'EOF'
// Coller ici le contenu de FileOrganizerUtility.java
EOF
    
    javac FileOrganizerUtility.java
    echo -e "${GREEN}   ✓ Compilé${NC}"
    echo ""
fi

# Exécuter l'organisation
echo -e "${BLUE}🚀 Lancement de l'organisation...${NC}"
echo ""

java FileOrganizerUtility "$SOURCE_DIR" "$OUTPUT_DIR"

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ ORGANISATION TERMINÉE AVEC SUCCÈS !${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Afficher la structure créée
echo -e "${BLUE}📂 Structure créée :${NC}"
echo ""
tree -L 2 "$OUTPUT_DIR" 2>/dev/null || find "$OUTPUT_DIR" -maxdepth 2 -type d | sort

echo ""
echo -e "${YELLOW}📋 Prochaines étapes :${NC}"
echo ""
echo -e "  1. ${BLUE}cd $OUTPUT_DIR${NC}"
echo -e "  2. ${BLUE}mvn clean install${NC}  (compiler le projet)"
echo -e "  3. ${BLUE}docker-compose up -d${NC}  (démarrer les services)"
echo -e "  4. Consulter le fichier ${YELLOW}REMAINING-TASKS.md${NC} pour voir ce qui reste à faire"
echo ""
echo -e "${GREEN}Bonne continuation ! 🚀${NC}"
echo ""

# ═══════════════════════════════════════════════════════════════
# FONCTION : Créer le fichier REMAINING-TASKS.md
# ═══════════════════════════════════════════════════════════════

create_remaining_tasks_file() {
    cat > "$OUTPUT_DIR/REMAINING-TASKS.md" << 'EOF'
# Ce qui reste à faire - Module 10

Consultez le fichier complet dans l'artifact "remaining-tasks-checklist"

## Résumé rapide

### Priorité HAUTE (Semaine 1-2)
- [ ] Implémenter complètement EventService, MetricService
- [ ] Compléter tous les Repositories ClickHouse
- [ ] Ajouter les fichiers de configuration (ClickHouseConfig, KafkaConfig, RedisConfig)
- [ ] Implémenter la gestion des erreurs et retry

### Priorité MOYENNE (Semaine 2-3)
- [ ] Compléter tous les tests (unitaires, intégration, E2E)
- [ ] Implémenter l'export PDF/Excel pour les rapports
- [ ] Créer les dashboards Grafana
- [ ] Configurer les alertes avancées

### Priorité BASSE (Semaine 3-4)
- [ ] Optimisations (cache, requêtes SQL)
- [ ] Sécurité (JWT, RBAC)
- [ ] Documentation API avancée
- [ ] Pipeline CI/CD

## Estimation
**18-20 jours** de travail total
- 1 développeur senior : 4 semaines
- 2 développeurs : 2-3 semaines
- 4 développeurs : 1-2 semaines
EOF
}

create_remaining_tasks_file

# ═══════════════════════════════════════════════════════════════
# FONCTION : Créer un Makefile utile
# ═══════════════════════════════════════════════════════════════

create_makefile() {
    cat > "$OUTPUT_DIR/Makefile" << 'EOF'
# Makefile pour Module 10 - Analytics & Monitoring

.PHONY: help build test run docker-build docker-up docker-down clean

help: ## Affiche cette aide
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

build: ## Compile le projet Maven
	mvn clean install -DskipTests

test: ## Lance tous les tests
	mvn test

test-integration: ## Lance les tests d'intégration
	mvn verify -P integration-tests

docker-build: ## Build l'image Docker
	docker build -t nexusai/analytics:latest .

docker-up: ## Démarre tous les services Docker
	docker-compose up -d

docker-down: ## Arrête tous les services Docker
	docker-compose down

docker-logs: ## Affiche les logs Docker
	docker-compose logs -f analytics-api

clean: ## Nettoie le projet
	mvn clean
	docker-compose down -v

deploy-k8s: ## Déploie sur Kubernetes
	kubectl apply -f k8s/

undeploy-k8s: ## Supprime le déploiement Kubernetes
	kubectl delete -f k8s/

init-clickhouse: ## Initialise les tables ClickHouse
	docker-compose exec clickhouse clickhouse-client --multiquery < sql/init-clickhouse.sql

status: ## Affiche le statut des services
	@echo "Docker Compose:"
	@docker-compose ps
	@echo ""
	@echo "Kubernetes:"
	@kubectl get pods -n nexusai -l app=analytics

health: ## Vérifie la santé des services
	@curl -s http://localhost:8080/actuator/health | jq .
EOF
}

create_makefile

echo -e "${GREEN}✓ Fichiers additionnels créés (Makefile, REMAINING-TASKS.md)${NC}"
echo ""
