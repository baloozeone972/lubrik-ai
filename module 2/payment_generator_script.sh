#!/bin/bash
# ============================================================================
# Script de Génération Complète du Module Payment
# À partir des artifacts de documentation Claude
# ============================================================================

set -e  # Arrêter en cas d'erreur

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}   NexusAI Payment Module - Générateur Complet${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo

# ============================================================================
# Configuration
# ============================================================================

OUTPUT_DIR="${1:-./nexusai-payment-service}"
TEMP_DIR="./temp_generator"

echo -e "${BLUE}📁 Dossier de sortie: $OUTPUT_DIR${NC}"
echo

# ============================================================================
# Étape 1 : Préparation
# ============================================================================

echo -e "${YELLOW}🔧 Étape 1/5 : Préparation de l'environnement${NC}"

# Créer dossiers temporaires
mkdir -p "$TEMP_DIR"
mkdir -p "$OUTPUT_DIR"

# Copier la classe générateur si elle existe
if [ -f "FileTreeGenerator.java" ]; then
    cp FileTreeGenerator.java "$TEMP_DIR/"
else
    echo -e "${RED}❌ FileTreeGenerator.java introuvable${NC}"
    echo -e "${YELLOW}Veuillez copier le code de la classe dans FileTreeGenerator.java${NC}"
    exit 1
fi

cd "$TEMP_DIR"

echo -e "${GREEN}✅ Environnement préparé${NC}"
echo

# ============================================================================
# Étape 2 : Compilation du Générateur
# ============================================================================

echo -e "${YELLOW}🔨 Étape 2/5 : Compilation du générateur${NC}"

javac FileTreeGenerator.java 2>/dev/null || {
    echo -e "${RED}❌ Erreur de compilation${NC}"
    echo -e "${YELLOW}Vérifiez que Java 21+ est installé:${NC}"
    java -version
    exit 1
}

echo -e "${GREEN}✅ Générateur compilé${NC}"
echo

# ============================================================================
# Étape 3 : Création du fichier de documentation consolidé
# ============================================================================

echo -e "${YELLOW}📝 Étape 3/5 : Consolidation de la documentation${NC}"

# Créer un fichier consolidé avec tout le contenu des artifacts
cat > nexusai-payment-complete.md << 'EOF'
# NexusAI Payment Module - Documentation Complète

Ce fichier contient tous les codes sources du Module 2 : Payment & Subscription System.

## Structure Maven Multi-Module

```xml
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
        <relativePath/>
    </parent>

    <groupId>com.nexusai</groupId>
    <artifactId>payment-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>payment-api</module>
        <module>payment-domain</module>
        <module>payment-infrastructure</module>
        <module>payment-application</module>
        <module>payment-web</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <stripe.version>24.15.0</stripe.version>
        <lombok.version>1.18.30</lombok.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

## Entité Subscription

```java
package com.nexusai.payment.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité représentant un abonnement utilisateur.
 */
@Entity
@Table(name = "subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "plan", nullable = false, length = 20)
    private String plan;
    
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    
    @Column(name = "start_date", nullable = false)
    private Instant startDate;
    
    @Column(name = "end_date")
    private Instant endDate;
    
    @Column(name = "monthly_price", nullable = false)
    private BigDecimal monthlyPrice;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
```

## Configuration Docker

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: nexusai-payment-db
    environment:
      POSTGRES_DB: nexusai_payment
      POSTGRES_USER: nexusai
      POSTGRES_PASSWORD: dev_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    container_name: nexusai-payment-redis
    ports:
      - "6379:6379"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: nexusai-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092

volumes:
  postgres_data:
```

## Configuration Application

```yaml
# src/main/resources/application.yml
server:
  port: 8082

spring:
  application:
    name: payment-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/nexusai_payment
    username: nexusai
    password: dev_password
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  
  kafka:
    bootstrap-servers: localhost:9092

logging:
  level:
    com.nexusai.payment: DEBUG
```

## Script de Setup

```bash
# scripts/dev-setup.sh
#!/bin/bash
set -e

echo "🚀 Setup Payment Service..."

# Démarrer infrastructure
docker-compose up -d

# Attendre que PostgreSQL soit prêt
echo "⏳ Attente PostgreSQL..."
sleep 10

# Compiler projet
mvn clean install -DskipTests

echo "✅ Setup terminé !"
```

## README Principal

```markdown
# NexusAI Payment Service

Module de gestion des paiements et abonnements.

## Démarrage Rapide

1. Lancer l'infrastructure :
   \`\`\`bash
   docker-compose up -d
   \`\`\`

2. Compiler le projet :
   \`\`\`bash
   mvn clean install
   \`\`\`

3. Lancer l'application :
   \`\`\`bash
   mvn spring-boot:run -pl payment-web
   \`\`\`

## Documentation

- API: http://localhost:8082/swagger-ui.html
- Actuator: http://localhost:8082/actuator

## Support

Email: payment-team@nexusai.com
```

EOF

echo -e "${GREEN}✅ Documentation consolidée créée${NC}"
echo

# ============================================================================
# Étape 4 : Génération de l'Arborescence
# ============================================================================

echo -e "${YELLOW}🏗️  Étape 4/5 : Génération de l'arborescence${NC}"
echo

# Exécuter le générateur
java FileTreeGenerator "../$OUTPUT_DIR" nexusai-payment-complete.md

echo
echo -e "${GREEN}✅ Arborescence générée${NC}"
echo

# ============================================================================
# Étape 5 : Création des Fichiers Additionnels
# ============================================================================

echo -e "${YELLOW}📦 Étape 5/5 : Création des fichiers additionnels${NC}"

cd "../$OUTPUT_DIR"

# Créer .gitignore
cat > .gitignore << 'EOF'
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties

# IDE
.idea/
*.iml
.vscode/
.classpath
.project
.settings/

# OS
.DS_Store
Thumbs.db

# Logs
*.log

# Env
.env
*.env
EOF

# Créer LICENSE
cat > LICENSE << 'EOF'
MIT License

Copyright (c) 2025 NexusAI

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
EOF

# Créer .env.example
cat > .env.example << 'EOF'
# Stripe Configuration
STRIPE_API_KEY=sk_test_your_test_key_here
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret_here

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/nexusai_payment
DATABASE_USER=nexusai
DATABASE_PASSWORD=change_me_in_production

# Kafka
KAFKA_BROKERS=localhost:9092

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
EOF

# Créer structure de dossiers manquante
mkdir -p scripts
mkdir -p k8s/production
mkdir -p k8s/staging
mkdir -p docs
mkdir -p db/migrations

echo -e "${GREEN}✅ Fichiers additionnels créés${NC}"
echo

# ============================================================================
# Étape 6 : Vérification et Rapport
# ============================================================================

echo
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}   📊 RAPPORT DE GÉNÉRATION${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo

# Compter fichiers
FILE_COUNT=$(find . -type f | wc -l)
DIR_COUNT=$(find . -type d | wc -l)
JAVA_COUNT=$(find . -name "*.java" | wc -l)
XML_COUNT=$(find . -name "*.xml" | wc -l)
YAML_COUNT=$(find . -name "*.yml" -o -name "*.yaml" | wc -l)

echo -e "${GREEN}📦 Fichiers créés: $FILE_COUNT${NC}"
echo -e "${GREEN}📁 Dossiers créés: $DIR_COUNT${NC}"
echo
echo -e "${YELLOW}Détails par type:${NC}"
echo -e "  • Java: $JAVA_COUNT fichiers"
echo -e "  • XML: $XML_COUNT fichiers"
echo -e "  • YAML: $YAML_COUNT fichiers"
echo

# Afficher arborescence
echo -e "${YELLOW}📁 Arborescence créée:${NC}"
echo

if command -v tree &> /dev/null; then
    tree -L 3 -I 'target' .
else
    find . -type d -maxdepth 3 | sed 's|./||' | sort
fi

echo
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo

# ============================================================================
# Étape 7 : Instructions Post-Génération
# ============================================================================

echo -e "${GREEN}✅ Génération terminée avec succès !${NC}"
echo
echo -e "${YELLOW}📝 Prochaines étapes:${NC}"
echo
echo "1. Configuration initiale:"
echo -e "   ${BLUE}cd $OUTPUT_DIR${NC}"
echo -e "   ${BLUE}cp .env.example .env${NC}"
echo -e "   ${BLUE}# Éditer .env avec vos clés Stripe${NC}"
echo
echo "2. Démarrer l'infrastructure:"
echo -e "   ${BLUE}docker-compose up -d${NC}"
echo
echo "3. Compiler le projet:"
echo -e "   ${BLUE}mvn clean install${NC}"
echo
echo "4. Lancer l'application:"
echo -e "   ${BLUE}mvn spring-boot:run -pl payment-web${NC}"
echo
echo "5. Accéder à l'application:"
echo -e "   ${BLUE}http://localhost:8082${NC}"
echo -e "   ${BLUE}http://localhost:8082/swagger-ui.html${NC}"
echo

# ============================================================================
# Nettoyage
# ============================================================================

echo -e "${YELLOW}🧹 Nettoyage des fichiers temporaires...${NC}"
cd ..
rm -rf "$TEMP_DIR"
echo -e "${GREEN}✅ Nettoyage terminé${NC}"
echo

echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}   🎉 Projet prêt à être développé !${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo

# Fin du script