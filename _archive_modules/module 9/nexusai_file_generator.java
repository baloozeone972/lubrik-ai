package com.nexusai.tools;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * Générateur automatique d'arborescence de projet à partir de documentation.
 * 
 * Cette classe permet de :
 * - Parser des fichiers contenant du code (Java, XML, YAML, SQL, etc.)
 * - Extraire automatiquement les fichiers avec leur contenu
 * - Créer l'arborescence complète du projet
 * - Écrire les fichiers dans les bons répertoires
 * 
 * Usage:
 * <pre>
 * ProjectFileGenerator generator = new ProjectFileGenerator("./nexus-moderation");
 * generator.parseAndGenerate("./documentation/module9-code.md");
 * </pre>
 * 
 * @author NexusAI Tools Team
 * @version 1.0.0
 */
public class ProjectFileGenerator {
    
    private final Path outputBasePath;
    private final Map<String, FileType> fileTypeMap;
    private final List<ParsedFile> parsedFiles;
    private final boolean verbose;
    
    /**
     * Constructeur avec chemin de sortie.
     * 
     * @param outputPath Chemin racine où créer l'arborescence
     */
    public ProjectFileGenerator(String outputPath) {
        this(outputPath, true);
    }
    
    /**
     * Constructeur avec chemin de sortie et mode verbose.
     * 
     * @param outputPath Chemin racine où créer l'arborescence
     * @param verbose Afficher les logs détaillés
     */
    public ProjectFileGenerator(String outputPath, boolean verbose) {
        this.outputBasePath = Paths.get(outputPath).toAbsolutePath();
        this.fileTypeMap = initializeFileTypeMap();
        this.parsedFiles = new ArrayList<>();
        this.verbose = verbose;
    }
    
    /**
     * Parse un fichier de documentation et génère l'arborescence.
     * 
     * @param documentationPath Chemin vers le fichier de documentation
     * @throws IOException En cas d'erreur de lecture/écriture
     */
    public void parseAndGenerate(String documentationPath) throws IOException {
        log("📚 Parsing documentation: " + documentationPath);
        
        String content = Files.readString(Path.of(documentationPath), StandardCharsets.UTF_8);
        
        // Parser le contenu
        parseContent(content);
        
        log("✅ Found " + parsedFiles.size() + " files");
        
        // Générer l'arborescence
        generateFileStructure();
        
        log("🎉 Project structure generated successfully at: " + outputBasePath);
    }
    
    /**
     * Parse du contenu brut (de la documentation).
     * 
     * @param content Contenu à parser
     */
    public void parseContent(String content) {
        parsedFiles.clear();
        
        // Pattern pour détecter les blocs de code avec nom de fichier
        // Formats supportés:
        // - // FICHIER: path/to/File.java
        // - # path/to/file.sql
        // - <!-- path/to/file.xml -->
        
        Pattern fileHeaderPattern = Pattern.compile(
            "(?://|#|<!--|/\\*)\\s*(?:FICHIER|FILE|FICHIER \\d+)?\\s*:?\\s*([\\w/.\\-]+\\.[a-z]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        // Pattern pour blocs de code
        Pattern codeBlockPattern = Pattern.compile(
            "```(\\w+)?\\s*\\n(.*?)\\n```",
            Pattern.DOTALL
        );
        
        String[] lines = content.split("\n");
        String currentFile = null;
        StringBuilder currentContent = new StringBuilder();
        boolean inCodeBlock = false;
        String currentLanguage = null;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // Détecter début de bloc de code
            if (line.startsWith("```")) {
                if (!inCodeBlock) {
                    // Début de bloc
                    inCodeBlock = true;
                    currentLanguage = line.substring(3).trim();
                    currentContent = new StringBuilder();
                } else {
                    // Fin de bloc
                    inCodeBlock = false;
                    
                    if (currentFile != null) {
                        // Sauvegarder le fichier
                        addParsedFile(currentFile, currentContent.toString(), currentLanguage);
                        currentFile = null;
                        currentContent = new StringBuilder();
                    }
                }
                continue;
            }
            
            // Détecter header de fichier
            Matcher headerMatcher = fileHeaderPattern.matcher(line);
            if (headerMatcher.find()) {
                String filePath = headerMatcher.group(1);
                
                // Si on était dans un fichier précédent, le sauvegarder
                if (currentFile != null && currentContent.length() > 0) {
                    addParsedFile(currentFile, currentContent.toString(), currentLanguage);
                }
                
                currentFile = filePath;
                currentContent = new StringBuilder();
                log("📄 Found file: " + filePath);
                continue;
            }
            
            // Accumuler le contenu si on est dans un bloc de code
            if (inCodeBlock && currentFile != null) {
                currentContent.append(line).append("\n");
            }
        }
        
        // Sauvegarder le dernier fichier si nécessaire
        if (currentFile != null && currentContent.length() > 0) {
            addParsedFile(currentFile, currentContent.toString(), currentLanguage);
        }
        
        // Parser aussi avec la méthode alternative (regex globale)
        parseWithGlobalRegex(content);
    }
    
    /**
     * Méthode alternative de parsing avec regex globale.
     */
    private void parseWithGlobalRegex(String content) {
        // Pattern pour fichiers Java avec séparateurs "===="
        Pattern javaFilePattern = Pattern.compile(
            "// =+\\s*\\n// FICHIER \\d+: ([\\w/.]+\\.java)\\s*\\n// =+\\s*\\n(.*?)(?=// =+|$)",
            Pattern.DOTALL
        );
        
        Matcher matcher = javaFilePattern.matcher(content);
        while (matcher.find()) {
            String filePath = matcher.group(1);
            String fileContent = matcher.group(2).trim();
            
            // Éviter les doublons
            if (parsedFiles.stream().noneMatch(f -> f.path.equals(filePath))) {
                addParsedFile(filePath, fileContent, "java");
            }
        }
        
        // Pattern pour fichiers SQL
        Pattern sqlFilePattern = Pattern.compile(
            "-- =+\\s*\\n-- ([\\w/_]+\\.sql)\\s*\\n-- =+\\s*\\n(.*?)(?=-- =+|$)",
            Pattern.DOTALL
        );
        
        matcher = sqlFilePattern.matcher(content);
        while (matcher.find()) {
            String filePath = matcher.group(1);
            String fileContent = matcher.group(2).trim();
            
            if (parsedFiles.stream().noneMatch(f -> f.path.equals(filePath))) {
                addParsedFile(filePath, fileContent, "sql");
            }
        }
    }
    
    /**
     * Ajoute un fichier parsé à la liste.
     */
    private void addParsedFile(String path, String content, String language) {
        FileType type = detectFileType(path, language);
        String normalizedPath = normalizePath(path, type);
        
        ParsedFile file = new ParsedFile(normalizedPath, content, type);
        parsedFiles.add(file);
        
        if (verbose) {
            log("  ✓ Added: " + normalizedPath + " (" + type + ", " + content.length() + " chars)");
        }
    }
    
    /**
     * Normalise le chemin d'un fichier selon son type.
     */
    private String normalizePath(String path, FileType type) {
        // Retirer les préfixes de package si présents
        path = path.replace("package com.nexusai.moderation.", "");
        
        // Construire le chemin complet
        switch (type) {
            case JAVA:
                if (!path.startsWith("src/")) {
                    // Extraire le package depuis le contenu si possible
                    return "src/main/java/com/nexusai/moderation/" + path;
                }
                break;
                
            case TEST_JAVA:
                if (!path.startsWith("src/")) {
                    return "src/test/java/com/nexusai/moderation/" + path;
                }
                break;
                
            case SQL:
                if (!path.startsWith("src/")) {
                    return "src/main/resources/db/migration/" + path;
                }
                break;
                
            case YAML:
                if (!path.startsWith("src/") && !path.equals("docker-compose.yml")) {
                    return "src/main/resources/" + path;
                }
                break;
                
            case PROPERTIES:
                if (!path.startsWith("src/")) {
                    return "src/main/resources/" + path;
                }
                break;
                
            default:
                break;
        }
        
        return path;
    }
    
    /**
     * Détecte le type de fichier.
     */
    private FileType detectFileType(String path, String language) {
        String extension = getFileExtension(path);
        
        // Vérifier si c'est un test
        if (path.contains("Test.java") || path.contains("src/test/")) {
            return FileType.TEST_JAVA;
        }
        
        // Détecter par extension
        if (fileTypeMap.containsKey(extension)) {
            return fileTypeMap.get(extension);
        }
        
        // Détecter par language
        if (language != null) {
            switch (language.toLowerCase()) {
                case "java": return FileType.JAVA;
                case "xml": return FileType.XML;
                case "yaml", "yml": return FileType.YAML;
                case "sql": return FileType.SQL;
                case "properties": return FileType.PROPERTIES;
                case "dockerfile": return FileType.DOCKERFILE;
                case "markdown", "md": return FileType.MARKDOWN;
                default: return FileType.OTHER;
            }
        }
        
        return FileType.OTHER;
    }
    
    /**
     * Génère la structure de fichiers sur le disque.
     */
    private void generateFileStructure() throws IOException {
        log("\n📁 Generating file structure...\n");
        
        // Créer la structure de base
        createBaseStructure();
        
        // Écrire chaque fichier
        for (ParsedFile file : parsedFiles) {
            writeFile(file);
        }
        
        // Créer les répertoires vides nécessaires
        createEmptyDirectories();
        
        log("\n✅ Generated " + parsedFiles.size() + " files");
    }
    
    /**
     * Crée la structure de répertoires de base.
     */
    private void createBaseStructure() throws IOException {
        String[] baseDirs = {
            "src/main/java/com/nexusai/moderation",
            "src/main/resources/db/migration",
            "src/main/resources/blacklists",
            "src/main/resources/templates/email",
            "src/test/java/com/nexusai/moderation",
            "src/test/resources",
            "docs",
            "monitoring",
            "k8s/production",
            "k8s/staging"
        };
        
        for (String dir : baseDirs) {
            Path dirPath = outputBasePath.resolve(dir);
            Files.createDirectories(dirPath);
        }
    }
    
    /**
     * Crée des répertoires vides nécessaires.
     */
    private void createEmptyDirectories() throws IOException {
        String[] emptyDirs = {
            "logs",
            "target"
        };
        
        for (String dir : emptyDirs) {
            Path dirPath = outputBasePath.resolve(dir);
            Files.createDirectories(dirPath);
            
            // Créer un .gitkeep pour conserver le répertoire dans Git
            Path gitkeep = dirPath.resolve(".gitkeep");
            if (!Files.exists(gitkeep)) {
                Files.writeString(gitkeep, "");
            }
        }
    }
    
    /**
     * Écrit un fichier sur le disque.
     */
    private void writeFile(ParsedFile file) throws IOException {
        Path filePath = outputBasePath.resolve(file.path);
        
        // Créer les répertoires parents si nécessaire
        Files.createDirectories(filePath.getParent());
        
        // Nettoyer le contenu
        String content = cleanContent(file.content, file.type);
        
        // Écrire le fichier
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        
        log("  ✓ Written: " + file.path);
    }
    
    /**
     * Nettoie le contenu avant écriture.
     */
    private String cleanContent(String content, FileType type) {
        // Supprimer les lignes de séparation "===="
        content = content.replaceAll("// =+\\s*\\n", "");
        content = content.replaceAll("-- =+\\s*\\n", "");
        content = content.replaceAll("# =+\\s*\\n", "");
        
        // Supprimer les commentaires de header de fichier redondants
        content = content.replaceAll("(?m)^// FICHIER \\d+:.*$\\n", "");
        content = content.replaceAll("(?m)^-- V\\d+_.*\\.sql\\s*$\\n", "");
        
        // Trim et assurer une nouvelle ligne à la fin
        content = content.trim();
        if (!content.endsWith("\n")) {
            content += "\n";
        }
        
        return content;
    }
    
    /**
     * Génère un rapport de ce qui a été créé.
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("📊 PROJECT GENERATION REPORT\n");
        report.append("═".repeat(50)).append("\n\n");
        report.append("Output path: ").append(outputBasePath).append("\n");
        report.append("Total files: ").append(parsedFiles.size()).append("\n\n");
        
        // Grouper par type
        Map<FileType, List<ParsedFile>> byType = parsedFiles.stream()
            .collect(Collectors.groupingBy(f -> f.type));
        
        report.append("Files by type:\n");
        for (FileType type : FileType.values()) {
            int count = byType.getOrDefault(type, Collections.emptyList()).size();
            if (count > 0) {
                report.append(String.format("  - %-15s: %3d files\n", type, count));
            }
        }
        
        report.append("\n");
        report.append("File tree:\n");
        report.append(generateFileTree());
        
        return report.toString();
    }
    
    /**
     * Génère un arbre de fichiers (ASCII art).
     */
    private String generateFileTree() {
        StringBuilder tree = new StringBuilder();
        
        // Trier les fichiers par chemin
        List<String> sortedPaths = parsedFiles.stream()
            .map(f -> f.path)
            .sorted()
            .collect(Collectors.toList());
        
        Map<String, List<String>> dirTree = new TreeMap<>();
        
        for (String path : sortedPaths) {
            Path p = Paths.get(path);
            String dir = p.getParent() != null ? p.getParent().toString() : "";
            String file = p.getFileName().toString();
            
            dirTree.computeIfAbsent(dir, k -> new ArrayList<>()).add(file);
        }
        
        // Construire l'arbre
        for (Map.Entry<String, List<String>> entry : dirTree.entrySet()) {
            tree.append("📁 ").append(entry.getKey().isEmpty() ? "." : entry.getKey()).append("/\n");
            
            List<String> files = entry.getValue();
            for (int i = 0; i < files.size(); i++) {
                boolean isLast = i == files.size() - 1;
                tree.append(isLast ? "  └── " : "  ├── ").append(files.get(i)).append("\n");
            }
        }
        
        return tree.toString();
    }
    
    /**
     * Sauvegarde le rapport dans un fichier.
     */
    public void saveReport(String reportPath) throws IOException {
        String report = generateReport();
        Files.writeString(Path.of(reportPath), report, StandardCharsets.UTF_8);
        log("📄 Report saved to: " + reportPath);
    }
    
    // =========================================================================
    // HELPER METHODS
    // =========================================================================
    
    private void log(String message) {
        if (verbose) {
            System.out.println(message);
        }
    }
    
    private String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(lastDot + 1).toLowerCase() : "";
    }
    
    private Map<String, FileType> initializeFileTypeMap() {
        Map<String, FileType> map = new HashMap<>();
        map.put("java", FileType.JAVA);
        map.put("xml", FileType.XML);
        map.put("yml", FileType.YAML);
        map.put("yaml", FileType.YAML);
        map.put("sql", FileType.SQL);
        map.put("properties", FileType.PROPERTIES);
        map.put("md", FileType.MARKDOWN);
        map.put("txt", FileType.TEXT);
        return map;
    }
    
    // =========================================================================
    // INNER CLASSES
    // =========================================================================
    
    /**
     * Représente un fichier parsé.
     */
    private static class ParsedFile {
        final String path;
        final String content;
        final FileType type;
        
        ParsedFile(String path, String content, FileType type) {
            this.path = path;
            this.content = content;
            this.type = type;
        }
    }
    
    /**
     * Types de fichiers supportés.
     */
    private enum FileType {
        JAVA,
        TEST_JAVA,
        XML,
        YAML,
        SQL,
        PROPERTIES,
        DOCKERFILE,
        MARKDOWN,
        TEXT,
        OTHER
    }
    
    // =========================================================================
    // MAIN - Exemple d'utilisation
    // =========================================================================
    
    public static void main(String[] args) {
        try {
            // Chemin de sortie
            String outputPath = args.length > 0 ? args[0] : "./nexus-moderation";
            
            // Créer le générateur
            ProjectFileGenerator generator = new ProjectFileGenerator(outputPath, true);
            
            System.out.println("🚀 NexusAI Project File Generator");
            System.out.println("═".repeat(50));
            System.out.println();
            
            // Option 1: Parser depuis un fichier de documentation
            if (args.length > 1) {
                String docPath = args[1];
                generator.parseAndGenerate(docPath);
            } 
            // Option 2: Parser depuis une chaîne de caractères (exemple)
            else {
                System.out.println("ℹ️  Usage: java ProjectFileGenerator <output-path> [documentation-file]");
                System.out.println("ℹ️  Example: java ProjectFileGenerator ./nexus-moderation docs/module9.md");
                System.out.println();
                
                // Exemple avec du contenu inline
                String exampleContent = """
                    // =============================================================================
                    // FICHIER 1: Example.java
                    // =============================================================================
                    package com.nexusai.moderation.example;
                    
                    public class Example {
                        public static void main(String[] args) {
                            System.out.println("Hello NexusAI!");
                        }
                    }
                    
                    -- =============================================================================
                    -- V1__create_example_table.sql
                    -- =============================================================================
                    CREATE TABLE example (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(255)
                    );
                    """;
                
                System.out.println("📝 Parsing example content...\n");
                generator.parseContent(exampleContent);
                generator.generateFileStructure();
            }
            
            // Générer et afficher le rapport
            System.out.println("\n" + generator.generateReport());
            
            // Sauvegarder le rapport
            String reportPath = outputPath + "/GENERATION_REPORT.txt";
            generator.saveReport(reportPath);
            
            System.out.println("\n✨ Done! Check the output at: " + outputPath);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
