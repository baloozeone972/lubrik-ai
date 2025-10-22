package com.nexusai.tools;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import lombok.extern.slf4j.Slf4j;

/**
 * GÉNÉRATEUR AUTOMATIQUE DE STRUCTURE DE PROJET
 * 
 * Utilitaire qui parse les fichiers de documentation contenant du code
 * et génère automatiquement la structure complète du projet avec tous
 * les fichiers aux bons emplacements.
 * 
 * Usage:
 * <pre>
 * ProjectGenerator generator = new ProjectGenerator();
 * generator.setOutputPath("/path/to/output");
 * generator.parseAndGenerate("nexusai-complete-docs.md");
 * </pre>
 * 
 * @author NexusAI Dev Team
 * @version 1.0.0
 */
@Slf4j
public class ProjectFileGenerator {
    
    private String outputPath;
    private Map<String, String> fileContents = new HashMap<>();
    private Set<String> directories = new HashSet<>();
    
    // Patterns pour détecter les blocs de code
    private static final Pattern CODE_BLOCK_PATTERN = 
        Pattern.compile("```(\\w+)?\\s*\\n([\\s\\S]*?)\\n```");
    
    private static final Pattern FILE_PATH_PATTERN = 
        Pattern.compile("(?://|#|<!--|/\\*)\\s*([\\w/.-]+\\.(java|xml|yml|yaml|properties|json|sh|sql|js|ts|tsx|css|html|md))");
    
    /**
     * Constructeur avec chemin de sortie
     */
    public ProjectFileGenerator(String outputPath) {
        this.outputPath = outputPath;
    }
    
    /**
     * Parse un fichier de documentation et extrait tous les fichiers de code
     * 
     * @param documentPath Chemin vers le fichier de documentation
     * @throws IOException Si erreur lecture fichier
     */
    public void parseAndGenerate(String documentPath) throws IOException {
        log.info("Parsing document: {}", documentPath);
        
        String content = Files.readString(Path.of(documentPath));
        
        // Extraire tous les blocs de code
        extractCodeBlocks(content);
        
        // Générer les fichiers
        generateFiles();
        
        log.info("Génération terminée. {} fichiers créés dans {}", 
                 fileContents.size(), outputPath);
    }
    
    /**
     * Extrait tous les blocs de code du document
     */
    private void extractCodeBlocks(String content) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String language = matcher.group(1);
            String code = matcher.group(2);
            
            if (language != null && !language.isEmpty()) {
                extractFile(code, language);
            }
        }
    }
    
    /**
     * Extrait un fichier depuis un bloc de code
     */
    private void extractFile(String code, String language) {
        // Détecter le chemin du fichier dans les commentaires
        String filePath = detectFilePath(code, language);
        
        if (filePath != null) {
            log.debug("Fichier détecté: {}", filePath);
            fileContents.put(filePath, code);
            
            // Ajouter le répertoire parent
            String directory = new File(filePath).getParent();
            if (directory != null) {
                directories.add(directory);
            }
        } else if (isJavaClass(code)) {
            // Si c'est une classe Java, extraire le package et le nom
            filePath = extractJavaFilePath(code);
            if (filePath != null) {
                log.debug("Classe Java détectée: {}", filePath);
                fileContents.put(filePath, code);
                directories.add(new File(filePath).getParent());
            }
        }
    }
    
    /**
     * Détecte le chemin du fichier dans les commentaires
     */
    private String detectFilePath(String code, String language) {
        Matcher matcher = FILE_PATH_PATTERN.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Vérifie si c'est du code Java
     */
    private boolean isJavaClass(String code) {
        return code.contains("package ") && 
               (code.contains("class ") || 
                code.contains("interface ") || 
                code.contains("enum "));
    }
    
    /**
     * Extrait le chemin d'un fichier Java depuis son package et nom de classe
     */
    private String extractJavaFilePath(String code) {
        // Extraire le package
        Pattern packagePattern = Pattern.compile("package\\s+([\\w.]+);");
        Matcher packageMatcher = packagePattern.matcher(code);
        
        String packageName = null;
        if (packageMatcher.find()) {
            packageName = packageMatcher.group(1);
        }
        
        // Extraire le nom de la classe/interface/enum
        Pattern classPattern = Pattern.compile(
            "(?:public\\s+)?(?:class|interface|enum)\\s+(\\w+)");
        Matcher classMatcher = classPattern.matcher(code);
        
        String className = null;
        if (classMatcher.find()) {
            className = classMatcher.group(1);
        }
        
        if (packageName != null && className != null) {
            // Déterminer le module
            String module = determineModule(packageName);
            
            // Construire le chemin
            String packagePath = packageName.replace('.', '/');
            return String.format("%s/src/main/java/%s/%s.java", 
                                module, packagePath, className);
        }
        
        return null;
    }
    
    /**
     * Détermine le module à partir du package
     */
    private String determineModule(String packageName) {
        if (packageName.contains(".common")) {
            return "conversation-common";
        } else if (packageName.contains(".api")) {
            return "conversation-api";
        } else if (packageName.contains(".core")) {
            return "conversation-core";
        } else if (packageName.contains(".llm")) {
            return "conversation-llm";
        } else if (packageName.contains(".memory")) {
            return "conversation-memory";
        } else if (packageName.contains(".persistence")) {
            return "conversation-persistence";
        }
        return "conversation-core"; // Par défaut
    }
    
    /**
     * Génère tous les fichiers extraits
     */
    private void generateFiles() throws IOException {
        Path basePath = Paths.get(outputPath);
        
        // Créer le répertoire de base s'il n'existe pas
        Files.createDirectories(basePath);
        
        // Créer tous les répertoires
        for (String directory : directories) {
            Path dirPath = basePath.resolve(directory);
            Files.createDirectories(dirPath);
            log.debug("Répertoire créé: {}", dirPath);
        }
        
        // Créer tous les fichiers
        for (Map.Entry<String, String> entry : fileContents.entrySet()) {
            String filePath = entry.getKey();
            String content = entry.getValue();
            
            Path fullPath = basePath.resolve(filePath);
            
            // S'assurer que le répertoire parent existe
            Files.createDirectories(fullPath.getParent());
            
            // Écrire le fichier
            Files.writeString(fullPath, content);
            log.info("Fichier créé: {}", fullPath);
        }
    }
    
    /**
     * Génère la structure Maven complète
     */
    public void generateMavenStructure() throws IOException {
        log.info("Génération structure Maven...");
        
        Path basePath = Paths.get(outputPath);
        
        String[] modules = {
            "conversation-common",
            "conversation-api",
            "conversation-core",
            "conversation-llm",
            "conversation-memory",
            "conversation-persistence"
        };
        
        for (String module : modules) {
            // Créer la structure Maven standard
            String[] paths = {
                module + "/src/main/java",
                module + "/src/main/resources",
                module + "/src/test/java",
                module + "/src/test/resources",
                module + "/target"
            };
            
            for (String path : paths) {
                Files.createDirectories(basePath.resolve(path));
            }
            
            log.info("Structure Maven créée pour: {}", module);
        }
    }
    
    /**
     * Génère les fichiers de configuration supplémentaires
     */
    public void generateConfigFiles() throws IOException {
        log.info("Génération fichiers de configuration...");
        
        Path basePath = Paths.get(outputPath);
        
        // .gitignore
        generateGitIgnore(basePath);
        
        // README.md
        generateReadme(basePath);
        
        // Scripts utilitaires
        generateScripts(basePath);
    }
    
    private void generateGitIgnore(Path basePath) throws IOException {
        String gitignore = """
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
            .eclipse/
            
            # OS
            .DS_Store
            Thumbs.db
            
            # Logs
            logs/
            *.log
            
            # Environment
            .env
            .env.local
            
            # Build
            build/
            dist/
            """;
        
        Files.writeString(basePath.resolve(".gitignore"), gitignore);
        log.info("Fichier .gitignore créé");
    }
    
    private void generateReadme(Path basePath) throws IOException {
        String readme = """
            # NexusAI - Conversation Module
            
            Module de gestion des conversations avec les compagnons IA.
            
            ## Structure
            
            ```
            conversation-module/
            ├── conversation-common/       DTOs & Models partagés
            ├── conversation-api/          REST & WebSocket
            ├── conversation-core/         Business Logic
            ├── conversation-llm/          Intégration LLM
            ├── conversation-memory/       Système de mémoire
            └── conversation-persistence/  Accès données
            ```
            
            ## Prérequis
            
            - Java 21
            - Maven 3.9+
            - Docker & Docker Compose
            - MongoDB 7+
            - Redis 7+
            - Kafka 3.6+
            
            ## Installation
            
            ```bash
            # Démarrer l'infrastructure
            docker-compose up -d
            
            # Build le projet
            mvn clean install
            
            # Lancer l'application
            mvn spring-boot:run -pl conversation-api
            ```
            
            ## Documentation
            
            - API: http://localhost:8080/swagger-ui.html
            - Actuator: http://localhost:8080/actuator
            - Grafana: http://localhost:3000
            
            ## Tests
            
            ```bash
            # Tests unitaires
            mvn test
            
            # Tests d'intégration
            mvn verify -Pintegration-tests
            
            # Couverture
            mvn jacoco:report
            ```
            
            ## Support
            
            - Documentation: https://docs.nexusai.com
            - Slack: #conversation-module
            - Email: support@nexusai.com
            """;
        
        Files.writeString(basePath.resolve("README.md"), readme);
        log.info("Fichier README.md créé");
    }
    
    private void generateScripts(Path basePath) throws IOException {
        Path scriptsDir = basePath.resolve("scripts");
        Files.createDirectories(scriptsDir);
        
        // Script de démarrage
        String startScript = """
            #!/bin/bash
            
            echo "🚀 Démarrage NexusAI Conversation Module"
            
            # Vérifier Docker
            if ! command -v docker &> /dev/null; then
                echo "❌ Docker n'est pas installé"
                exit 1
            fi
            
            # Démarrer infrastructure
            echo "📦 Démarrage infrastructure..."
            docker-compose up -d
            
            # Attendre que les services soient prêts
            echo "⏳ Attente des services..."
            sleep 10
            
            # Build application
            echo "🔨 Build de l'application..."
            mvn clean package -DskipTests
            
            # Lancer application
            echo "✅ Démarrage de l'application..."
            java -jar conversation-api/target/conversation-api-1.0.0.jar
            """;
        
        Path startPath = scriptsDir.resolve("start.sh");
        Files.writeString(startPath, startScript);
        startPath.toFile().setExecutable(true);
        
        log.info("Scripts créés dans: {}", scriptsDir);
    }
    
    /**
     * Génère un rapport de la structure créée
     */
    public void generateReport() throws IOException {
        Path reportPath = Paths.get(outputPath, "GENERATION_REPORT.md");
        
        StringBuilder report = new StringBuilder();
        report.append("# Rapport de Génération du Projet\n\n");
        report.append("## Statistiques\n\n");
        report.append(String.format("- **Fichiers générés**: %d\n", fileContents.size()));
        report.append(String.format("- **Répertoires créés**: %d\n", directories.size()));
        report.append(String.format("- **Chemin de sortie**: %s\n\n", outputPath));
        
        report.append("## Fichiers Générés\n\n");
        
        // Grouper par module
        Map<String, List<String>> filesByModule = new HashMap<>();
        for (String filePath : fileContents.keySet()) {
            String module = filePath.split("/")[0];
            filesByModule.computeIfAbsent(module, k -> new ArrayList<>())
                        .add(filePath);
        }
        
        for (Map.Entry<String, List<String>> entry : filesByModule.entrySet()) {
            report.append(String.format("### %s (%d fichiers)\n\n", 
                                       entry.getKey(), entry.getValue().size()));
            
            for (String file : entry.getValue()) {
                report.append(String.format("- `%s`\n", file));
            }
            report.append("\n");
        }
        
        Files.writeString(reportPath, report.toString());
        log.info("Rapport généré: {}", reportPath);
    }
    
    /**
     * Point d'entrée principal
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: ProjectFileGenerator <document-path> <output-path>");
            System.exit(1);
        }
        
        String documentPath = args[0];
        String outputPath = args[1];
        
        try {
            ProjectFileGenerator generator = new ProjectFileGenerator(outputPath);
            
            // Parser et générer les fichiers
            generator.parseAndGenerate(documentPath);
            
            // Générer la structure Maven
            generator.generateMavenStructure();
            
            // Générer les fichiers de configuration
            generator.generateConfigFiles();
            
            // Générer le rapport
            generator.generateReport();
            
            System.out.println("✅ Génération terminée avec succès !");
            System.out.println("📁 Projet créé dans: " + outputPath);
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la génération: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

/**
 * Classe utilitaire pour les opérations sur les fichiers
 */
class FileUtils {
    
    /**
     * Crée une arborescence de répertoires
     */
    public static void createDirectoryTree(Path basePath, String... paths) 
            throws IOException {
        for (String path : paths) {
            Files.createDirectories(basePath.resolve(path));
        }
    }
    
    /**
     * Copie un fichier avec remplacement
     */
    public static void copyFileWithReplace(
            Path source, 
            Path target,
            Map<String, String> replacements) throws IOException {
        
        String content = Files.readString(source);
        
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            content = content.replace(entry.getKey(), entry.getValue());
        }
        
        Files.writeString(target, content);
    }
    
    /**
     * Liste récursivement tous les fichiers
     */
    public static List<Path> listAllFiles(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        
        Files.walk(directory)
            .filter(Files::isRegularFile)
            .forEach(files::add);
        
        return files;
    }
}

/**
 * Builder pour faciliter la création du générateur
 */
class ProjectGeneratorBuilder {
    
    private String outputPath;
    private boolean generateTests = true;
    private boolean generateDocs = true;
    private boolean generateScripts = true;
    
    public ProjectGeneratorBuilder outputPath(String path) {
        this.outputPath = path;
        return this;
    }
    
    public ProjectGeneratorBuilder withTests(boolean generateTests) {
        this.generateTests = generateTests;
        return this;
    }
    
    public ProjectGeneratorBuilder withDocs(boolean generateDocs) {
        this.generateDocs = generateDocs;
        return this;
    }
    
    public ProjectGeneratorBuilder withScripts(boolean generateScripts) {
        this.generateScripts = generateScripts;
        return this;
    }
    
    public ProjectFileGenerator build() {
        if (outputPath == null) {
            throw new IllegalStateException("Output path must be set");
        }
        
        ProjectFileGenerator generator = new ProjectFileGenerator(outputPath);
        // Appliquer les configurations
        return generator;
    }
}
