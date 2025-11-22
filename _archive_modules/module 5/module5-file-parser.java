package com.nexusai.tools;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * NEXUSAI - MODULE 5 FILE PARSER & GENERATOR
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Cette classe parse les artifacts contenant du code multi-langage
 * et génère l'arborescence complète du projet.
 * 
 * Fonctionnalités:
 * - Parse les fichiers Java, XML, YAML, Python, SQL, Markdown, etc.
 * - Détecte automatiquement le type et le chemin du fichier
 * - Crée l'arborescence de dossiers
 * - Gère les encodages
 * - Génère des rapports de parsing
 * 
 * Utilisation:
 * <pre>
 * ModuleFileParser parser = new ModuleFileParser("./nexus-image-generation");
 * parser.parseArtifact("artifacts/nexus-image-gen-module.md");
 * parser.generateReport();
 * </pre>
 * 
 * @author NexusAI Team
 * @version 1.0.0
 * @since 2025-01-20
 */
public class ModuleFileParser {

    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════

    private final String outputBasePath;
    private final Map<String, Integer> fileTypeCounter = new HashMap<>();
    private final List<ParsedFile> parsedFiles = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    // Patterns de détection de fichiers
    private static final Map<String, Pattern> FILE_PATTERNS = Map.ofEntries(
        // Java files
        Map.entry("java", Pattern.compile("^package\\s+([a-z.]+);.*?(?:public|private)\\s+(?:class|interface|enum|record)\\s+(\\w+)", 
            Pattern.DOTALL | Pattern.MULTILINE)),
        
        // POM.xml
        Map.entry("pom", Pattern.compile("<artifactId>([^<]+)</artifactId>")),
        
        // YAML files
        Map.entry("yaml", Pattern.compile("^(?:spring:|server:|aws:|management:)", Pattern.MULTILINE)),
        
        // Python files
        Map.entry("python", Pattern.compile("^(?:import|from|def|class)\\s+", Pattern.MULTILINE)),
        
        // SQL files
        Map.entry("sql", Pattern.compile("^(?:CREATE|ALTER|INSERT|SELECT|UPDATE|DELETE)\\s+", 
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)),
        
        // Dockerfile
        Map.entry("dockerfile", Pattern.compile("^FROM\\s+", Pattern.MULTILINE)),
        
        // Docker Compose
        Map.entry("docker-compose", Pattern.compile("^version:\\s*['\"]?\\d", Pattern.MULTILINE)),
        
        // Properties
        Map.entry("properties", Pattern.compile("^[a-zA-Z0-9._-]+\\s*=", Pattern.MULTILINE)),
        
        // Markdown
        Map.entry("markdown", Pattern.compile("^#\\s+", Pattern.MULTILINE)),
        
        // JSON
        Map.entry("json", Pattern.compile("^\\s*[{\\[]", Pattern.MULTILINE))
    );

    /**
     * Classe représentant un fichier parsé
     */
    private static class ParsedFile {
        String relativePath;
        String content;
        String type;
        int lineCount;
        long size;

        ParsedFile(String relativePath, String content, String type) {
            this.relativePath = relativePath;
            this.content = content;
            this.type = type;
            this.lineCount = content.split("\n").length;
            this.size = content.getBytes(StandardCharsets.UTF_8).length;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTEUR
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Constructeur
     * 
     * @param outputBasePath Chemin de sortie pour l'arborescence (ex: "./nexus-image-generation")
     */
    public ModuleFileParser(String outputBasePath) {
        this.outputBasePath = outputBasePath;
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║   NEXUSAI - MODULE FILE PARSER & GENERATOR               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println("Output path: " + outputBasePath);
        System.out.println();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTHODES PRINCIPALES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Parse un fichier artifact contenant du code multi-langage
     * 
     * @param artifactPath Chemin vers le fichier artifact
     * @throws IOException Si erreur de lecture
     */
    public void parseArtifact(String artifactPath) throws IOException {
        System.out.println("📖 Parsing artifact: " + artifactPath);
        
        String content = Files.readString(Path.of(artifactPath), StandardCharsets.UTF_8);
        
        // Détecter les blocs de code
        List<CodeBlock> codeBlocks = extractCodeBlocks(content);
        
        System.out.println("✓ Found " + codeBlocks.size() + " code blocks");
        
        // Parser chaque bloc
        for (CodeBlock block : codeBlocks) {
            parseCodeBlock(block);
        }
    }

    /**
     * Parse du contenu brut (texte avec blocs de code)
     * 
     * @param rawContent Contenu brut à parser
     */
    public void parseRawContent(String rawContent) {
        System.out.println("📖 Parsing raw content");
        
        List<CodeBlock> codeBlocks = extractCodeBlocks(rawContent);
        System.out.println("✓ Found " + codeBlocks.size() + " code blocks");
        
        for (CodeBlock block : codeBlocks) {
            parseCodeBlock(block);
        }
    }

    /**
     * Extrait les blocs de code du contenu
     * 
     * @param content Contenu à parser
     * @return Liste des blocs de code
     */
    private List<CodeBlock> extractCodeBlocks(String content) {
        List<CodeBlock> blocks = new ArrayList<>();
        
        // Pattern pour détecter les blocs de code (Markdown, XML comments, etc.)
        Pattern codeBlockPattern = Pattern.compile(
            "(?:```(\\w+)?\\s*\\n([\\s\\S]*?)```)|" +  // Markdown code blocks
            "(?:<!--\\s*([\\s\\S]*?)-->)|" +           // XML/HTML comments
            "(?:\"{3}\\s*\\n([\\s\\S]*?)\"{3})",       // Python docstrings
            Pattern.MULTILINE
        );
        
        Matcher matcher = codeBlockPattern.matcher(content);
        
        while (matcher.find()) {
            String language = matcher.group(1);
            String code = matcher.group(2);
            
            if (code == null) {
                code = matcher.group(3);
                if (code == null) {
                    code = matcher.group(4);
                }
            }
            
            if (code != null && !code.trim().isEmpty()) {
                blocks.add(new CodeBlock(language, code.trim()));
            }
        }
        
        return blocks;
    }

    /**
     * Parse un bloc de code et détermine son type et chemin
     * 
     * @param block Bloc de code à parser
     */
    private void parseCodeBlock(CodeBlock block) {
        try {
            String code = block.content;
            
            // Détection du type de fichier
            FileInfo fileInfo = detectFileType(code, block.language);
            
            if (fileInfo != null) {
                // Créer le fichier
                createFile(fileInfo.path, code);
                
                // Enregistrer le fichier parsé
                ParsedFile parsedFile = new ParsedFile(fileInfo.path, code, fileInfo.type);
                parsedFiles.add(parsedFile);
                
                // Compteur
                fileTypeCounter.merge(fileInfo.type, 1, Integer::sum);
                
                System.out.println("  ✓ " + fileInfo.type.toUpperCase() + " → " + fileInfo.path);
            }
            
        } catch (Exception e) {
            String error = "Error parsing block: " + e.getMessage();
            errors.add(error);
            System.err.println("  ✗ " + error);
        }
    }

    /**
     * Détecte le type de fichier et son chemin
     * 
     * @param code Contenu du code
     * @param hintLanguage Langage suggéré (optionnel)
     * @return Informations sur le fichier
     */
    private FileInfo detectFileType(String code, String hintLanguage) {
        
        // 1. Détection par commentaire explicite de chemin
        Pattern pathCommentPattern = Pattern.compile(
            "//\\s*([\\w-]+/[\\w/-]+\\.\\w+)|" +  // Java style: // path/to/File.java
            "#\\s*([\\w-]+/[\\w/-]+\\.\\w+)|" +   // Python style: # path/to/file.py
            "<!--\\s*([\\w-]+/[\\w/-]+\\.\\w+)\\s*-->", // XML style: <!-- path/to/file.xml -->
            Pattern.MULTILINE
        );
        
        Matcher pathMatcher = pathCommentPattern.matcher(code);
        if (pathMatcher.find()) {
            for (int i = 1; i <= pathMatcher.groupCount(); i++) {
                String path = pathMatcher.group(i);
                if (path != null) {
                    String type = path.substring(path.lastIndexOf('.') + 1);
                    return new FileInfo(path, type);
                }
            }
        }
        
        // 2. Détection Java avec package
        if (hintLanguage == null || "java".equalsIgnoreCase(hintLanguage)) {
            FileInfo javaInfo = detectJavaFile(code);
            if (javaInfo != null) return javaInfo;
        }
        
        // 3. Détection POM.xml
        if (code.contains("<project") && code.contains("<artifactId>")) {
            return new FileInfo("pom.xml", "xml");
        }
        
        // 4. Détection application.yml/yaml
        if (code.contains("spring:") || code.contains("server:")) {
            return new FileInfo("src/main/resources/application.yml", "yaml");
        }
        
        // 5. Détection Python
        if ("python".equalsIgnoreCase(hintLanguage) || code.matches("(?s).*\\b(?:import|from|def|class)\\s+.*")) {
            FileInfo pythonInfo = detectPythonFile(code);
            if (pythonInfo != null) return pythonInfo;
        }
        
        // 6. Détection SQL
        if (code.matches("(?si).*(?:CREATE|ALTER|INSERT|SELECT)\\s+.*")) {
            return new FileInfo("scripts/schema.sql", "sql");
        }
        
        // 7. Détection Dockerfile
        if (code.startsWith("FROM ")) {
            return new FileInfo("Dockerfile", "dockerfile");
        }
        
        // 8. Détection docker-compose.yml
        if (code.contains("version:") && code.contains("services:")) {
            return new FileInfo("docker-compose.yml", "yaml");
        }
        
        // 9. Détection requirements.txt
        if (code.matches("(?m)^[a-zA-Z0-9_-]+==\\d.*")) {
            return new FileInfo("requirements.txt", "text");
        }
        
        // 10. Détection README.md
        if (code.startsWith("# ") && code.length() > 1000) {
            return new FileInfo("README.md", "markdown");
        }
        
        // 11. Par défaut selon le hint
        if (hintLanguage != null) {
            return new FileInfo("unknown." + hintLanguage, hintLanguage);
        }
        
        return null;
    }

    /**
     * Détecte un fichier Java et construit son chemin
     * 
     * @param code Contenu du fichier Java
     * @return Informations sur le fichier
     */
    private FileInfo detectJavaFile(String code) {
        Pattern javaPattern = Pattern.compile(
            "package\\s+([a-z.]+);.*?(?:public\\s+)?(?:class|interface|enum|record)\\s+(\\w+)",
            Pattern.DOTALL
        );
        
        Matcher matcher = javaPattern.matcher(code);
        if (matcher.find()) {
            String packageName = matcher.group(1);
            String className = matcher.group(2);
            
            // Déterminer le module
            String module = determineModule(packageName);
            
            // Construire le chemin
            String packagePath = packageName.replace('.', '/');
            String path = String.format("%s/src/main/java/%s/%s.java", 
                module, packagePath, className);
            
            return new FileInfo(path, "java");
        }
        
        return null;
    }

    /**
     * Détecte un fichier Python et construit son chemin
     * 
     * @param code Contenu du fichier Python
     * @return Informations sur le fichier
     */
    private FileInfo detectPythonFile(String code) {
        // Si contient "if __name__ == '__main__':", c'est probablement worker.py
        if (code.contains("if __name__ == \"__main__\":") || 
            code.contains("if __name__ == '__main__':")) {
            return new FileInfo("nexus-image-worker/worker.py", "python");
        }
        
        // Par défaut
        return new FileInfo("nexus-image-worker/script.py", "python");
    }

    /**
     * Détermine le module Maven selon le package
     * 
     * @param packageName Nom du package Java
     * @return Nom du module
     */
    private String determineModule(String packageName) {
        if (packageName.contains(".domain")) {
            return "nexus-image-domain";
        } else if (packageName.contains(".infrastructure")) {
            return "nexus-image-infrastructure";
        } else if (packageName.contains(".core")) {
            return "nexus-image-core";
        } else if (packageName.contains(".api")) {
            return "nexus-image-api";
        } else if (packageName.contains(".config")) {
            return "nexus-image-config";
        } else {
            return "nexus-image-api"; // Par défaut
        }
    }

    /**
     * Crée un fichier avec son contenu
     * 
     * @param relativePath Chemin relatif du fichier
     * @param content Contenu du fichier
     * @throws IOException Si erreur de création
     */
    private void createFile(String relativePath, String content) throws IOException {
        Path fullPath = Path.of(outputBasePath, relativePath);
        
        // Créer les dossiers parents
        Files.createDirectories(fullPath.getParent());
        
        // Écrire le fichier
        Files.writeString(fullPath, content, StandardCharsets.UTF_8, 
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RAPPORTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Génère un rapport de parsing
     */
    public void generateReport() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║                    PARSING REPORT                        ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Statistiques générales
        System.out.println("📊 STATISTICS");
        System.out.println("  Total files: " + parsedFiles.size());
        System.out.println("  Total lines: " + parsedFiles.stream().mapToInt(f -> f.lineCount).sum());
        System.out.println("  Total size: " + formatBytes(parsedFiles.stream().mapToLong(f -> f.size).sum()));
        System.out.println();
        
        // Par type de fichier
        System.out.println("📁 FILES BY TYPE");
        fileTypeCounter.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> 
                System.out.println("  " + entry.getKey().toUpperCase() + ": " + entry.getValue() + " files")
            );
        System.out.println();
        
        // Liste des fichiers
        System.out.println("📄 GENERATED FILES");
        parsedFiles.stream()
            .sorted(Comparator.comparing(f -> f.relativePath))
            .forEach(file -> 
                System.out.println("  " + file.relativePath + " (" + file.lineCount + " lines)")
            );
        System.out.println();
        
        // Erreurs
        if (!errors.isEmpty()) {
            System.out.println("⚠️  ERRORS (" + errors.size() + ")");
            errors.forEach(error -> System.out.println("  - " + error));
            System.out.println();
        }
        
        System.out.println("✓ Report generated successfully!");
        System.out.println("📂 Output directory: " + outputBasePath);
    }

    /**
     * Sauvegarde le rapport dans un fichier
     * 
     * @param reportPath Chemin du fichier de rapport
     * @throws IOException Si erreur d'écriture
     */
    public void saveReport(String reportPath) throws IOException {
        StringBuilder report = new StringBuilder();
        
        report.append("# NEXUSAI - MODULE 5 PARSING REPORT\n\n");
        report.append("Date: ").append(new Date()).append("\n");
        report.append("Output path: ").append(outputBasePath).append("\n\n");
        
        report.append("## Statistics\n\n");
        report.append("- Total files: ").append(parsedFiles.size()).append("\n");
        report.append("- Total lines: ").append(parsedFiles.stream().mapToInt(f -> f.lineCount).sum()).append("\n");
        report.append("- Total size: ").append(formatBytes(parsedFiles.stream().mapToLong(f -> f.size).sum())).append("\n\n");
        
        report.append("## Files by Type\n\n");
        fileTypeCounter.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> 
                report.append("- ").append(entry.getKey().toUpperCase())
                      .append(": ").append(entry.getValue()).append(" files\n")
            );
        
        report.append("\n## Generated Files\n\n");
        parsedFiles.stream()
            .sorted(Comparator.comparing(f -> f.relativePath))
            .forEach(file -> 
                report.append("- `").append(file.relativePath)
                      .append("` (").append(file.lineCount).append(" lines)\n")
            );
        
        if (!errors.isEmpty()) {
            report.append("\n## Errors\n\n");
            errors.forEach(error -> report.append("- ").append(error).append("\n"));
        }
        
        Files.writeString(Path.of(reportPath), report.toString(), StandardCharsets.UTF_8);
        System.out.println("✓ Report saved to: " + reportPath);
    }

    /**
     * Formate une taille en bytes
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLASSES INTERNES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Représente un bloc de code extrait
     */
    private static class CodeBlock {
        String language;
        String content;

        CodeBlock(String language, String content) {
            this.language = language;
            this.content = content;
        }
    }

    /**
     * Informations sur un fichier détecté
     */
    private static class FileInfo {
        String path;
        String type;

        FileInfo(String path, String type) {
            this.path = path;
            this.type = type;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN - EXEMPLE D'UTILISATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Point d'entrée principal
     * 
     * @param args Arguments: [outputPath] [artifactPath1] [artifactPath2] ...
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ModuleFileParser <outputPath> [artifactPath1] [artifactPath2] ...");
            System.err.println();
            System.err.println("Example:");
            System.err.println("  java ModuleFileParser ./nexus-image-generation artifacts/*.md");
            System.exit(1);
        }
        
        try {
            String outputPath = args[0];
            
            // Créer le parser
            ModuleFileParser parser = new ModuleFileParser(outputPath);
            
            // Parser tous les artifacts fournis
            if (args.length > 1) {
                for (int i = 1; i < args.length; i++) {
                    parser.parseArtifact(args[i]);
                }
            } else {
                // Si pas d'artifact fourni, lire depuis stdin
                System.out.println("No artifacts provided. Reading from stdin...");
                System.out.println("Paste your content and press Ctrl+D (Unix) or Ctrl+Z (Windows) when done:");
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                StringBuilder content = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                
                parser.parseRawContent(content.toString());
            }
            
            // Générer et sauvegarder le rapport
            parser.generateReport();
            parser.saveReport(outputPath + "/PARSING_REPORT.md");
            
            System.out.println();
            System.out.println("✅ All done! Check the output at: " + outputPath);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
