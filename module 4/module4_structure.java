/**
 * MODULE 4 - CONVERSATION ENGINE
 * Architecture Modulaire Multi-Module Maven
 * 
 * Structure du projet:
 * 
 * nexusai-conversation-module/
 * ├── pom.xml (parent)
 * ├── conversation-api/          [Module 1 - APIs REST & WebSocket]
 * ├── conversation-core/         [Module 2 - Logique métier]
 * ├── conversation-llm/          [Module 3 - Intégration LLM]
 * ├── conversation-memory/       [Module 4 - Système de mémoire]
 * ├── conversation-persistence/  [Module 5 - Base de données]
 * └── conversation-common/       [Module 6 - Utilitaires communs]
 */

// ============================================================================
// POM.XML PARENT
// ============================================================================

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <groupId>com.nexusai</groupId>
    <artifactId>conversation-module</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <name>NexusAI Conversation Module</name>
    <description>Module de gestion des conversations avec les compagnons IA</description>
    
    <modules>
        <module>conversation-api</module>
        <module>conversation-core</module>
        <module>conversation-llm</module>
        <module>conversation-memory</module>
        <module>conversation-persistence</module>
        <module>conversation-common</module>
    </modules>
    
    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Versions des dépendances -->
        <spring-boot.version>3.2.0</spring-boot.version>
        <mongodb.version>4.11.1</mongodb.version>
        <redis.version>3.2.0</redis.version>
        <kafka.version>3.6.0</kafka.version>
        <openai.version>0.18.0</openai.version>
        <pinecone.version>0.7.0</pinecone.version>
        <lombok.version>1.18.30</lombok.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-webflux</artifactId>
                <version>${spring-boot.version}</version>
            </dependency>
            
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-websocket</artifactId>
                <version>${spring-boot.version}</version>
            </dependency>
            
            <!-- MongoDB -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-data-mongodb-reactive</artifactId>
                <version>${spring-boot.version}</version>
            </dependency>
            
            <!-- Redis -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
                <version>${spring-boot.version}</version>
            </dependency>
            
            <!-- Kafka -->
            <dependency>
                <groupId>org.springframework.kafka</groupId>
                <artifactId>spring-kafka</artifactId>
                <version>${kafka.version}</version>
            </dependency>
            
            <!-- OpenAI -->
            <dependency>
                <groupId>com.theokanning.openai-gpt3-java</groupId>
                <artifactId>service</artifactId>
                <version>${openai.version}</version>
            </dependency>
            
            <!-- Pinecone Vector DB -->
            <dependency>
                <groupId>io.pinecone</groupId>
                <artifactId>pinecone-client</artifactId>
                <version>${pinecone.version}</version>
            </dependency>
            
            <!-- Lombok -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
                <scope>provided</scope>
            </dependency>
            
            <!-- MapStruct -->
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
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
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
// STRUCTURE DES SOUS-MODULES
// ============================================================================

/**
 * RÉPARTITION DES TÂCHES ENTRE DÉVELOPPEURS:
 * 
 * DÉVELOPPEUR 1: conversation-api
 * - Controllers REST
 * - WebSocket handlers
 * - DTOs & validation
 * 
 * DÉVELOPPEUR 2: conversation-core
 * - Services métier
 * - Orchestration conversation
 * - Logique business
 * 
 * DÉVELOPPEUR 3: conversation-llm
 * - Intégration OpenAI
 * - Intégration Anthropic
 * - Gestion prompts
 * 
 * DÉVELOPPEUR 4: conversation-memory
 * - Mémoire court terme (Redis)
 * - Mémoire long terme (Vector DB)
 * - Contexte conversationnel
 * 
 * DÉVELOPPEUR 5: conversation-persistence
 * - Repositories MongoDB
 * - Entités
 * - Migrations
 */