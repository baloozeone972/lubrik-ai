#!/bin/bash

# Script d'installation COMPLET - Correction FINALE nexus-analytics

echo "🔧 Installation COMPLÈTE des corrections pour nexus-analytics..."
echo ""

PROJECT_DIR="/Users/baloozeone/W/DEV/lubrik-ai/lubrik ia"

# ========== ÉTAPE 1: Créer les dossiers ==========
echo "📁 Création des dossiers..."
mkdir -p "$PROJECT_DIR/nexus-analytics/src/main/java/com/nexusai/analytics/dto"
mkdir -p "$PROJECT_DIR/nexus-analytics/src/main/java/com/nexusai/analytics/repository"
mkdir -p "$PROJECT_DIR/nexus-analytics/src/main/java/com/nexusai/analytics/service"
mkdir -p "$PROJECT_DIR/nexus-core/src/main/java/com/nexusai/core/repository"

# ========== ÉTAPE 2: Installer les DTOs ==========
echo "📦 Installation des DTOs..."
cp EventDTO.java \
   "$PROJECT_DIR/nexus-analytics/src/main/java/com/nexusai/analytics/dto/"

cp MetricsDTO-CORRECTED.java \
   "$PROJECT_DIR/nexus-analytics/src/main/java/com/nexusai/analytics/dto/MetricsDTO.java"

cp AnalyticsEventDTO.java \
   "$PROJECT_DIR/nexus-analytics/src/main/java/com/nexusai/analytics/dto/"

cp TrackEventRequest.java \
   "$PROJECT_DIR/nexus-analytics/src/main/java/com/nexusai/analytics/dto/"

# ========== ÉTAPE 3: Installer les Repositories ==========
echo "📦 Installation des Repositories..."

# Repository CORE (pour AnalyticsService) - utilise timestamp
cp AnalyticsEventRepository-COMPLETE.java \
   "$PROJECT_DIR/nexus-core/src/main/java/com/nexusai/core/repository/AnalyticsEventRepository.java"

# Repository ANALYTICS (pour EventService) - utilise createdAt
cp AnalyticsEventRepository-nexus-analytics.java \
   "$PROJECT_DIR/nexus-analytics/src/main/java/com/nexusai/analytics/repository/AnalyticsEventRepository.java"

# ========== ÉTAPE 4: Installer les Services corrigés ==========
echo "📦 Installation des Services corrigés..."

cp AnalyticsService-CORRECTED.java \
   "$PROJECT_DIR/nexus-analytics/src/main/java/com/nexusai/analytics/service/AnalyticsService.java"

cp EventService-CORRECTED.java \
   "$PROJECT_DIR/nexus-analytics/src/main/java/com/nexusai/analytics/service/EventService.java"

echo ""
echo "✅ Installation terminée !"
echo ""
echo "📝 Fichiers installés :"
echo ""
echo "  DTOs (nexus-analytics/dto/):"
echo "    ✅ EventDTO.java"
echo "    ✅ MetricsDTO.java (corrigé sans userId)"
echo "    ✅ AnalyticsEventDTO.java"
echo "    ✅ TrackEventRequest.java"
echo ""
echo "  Repositories:"
echo "    ✅ AnalyticsEventRepository.java (nexus-core) - avec timestamp"
echo "    ✅ AnalyticsEventRepository.java (nexus-analytics) - avec createdAt"
echo ""
echo "  Services (nexus-analytics/service/):"
echo "    ✅ AnalyticsService.java (corrigé)"
echo "    ✅ EventService.java (corrigé)"
echo ""
echo "🔧 Prochaine étape : Compiler le projet"
echo "   cd \"$PROJECT_DIR\""
echo "   mvn clean compile"
echo ""
echo "💡 Corrections appliquées :"
echo "   - MetricsDTO sans userId"
echo "   - Conversion List<Map> vers Map<String,Long>"
echo "   - EventService utilise le repository local"
echo "   - Tous les DTOs nécessaires ajoutés"
