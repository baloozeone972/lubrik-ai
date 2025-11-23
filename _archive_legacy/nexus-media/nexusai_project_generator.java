package com.nexusai.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Générateur de projet NexusAI Audio Module.
 * 
 * <p>Cette classe parse un fichier de documentation contenant tous les
 * fichiers du projet (Java, XML, YAML, SQL, etc.) et génère l'arborescence
 * complète du projet.</p>
 * 
 * <p><strong>Usage :</strong></p>
 * <pre>{@code
 * ProjectGenerator generator = new ProjectGenerator(
 *     "documentation.md",
 *     "/path/to/output"
 * );
 * generator.generate();
 * }</pre>
 * 
 * <p><strong>Format attendu dans le fichier :</strong></p>
 * <pre>
 * // Fichier Java
 * // path/to/MyClass.java
 * package com.example;
 * public class MyClass { }
 * 
 * # Fichier YAML
 * # config/application.yml
 * spring:
 *   application:
 *     name: my-app
 * 
 * &lt;!-- Fichier XML --&gt;
 * &lt;!-- pom.xml --&gt;
 * &lt;project&gt;...&lt;/project&gt;
 * </pre>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-10-20
 */
public class ProjectGenerator {
    
    private final String documentationPath;
    private final String outputBasePath;
    private final Map<String, FileInfo> filesToGenerate;
    
    // Patterns pour détecter les différents types de fichiers
    private static final Pattern JAVA_FILE_PATTERN = 
        Pattern.compile("^//\\s*(.+\\.java)\\s*$", Pattern.MULTILINE);
    
    private static final Pattern YAML_FILE_PATTERN = 
        Pattern.compile("^#\\s*(.+\\.(yml|yaml))\\s*$", Pattern.MULTILINE);
    
    private static final Pattern XML_FILE_PATTERN = 
        Pattern.compile("^<!--\\s*(.+\\.xml)\\s*-->\\s*$", Pattern.MULTILINE);
    
    private static final Pattern SQL_FILE_PATTERN = 
        Pattern.compile("^--\\s*(.+\\.sql)\\s*$", Pattern.MULTILINE);
    
    private static final Pattern PROPERTIES_FILE_PATTERN = 
        Pattern.compile("^#\\s*(.+\\.properties)\\s*$", Pattern.MULTILINE);
    
    private static final Pattern MAKEFILE_PATTERN = 
        Pattern.compile("^#\\s*(Makefile)\\s*$", Pattern.MULTILINE);
    
    private static final Pattern DOCKERFILE_PATTERN = 
        Pattern.compile("^#\\s*(Dockerfile)\\s*$", Pattern.MULTILINE);
    
    private static final Pattern MARKDOWN_PATTERN = 
        Pattern.compile("^#\\s*(.+\\.md)\\s*$", Pattern.MULTILINE);
    
    /**
     * Classe interne représentant un fichier à générer.
     */
    private static class FileInfo {
        String relativePath;
        String content;
        FileType type;
        
        FileInfo(String relativePath, String content, FileType type) {
            this.relativePath = relativePath;
            this.content = content;
            this.type = type;
        }
    }
    
    /**
     * Énumération des types de fichiers supportés.
     */
    private enum FileType {
        JAVA,
        XML,
        YAML,
        SQL,
        PROPERTIES,
        MAKEFILE,
        DOCKERFILE,
        MARKDOWN,
        SHELL,
        UNKNOWN
    }
    
    /**
     * Constructeur.
     * 
     * @param documentationPath Chemin du fichier de documentation source
     * @param outputBasePath Chemin de base où générer le projet
     */
    public ProjectGenerator(String documentationPath, String outputBasePath) {
        this.documentationPath = documentationPath;
        this.outputBasePath = outputBasePath;
        this.filesToGenerate = new LinkedHashMap<>();
    }
    
    /**
     * Génère le projet complet.
     * 
     * @throws IOException Si une erreur d'I/O survient
     */
    public void generate() throws IOException {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║   NexusAI Audio Module - Générateur de Projet        ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // 1. Lire le fichier de documentation
        System.out.println("📖 Lecture du fichier de documentation...");
        String documentation = readFile(documentationPath);
        
        // 2. Parser et extraire tous les fichiers
        System.out.println("🔍 Extraction des fichiers...");
        parseDocumentation(documentation);
        
        System.out.println("   ✓ " + filesToGenerate.size() + " fichiers détectés");
        System.out.println();
        
        // 3. Créer l'arborescence et écrire les fichiers
        System.out.println("📁 Génération de l'arborescence...");
        createDirectoryStructure();
        
        System.out.println("📝 Écriture des fichiers...");
        writeAllFiles();
        
        // 4. Afficher le résumé
        printSummary();
    }
    
    /**
     * Lit le contenu d'un fichier.
     * 
     * @param filePath Chemin du fichier
     * @return Contenu du fichier
     * @throws IOException Si le fichier ne peut pas être lu
     */
    private String readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readString(path);
    }
    
    /**
     * Parse la documentation et extrait tous les fichiers.
     * 
     * @param documentation Contenu de la documentation
     */
    private void parseDocumentation(String documentation) {
        // Diviser le document en blocs de code
        String[] blocks = documentation.split("```");
        
        for (int i = 0; i < blocks.length; i++) {
            if (i % 2 == 0) continue; // Ignorer les blocs non-code
            
            String block = blocks[i];
            
            // Détecter le langage du bloc
            String[] lines = block.split("\n", 2);
            if (lines.length < 2) continue;
            
            String language = lines[0].trim().toLowerCase();
            String content = lines.length > 1 ? lines[1] : "";
            
            // Parser selon le type
            switch (language) {
                case "java":
                    parseJavaBlock(content);
                    break;
                case "xml":
                    parseXmlBlock(content);
                    break;
                case "yaml", "yml":
                    parseYamlBlock(content);
                    break;
                case "sql":
                    parseSqlBlock(content);
                    break;
                case "properties":
                    parsePropertiesBlock(content);
                    break;
                case "bash", "sh", "shell":
                    parseShellBlock(content);
                    break;
                case "markdown", "md":
                    parseMarkdownBlock(content);
                    break;
                default:
                    // Essayer de détecter automatiquement
                    autoDetectAndParse(content);
            }
        }
        
        // Parser aussi les fichiers en dehors des blocs de code
        parseInlineFiles(documentation);
    }
    
    /**
     * Parse un bloc de code Java.
     */
    private void parseJavaBlock(String content) {
        // Détecter le chemin du fichier dans les commentaires
        Matcher matcher = JAVA_FILE_PATTERN.matcher(content);
        
        if (matcher.find()) {
            String filePath = matcher.group(1).trim();
            String fileContent = content.substring(matcher.end()).trim();
            
            filesToGenerate.put(filePath, 
                new FileInfo(filePath, fileContent, FileType.JAVA));
        } else {
            // Essayer d'extraire le package pour deviner le chemin
            extractJavaFileFromPackage(content);
        }
    }
    
    /**
     * Extrait un fichier Java en se basant sur le package.
     */
    private void extractJavaFileFromPackage(String content) {
        Pattern packagePattern = Pattern.compile("package\\s+([a-z0-9.]+);");
        Pattern classPattern = Pattern.compile("(public\\s+)?(class|interface|enum)\\s+(\\w+)");
        
        Matcher packageMatcher = packagePattern.matcher(content);
        Matcher classMatcher = classPattern.matcher(content);
        
        if (packageMatcher.find() && classMatcher.find()) {
            String packageName = packageMatcher.group(1);
            String className = classMatcher.group(3);
            
            // Déterminer le module
            String module = guessModuleFromPackage(packageName);
            
            // Construire le chemin
            String packagePath = packageName.replace('.', '/');
            String filePath = module + "/src/main/java/" + packagePath + "/" + className + ".java";
            
            filesToGenerate.put(filePath, 
                new FileInfo(filePath, content.trim(), FileType.JAVA));
        }
    }
    
    /**
     * Devine le module à partir du package.
     */
    private String guessModuleFromPackage(String packageName) {
        if (packageName.contains(".api")) return "nexus-audio-api";
        if (packageName.contains(".core")) return "nexus-audio-core";
        if (packageName.contains(".stt")) return "nexus-audio-stt";
        if (packageName.contains(".tts")) return "nexus-audio-tts";
        if (packageName.contains(".webrtc")) return "nexus-audio-webrtc";
        if (packageName.contains(".storage")) return "nexus-audio-storage";
        if (packageName.contains(".emotion")) return "nexus-audio-emotion";
        if (packageName.contains(".persistence")) return "nexus-audio-persistence";
        if (packageName.contains(".tools")) return "tools";
        return "nexus-audio-api";
    }
    
    /**
     * Parse un bloc XML.
     */
    private void parseXmlBlock(String content) {
        Matcher matcher = XML_FILE_PATTERN.matcher(content);
        
        if (matcher.find()) {
            String filePath = matcher.group(1).trim();
            String fileContent = content.substring(matcher.end()).trim();
            
            filesToGenerate.put(filePath, 
                new FileInfo(filePath, fileContent, FileType.XML));
        } else {
            // Si c'est un POM, deviner le chemin
            if (content.contains("<artifactId>")) {
                extractPomFile(content);
            }
        }
    }
    
    /**
     * Extrait un fichier POM.xml.
     */
    private void extractPomFile(String content) {
        Pattern artifactPattern = Pattern.compile("<artifactId>([^<]+)</artifactId>");
        Matcher matcher = artifactPattern.matcher(content);
        
        if (matcher.find()) {
            String artifactId = matcher.group(1);
            
            String filePath;
            if (artifactId.equals("nexus-audio")) {
                filePath = "pom.xml"; // Parent POM
            } else {
                filePath = artifactId + "/pom.xml";
            }
            
            filesToGenerate.put(filePath, 
                new FileInfo(filePath, content.trim(), FileType.XML));
        }
    }
    
    /**
     * Parse un bloc YAML.
     */
    private void parseYamlBlock(String content) {
        Matcher matcher = YAML_FILE_PATTERN.matcher(content);
        
        if (matcher.find()) {
            String filePath = matcher.group(1).trim();
            String fileContent = content.substring(matcher.end()).trim();
            
            filesToGenerate.put(filePath, 
                new FileInfo(filePath, fileContent, FileType.YAML));
        } else {
            // Par défaut, application.yml
            if (content.contains("spring:")) {
                String filePath = "nexus-audio-api/src/main/resources/application.yml";
                filesToGenerate.put(filePath, 
                    new FileInfo(filePath, content.trim(), FileType.YAML));
            } else if (content.contains("services:")) {
                // Docker Compose
                filesToGenerate.put("docker-compose.yml", 
                    new FileInfo("docker-compose.yml", content.trim(), FileType.YAML));
            }
        }
    }
    
    /**
     * Parse un bloc SQL.
     */
    private void parseSqlBlock(String content) {
        Matcher matcher = SQL_FILE_PATTERN.matcher(content);
        
        if (matcher.find()) {
            String filePath = matcher.group(1).trim();
            String fileContent = content.substring(matcher.end()).trim();
            
            filesToGenerate.put(filePath, 
                new FileInfo(filePath, fileContent, FileType.SQL));
        } else {
            // Migration Flyway par défaut
            if (content.contains("CREATE TABLE")) {
                String filePath = "nexus-audio-persistence/src/main/resources/db/migration/V1__create_voice_tables.sql";
                filesToGenerate.put(filePath, 
                    new FileInfo(filePath, content.trim(), FileType.SQL));
            }
        }
    }
    
    /**
     * Parse un bloc Properties.
     */
    private void parsePropertiesBlock(String content) {
        Matcher matcher = PROPERTIES_FILE_PATTERN.matcher(content);
        
        if (matcher.find()) {
            String filePath = matcher.group(1).trim();
            String fileContent = content.substring(matcher.end()).trim();
            
            filesToGenerate.put(filePath, 
                new FileInfo(filePath, fileContent, FileType.PROPERTIES));
        }
    }
    
    /**
     * Parse un bloc Shell.
     */
    private void parseShellBlock(String content) {
        // Détecter les scripts shell
        if (content.startsWith("#!")) {
            // C'est un script
            String filePath = "scripts/setup.sh";
            filesToGenerate.put(filePath, 
                new FileInfo(filePath, content.trim(), FileType.SHELL));
        }
    }
    
    /**
     * Parse un bloc Markdown.
     */
    private void parseMarkdownBlock(String content) {
        Matcher matcher = MARKDOWN_PATTERN.matcher(content);
        
        if (matcher.find()) {
            String filePath = matcher.group(1).trim();
            String fileContent = content.substring(matcher.end()).trim();
            
            filesToGenerate.put(filePath, 
                new FileInfo(filePath, fileContent, FileType.MARKDOWN));
        } else if (content.startsWith("# ")) {
            // README par défaut
            filesToGenerate.put("README.md", 
                new FileInfo("README.md", content.trim(), FileType.MARKDOWN));
        }
    }
    
    /**
     * Détection automatique du type de fichier.
     */
    private void autoDetectAndParse(String content) {
        // Makefile
        if (content.contains(".PHONY:") || content.contains("@echo")) {
            filesToGenerate.put("Makefile", 
                new FileInfo("Makefile", content.trim(), FileType.MAKEFILE));
        }
        // Dockerfile
        else if (content.startsWith("FROM ")) {
            filesToGenerate.put("Dockerfile", 
                new FileInfo("Dockerfile", content.trim(), FileType.DOCKERFILE));
        }
        // .env
        else if (content.contains("API_KEY=") || content.contains("OPENAI")) {
            filesToGenerate.put(".env.example", 
                new FileInfo(".env.example", content.trim(), FileType.PROPERTIES));
        }
    }
    
    /**
     * Parse les fichiers mentionnés inline dans la documentation.
     */
    private void parseInlineFiles(String documentation) {
        // Cette méthode peut être étendue pour parser d'autres formats
    }
    
    /**
     * Crée l'arborescence des dossiers.
     */
    private void createDirectoryStructure() throws IOException {
        Set<String> directories = new HashSet<>();
        
        for (FileInfo fileInfo : filesToGenerate.values()) {
            Path filePath = Paths.get(outputBasePath, fileInfo.relativePath);
            Path parentDir = filePath.getParent();
            
            if (parentDir != null) {
                directories.add(parentDir.toString());
            }
        }
        
        for (String dir : directories) {
            Files.createDirectories(Paths.get(dir));
        }
        
        System.out.println("   ✓ " + directories.size() + " dossiers créés");
    }
    
    /**
     * Écrit tous les fichiers extraits.
     */
    private void writeAllFiles() throws IOException {
        int written = 0;
        
        for (FileInfo fileInfo : filesToGenerate.values()) {
            writeFile(fileInfo);
            written++;
            
            if (written % 10 == 0) {
                System.out.println("   ✓ " + written + "/" + filesToGenerate.size() + " fichiers écrits...");
            }
        }
        
        System.out.println("   ✓ " + written + " fichiers écrits au total");
    }
    
    /**
     * Écrit un fichier sur le disque.
     */
    private void writeFile(FileInfo fileInfo) throws IOException {
        Path fullPath = Paths.get(outputBasePath, fileInfo.relativePath);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fullPath.toFile()))) {
            writer.write(fileInfo.content);
        }
    }
    
    /**
     * Affiche un résumé de la génération.
     */
    private void printSummary() {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("                    RÉSUMÉ");
        System.out.println("═══════════════════════════════════════════════════════");
        
        Map<FileType, Integer> countByType = new HashMap<>();
        for (FileInfo fileInfo : filesToGenerate.values()) {
            countByType.merge(fileInfo.type, 1, Integer::sum);
        }
        
        System.out.println();
        System.out.println("Fichiers générés par type :");
        countByType.forEach((type, count) -> 
            System.out.printf("  • %-15s : %3d fichiers%n", type.name(), count)
        );
        
        System.out.println();
        System.out.println("Projet généré dans : " + outputBasePath);
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("✅ Génération terminée avec succès !");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("Prochaines étapes :");
        System.out.println("  1. cd " + outputBasePath);
        System.out.println("  2. docker-compose up -d");
        System.out.println("  3. mvn clean install");
        System.out.println("  4. mvn spring-boot:run -pl nexus-audio-api");
    }
    
    /**
     * Point d'entrée principal.
     * 
     * @param args Arguments : [0] = fichier documentation, [1] = chemin sortie
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ProjectGenerator <documentation.md> <output-path>");
            System.err.println();
            System.err.println("Exemple:");
            System.err.println("  java ProjectGenerator docs/module-audio.md /home/user/nexus-audio");
            System.exit(1);
        }
        
        String docPath = args[0];
        String outputPath = args[1];
        
        try {
            ProjectGenerator generator = new ProjectGenerator(docPath, outputPath);
            generator.generate();
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de la génération du projet :");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
