/**
 * NEXUSAI - MODULE 2 : PAYMENT & SUBSCRIPTION SYSTEM
 * Structure du projet Maven multi-module
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-10-18
 */

/*
 * ============================================================================
 * STRUCTURE DU PROJET
 * ============================================================================
 * 
 * nexusai-payment-service/
 * ├── pom.xml (parent)
 * ├── payment-api/           → Contrats API (DTOs, interfaces)
 * │   └── src/main/java/com/nexusai/payment/api/
 * ├── payment-domain/        → Logique métier (entities, use cases)
 * │   └── src/main/java/com/nexusai/payment/domain/
 * ├── payment-infrastructure/→ Implémentations (Stripe, DB, Kafka)
 * │   └── src/main/java/com/nexusai/payment/infrastructure/
 * ├── payment-application/   → Services applicatifs (orchestration)
 * │   └── src/main/java/com/nexusai/payment/application/
 * └── payment-web/           → REST Controllers
 *     └── src/main/java/com/nexusai/payment/web/
 * 
 * Cette structure permet :
 * ✓ Séparation des responsabilités (Clean Architecture)
 * ✓ Travail en parallèle sur différents modules
 * ✓ Tests indépendants par couche
 * ✓ Réutilisation du code
 * ✓ Déploiement flexible
 */

// ============================================================================
// pom.xml (PARENT)
// ============================================================================

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

    <name>NexusAI Payment Service</name>
    <description>Module de gestion des paiements et abonnements</description>

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
        
        <!-- Versions des dépendances -->
        <stripe.version>24.15.0</stripe.version>
        <kafka.version>3.6.0</kafka.version>
        <lombok.version>1.18.30</lombok.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Modules internes -->
            <dependency>
                <groupId>com.nexusai</groupId>
                <artifactId>payment-api</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.nexusai</groupId>
                <artifactId>payment-domain</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.nexusai</groupId>
                <artifactId>payment-infrastructure</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.nexusai</groupId>
                <artifactId>payment-application</artifactId>
                <version>${project.version}</version>
            </dependency>

            <!-- Stripe -->
            <dependency>
                <groupId>com.stripe</groupId>
                <artifactId>stripe-java</artifactId>
                <version>${stripe.version}</version>
            </dependency>

            <!-- Kafka -->
            <dependency>
                <groupId>org.springframework.kafka</groupId>
                <artifactId>spring-kafka</artifactId>
                <version>${kafka.version}</version>
            </dependency>

            <!-- Lombok -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
                <scope>provided</scope>
            </dependency>

            <!-- MapStruct (mapping DTOs) -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>

// ============================================================================
// GUIDE DE DÉVELOPPEMENT EN ÉQUIPE
// ============================================================================

/**
 * RÉPARTITION DES TÂCHES PAR ÉQUIPE
 * 
 * ÉQUIPE 1 (Backend Core) - 2 développeurs
 * ├─ Developer 1: payment-domain + payment-api
 * │  └─ Tâches: Entités, Use Cases, DTOs, Interfaces
 * └─ Developer 2: payment-application
 *    └─ Tâches: Services applicatifs, Orchestration
 * 
 * ÉQUIPE 2 (Infrastructure) - 2 développeurs
 * ├─ Developer 3: payment-infrastructure (Stripe)
 * │  └─ Tâches: Intégration Stripe, Repository
 * └─ Developer 4: payment-infrastructure (Kafka + Events)
 *    └─ Tâches: Event publishers, Listeners
 * 
 * ÉQUIPE 3 (API) - 1 développeur
 * └─ Developer 5: payment-web
 *    └─ Tâches: REST Controllers, Validation, Documentation
 * 
 * ÉQUIPE 4 (Tests & QA) - 1 développeur
 * └─ Developer 6: Tests
 *    └─ Tâches: Tests unitaires, Tests intégration, Tests E2E
 * 
 * WORKFLOW GITFLOW:
 * main → develop → feature/payment-{module}-{feature}
 * 
 * BRANCHES PAR DÉVELOPPEUR:
 * - feature/payment-domain-entities
 * - feature/payment-domain-usecases
 * - feature/payment-stripe-integration
 * - feature/payment-kafka-events
 * - feature/payment-api-controllers
 * - feature/payment-tests
 */

// ============================================================================
// CONVENTIONS DE NOMMAGE
// ============================================================================

/**
 * PACKAGES:
 * - com.nexusai.payment.api.dto        → DTOs (Data Transfer Objects)
 * - com.nexusai.payment.api.request    → Requêtes API
 * - com.nexusai.payment.api.response   → Réponses API
 * - com.nexusai.payment.domain.entity  → Entités métier
 * - com.nexusai.payment.domain.usecase → Cas d'usage
 * - com.nexusai.payment.domain.port    → Interfaces (ports)
 * - com.nexusai.payment.infrastructure.stripe → Implémentation Stripe
 * - com.nexusai.payment.infrastructure.repository → Repositories JPA
 * - com.nexusai.payment.infrastructure.event → Event handling
 * - com.nexusai.payment.application.service → Services applicatifs
 * - com.nexusai.payment.web.controller → REST Controllers
 * 
 * CLASSES:
 * - Entity: Subscription, TokenWallet, PaymentTransaction
 * - UseCase: CreateSubscriptionUseCase, PurchaseTokensUseCase
 * - Service: SubscriptionService, TokenService
 * - Controller: SubscriptionController, TokenController
 * - Repository: SubscriptionRepository, TokenWalletRepository
 * - DTO: SubscriptionDTO, TokenTransactionDTO
 * - Request: CreateSubscriptionRequest, PurchaseTokensRequest
 * - Response: SubscriptionResponse, TokenBalanceResponse
 */

// ============================================================================
// DOCUMENTATION JAVADOC - STANDARDS
// ============================================================================

/**
 * Standard de documentation pour TOUTES les classes et méthodes publiques.
 * 
 * Template pour les classes:
 * 
 * /**
 *  * Description courte de la classe.
 *  * 
 *  * <p>Description détaillée avec contexte et responsabilités.</p>
 *  * 
 *  * <p><b>Responsabilités:</b></p>
 *  * <ul>
 *  *   <li>Responsabilité 1</li>
 *  *   <li>Responsabilité 2</li>
 *  * </ul>
 *  * 
 *  * <p><b>Dépendances:</b></p>
 *  * <ul>
 *  *   <li>Module X pour Y</li>
 *  * </ul>
 *  * 
 *  * @author Nom du développeur
 *  * @since 1.0
 *  * @see ClasseReliée
 *  * /
 * 
 * Template pour les méthodes:
 * 
 * /**
 *  * Description courte de ce que fait la méthode.
 *  * 
 *  * <p>Description détaillée avec contexte.</p>
 *  * 
 *  * <p><b>Exemple d'utilisation:</b></p>
 *  * <pre>{@code
 *  * Subscription sub = service.createSubscription(userId, SubscriptionPlan.PREMIUM);
 *  * }</pre>
 *  * 
 *  * @param paramName Description du paramètre
 *  * @return Description du retour
 *  * @throws ExceptionType Description de quand l'exception est levée
 *  * /
 */