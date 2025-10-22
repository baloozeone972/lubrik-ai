import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * Générateur d'arborescence de fichiers à partir de documentation technique.
 * 
 * <p>Cette classe permet de parser du contenu structuré (Markdown, commentaires)
 * contenant du code et de générer automatiquement l'arborescence complète
 * des fichiers du projet.</p>
 * 
 * <p><b>Exemple d'utilisation :</b></p>
 * <pre>{@code
 * // Créer le générateur
 * FileTreeGenerator generator = new FileTreeGenerator("./payment-service");
 * 
 * // Parser et générer depuis un fichier
 * generator.parseAndGenerate(new File("documentation.md"));
 * 
 * // Ou depuis une String
 * String content = "..."; // Contenu de la documentation
 * generator.parseAndGenerateFromString(content);
 * 
 * // Obtenir le rapport
 * GenerationReport report = generator.getLastReport();
 * System.out.println(report);
 * }</pre>
 * 
 * @author NexusAI Team
 * @version 1.0
 */
public class FileTreeGenerator {
    
    private final Path outputRoot;
    private GenerationReport lastReport;
    
    /**
     * Constructeur avec chemin de sortie.
     * 
     * @param outputPath Chemin racine où générer l'arborescence
     */
    public FileTreeGenerator(String outputPath) {
        this.outputRoot = Paths.get(outputPath).toAbsolutePath();
        this.lastReport = new GenerationReport();
    }
    
    /**
     * Parse et génère l'arborescence depuis un fichier.
     * 
     * @param inputFile Fichier contenant la documentation
     * @throws IOException Si erreur de lecture
     */
    public void parseAndGenerate(File inputFile) throws IOException {
        String content = Files.readString(inputFile.toPath());
        parseAndGenerateFromString(content);
    }
    
    /**
     * Parse et génère l'arborescence depuis une chaîne.
     * 
     * @param content Contenu de la documentation
     * @throws IOException Si erreur d'écriture
     */
    public void parseAndGenerateFromString(String content) throws IOException {
        lastReport = new GenerationReport();
        lastReport.startTime = System.currentTimeMillis();
        
        System.out.println("🚀 Démarrage de la génération d'arborescence...");
        System.out.println("📁 Dossier de sortie: " + outputRoot);
        System.out.println();
        
        // Extraire tous les blocs de code
        List<CodeBlock> codeBlocks = extractCodeBlocks(content);
        System.out.println("📦 " + codeBlocks.size() + " blocs de code détectés");
        
        // Traiter chaque bloc
        for (CodeBlock block : codeBlocks) {
            try {
                processCodeBlock(block);
            } catch (Exception e) {
                lastReport.errors.add("Erreur traitement bloc: " + e.getMessage());
                System.err.println("❌ Erreur: " + e.getMessage());
            }
        }
        
        lastReport.endTime = System.currentTimeMillis();
        printReport();
    }
    
    /**
     * Extrait tous les blocs de code du contenu.
     */
    private List<CodeBlock> extractCodeBlocks(String content) {
        List<CodeBlock> blocks = new ArrayList<>();
        
        // Pattern pour blocs Markdown avec language
        Pattern markdownPattern = Pattern.compile(
            "```(\\w+)\\s*\\n(.*?)```",
            Pattern.DOTALL
        );
        
        Matcher matcher = markdownPattern.matcher(content);
        while (matcher.find()) {
            String language = matcher.group(1);
            String code = matcher.group(2);
            blocks.add(new CodeBlock(language, code));
        }
        
        // Pattern pour commentaires Java multi-lignes contenant du code
        Pattern javaCommentPattern = Pattern.compile(
            "/\\*\\*.*?\\*/(.*?)(?=\\/\\*\\*|$)",
            Pattern.DOTALL
        );
        
        matcher = javaCommentPattern.matcher(content);
        while (matcher.find()) {
            String code = matcher.group(1).trim();
            if (code.startsWith("package ") || code.contains("class ") || code.contains("interface ")) {
                blocks.add(new CodeBlock("java", code));
            }
        }
        
        return blocks;
    }
    
    /**
     * Traite un bloc de code et génère le(s) fichier(s).
     */
    private void processCodeBlock(CodeBlock block) throws IOException {
        // Déterminer le type de fichier
        FileType fileType = detectFileType(block);
        
        if (fileType == FileType.UNKNOWN) {
            lastReport.skipped++;
            return;
        }
        
        // Extraire les fichiers du bloc
        List<FileInfo> files = extractFiles(block, fileType);
        
        // Créer chaque fichier
        for (FileInfo file : files) {
            createFile(file);
        }
    }
    
    /**
     * Détecte le type de fichier depuis le bloc de code.
     */
    private FileType detectFileType(CodeBlock block) {
        String lang = block.language.toLowerCase();
        String code = block.code.trim();
        
        // Détection par language déclaré
        if (lang.equals("java")) return FileType.JAVA;
        if (lang.equals("xml")) return FileType.XML;
        if (lang.equals("yaml") || lang.equals("yml")) return FileType.YAML;
        if (lang.equals("properties")) return FileType.PROPERTIES;
        if (lang.equals("sql")) return FileType.SQL;
        if (lang.equals("bash") || lang.equals("sh")) return FileType.SHELL;
        if (lang.equals("dockerfile")) return FileType.DOCKERFILE;
        if (lang.equals("markdown") || lang.equals("md")) return FileType.MARKDOWN;
        
        // Détection par contenu
        if (code.contains("package ") && code.contains("class ")) return FileType.JAVA;
        if (code.startsWith("<?xml")) return FileType.XML;
        if (code.startsWith("<project") && code.contains("xmlns")) return FileType.XML;
        if (code.contains("apiVersion:") || code.contains("kind:")) return FileType.YAML;
        if (code.contains("spring:") || code.contains("server:")) return FileType.YAML;
        if (code.startsWith("#") && code.contains("!/bin/")) return FileType.SHELL;
        if (code.startsWith("FROM ")) return FileType.DOCKERFILE;
        if (code.startsWith("CREATE TABLE") || code.contains("SELECT ")) return FileType.SQL;
        
        return FileType.UNKNOWN;
    }
    
    /**
     * Extrait les informations de fichiers depuis un bloc de code.
     */
    private List<FileInfo> extractFiles(CodeBlock block, FileType fileType) {
        List<FileInfo> files = new ArrayList<>();
        
        switch (fileType) {
            case JAVA:
                files.addAll(extractJavaFiles(block.code));
                break;
            case XML:
                files.addAll(extractXmlFiles(block.code));
                break;
            case YAML:
                files.addAll(extractYamlFiles(block.code));
                break;
            case SQL:
                files.addAll(extractSqlFiles(block.code));
                break;
            case SHELL:
                files.addAll(extractShellFiles(block.code));
                break;
            case DOCKERFILE:
                files.add(new FileInfo("Dockerfile", "", block.code));
                break;
            case PROPERTIES:
                files.add(new FileInfo("application.properties", "src/main/resources", block.code));
                break;
            case MARKDOWN:
                files.addAll(extractMarkdownFiles(block.code));
                break;
        }
        
        return files;
    }
    
    /**
     * Extrait les fichiers Java depuis le code.
     */
    private List<FileInfo> extractJavaFiles(String code) {
        List<FileInfo> files = new ArrayList<>();
        
        // Découper en classes/interfaces si plusieurs dans le même bloc
        List<String> classBlocks = splitJavaClasses(code);
        
        for (String classBlock : classBlocks) {
            // Extraire package
            Pattern packagePattern = Pattern.compile("package\\s+([\\w.]+);");
            Matcher packageMatcher = packagePattern.matcher(classBlock);
            String packageName = "";
            if (packageMatcher.find()) {
                packageName = packageMatcher.group(1);
            }
            
            // Extraire nom de classe/interface
            Pattern classPattern = Pattern.compile(
                "(?:public\\s+)?(?:class|interface|enum|@interface)\\s+(\\w+)"
            );
            Matcher classMatcher = classPattern.matcher(classBlock);
            
            if (classMatcher.find()) {
                String className = classMatcher.group(1);
                String relativePath = packageName.replace('.', '/');
                String fileName = className + ".java";
                
                files.add(new FileInfo(
                    fileName,
                    "src/main/java/" + relativePath,
                    classBlock
                ));
            }
        }
        
        return files;
    }
    
    /**
     * Découpe le code Java en classes séparées.
     */
    private List<String> splitJavaClasses(String code) {
        List<String> classes = new ArrayList<>();
        
        // Pattern pour détecter le début d'une nouvelle classe
        Pattern classStartPattern = Pattern.compile(
            "^(package\\s+[\\w.]+;.*?)?(?:public\\s+)?(?:class|interface|enum)\\s+\\w+",
            Pattern.MULTILINE | Pattern.DOTALL
        );
        
        // Si une seule classe, retourner tel quel
        Matcher matcher = classStartPattern.matcher(code);
        int matchCount = 0;
        while (matcher.find()) matchCount++;
        
        if (matchCount <= 1) {
            classes.add(code);
            return classes;
        }
        
        // Sinon, découper
        String[] lines = code.split("\n");
        StringBuilder currentClass = new StringBuilder();
        int braceCount = 0;
        boolean inClass = false;
        String currentPackage = "";
        
        for (String line : lines) {
            if (line.trim().startsWith("package ")) {
                currentPackage = line;
                continue;
            }
            
            if (line.matches(".*\\b(class|interface|enum)\\s+\\w+.*") && braceCount == 0) {
                if (inClass && currentClass.length() > 0) {
                    classes.add(currentClass.toString());
                    currentClass = new StringBuilder();
                }
                inClass = true;
                if (!currentPackage.isEmpty()) {
                    currentClass.append(currentPackage).append("\n\n");
                }
            }
            
            if (inClass) {
                currentClass.append(line).append("\n");
                braceCount += countChar(line, '{') - countChar(line, '}');
                
                if (braceCount == 0 && line.contains("}")) {
                    classes.add(currentClass.toString());
                    currentClass = new StringBuilder();
                    inClass = false;
                }
            }
        }
        
        if (currentClass.length() > 0) {
            classes.add(currentClass.toString());
        }
        
        return classes;
    }
    
    /**
     * Extrait les fichiers XML (pom.xml, config, etc.).
     */
    private List<FileInfo> extractXmlFiles(String code) {
        List<FileInfo> files = new ArrayList<>();
        
        String fileName = "file.xml";
        String relativePath = "";
        
        // Détecter pom.xml
        if (code.contains("<project") && code.contains("xmlns")) {
            fileName = "pom.xml";
            relativePath = "";
        }
        // Détecter beans.xml
        else if (code.contains("<beans")) {
            fileName = "beans.xml";
            relativePath = "src/main/resources";
        }
        
        files.add(new FileInfo(fileName, relativePath, code));
        return files;
    }
    
    /**
     * Extrait les fichiers YAML.
     */
    private List<FileInfo> extractYamlFiles(String code) {
        List<FileInfo> files = new ArrayList<>();
        
        String fileName = "config.yml";
        String relativePath = "";
        
        // Chercher les commentaires indiquant le nom du fichier
        Pattern fileNamePattern = Pattern.compile("^#\\s*([\\w.-]+\\.ya?ml)\\s*$", Pattern.MULTILINE);
        Matcher matcher = fileNamePattern.matcher(code);
        if (matcher.find()) {
            fileName = matcher.group(1);
        }
        
        // Détecter application.yml
        if (code.contains("spring:") || code.contains("server:")) {
            fileName = "application.yml";
            relativePath = "src/main/resources";
        }
        // Détecter docker-compose.yml
        else if (code.contains("version:") && code.contains("services:")) {
            fileName = "docker-compose.yml";
            relativePath = "";
        }
        // Détecter Kubernetes
        else if (code.contains("apiVersion:") && code.contains("kind:")) {
            String kind = extractYamlValue(code, "kind");
            fileName = kind.toLowerCase() + ".yaml";
            relativePath = "k8s";
        }
        
        files.add(new FileInfo(fileName, relativePath, code));
        return files;
    }
    
    /**
     * Extrait les fichiers SQL.
     */
    private List<FileInfo> extractSqlFiles(String code) {
        List<FileInfo> files = new ArrayList<>();
        
        String fileName = "schema.sql";
        String relativePath = "db/migrations";
        
        // Chercher pattern de migration Flyway
        Pattern migrationPattern = Pattern.compile("-- Migration (V\\d+__[\\w_]+\\.sql)");
        Matcher matcher = migrationPattern.matcher(code);
        if (matcher.find()) {
            fileName = matcher.group(1);
        }
        
        files.add(new FileInfo(fileName, relativePath, code));
        return files;
    }
    
    /**
     * Extrait les fichiers Shell.
     */
    private List<FileInfo> extractShellFiles(String code) {
        List<FileInfo> files = new ArrayList<>();
        
        String fileName = "script.sh";
        String relativePath = "scripts";
        
        // Chercher le nom dans les commentaires
        Pattern fileNamePattern = Pattern.compile("^#\\s*([\\w.-]+\\.sh)\\s*$", Pattern.MULTILINE);
        Matcher matcher = fileNamePattern.matcher(code);
        if (matcher.find()) {
            fileName = matcher.group(1);
        }
        
        files.add(new FileInfo(fileName, relativePath, code));
        return files;
    }
    
    /**
     * Extrait les fichiers Markdown.
     */
    private List<FileInfo> extractMarkdownFiles(String code) {
        List<FileInfo> files = new ArrayList<>();
        
        String fileName = "README.md";
        String relativePath = "";
        
        // Chercher titre principal
        Pattern titlePattern = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = titlePattern.matcher(code);
        if (matcher.find()) {
            String title = matcher.group(1).trim();
            if (title.toUpperCase().contains("README")) {
                fileName = "README.md";
            } else {
                fileName = title.replaceAll("[^a-zA-Z0-9-]", "-").toLowerCase() + ".md";
            }
        }
        
        files.add(new FileInfo(fileName, relativePath, code));
        return files;
    }
    
    /**
     * Crée physiquement un fichier sur le disque.
     */
    private void createFile(FileInfo fileInfo) throws IOException {
        Path fullPath;
        
        if (fileInfo.relativePath.isEmpty()) {
            fullPath = outputRoot.resolve(fileInfo.fileName);
        } else {
            fullPath = outputRoot.resolve(fileInfo.relativePath).resolve(fileInfo.fileName);
        }
        
        // Créer les dossiers parents si nécessaire
        Files.createDirectories(fullPath.getParent());
        
        // Nettoyer le contenu (supprimer les marqueurs de commentaire si présents)
        String cleanContent = cleanContent(fileInfo.content);
        
        // Écrire le fichier
        Files.writeString(fullPath, cleanContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        lastReport.filesCreated++;
        lastReport.createdFiles.add(fullPath.toString());
        
        System.out.println("✅ Créé: " + outputRoot.relativize(fullPath));
    }
    
    /**
     * Nettoie le contenu avant écriture.
     */
    private String cleanContent(String content) {
        // Supprimer les marqueurs XML de commentaires si présents
        content = content.replaceAll("^<!--\\s*", "");
        content = content.replaceAll("\\s*-->$", "");
        
        // Supprimer les commentaires de documentation si présents au début
        if (content.trim().startsWith("/**")) {
            content = content.replaceFirst("/\\*\\*.*?\\*/\\s*", "");
        }
        
        return content.trim() + "\n";
    }
    
    /**
     * Utilitaire : compte les occurrences d'un caractère.
     */
    private int countChar(String str, char c) {
        return (int) str.chars().filter(ch -> ch == c).count();
    }
    
    /**
     * Utilitaire : extrait une valeur YAML.
     */
    private String extractYamlValue(String yaml, String key) {
        Pattern pattern = Pattern.compile(key + ":\\s*([\\w-]+)");
        Matcher matcher = pattern.matcher(yaml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }
    
    /**
     * Affiche le rapport de génération.
     */
    private void printReport() {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("📊 RAPPORT DE GÉNÉRATION");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println(lastReport);
        System.out.println("═══════════════════════════════════════════════════════");
    }
    
    /**
     * Retourne le dernier rapport de génération.
     */
    public GenerationReport getLastReport() {
        return lastReport;
    }
    
    // ========================================================================
    // CLASSES INTERNES
    // ========================================================================
    
    /**
     * Représente un bloc de code extrait.
     */
    private static class CodeBlock {
        String language;
        String code;
        
        CodeBlock(String language, String code) {
            this.language = language;
            this.code = code;
        }
    }
    
    /**
     * Représente les informations d'un fichier à créer.
     */
    private static class FileInfo {
        String fileName;
        String relativePath;
        String content;
        
        FileInfo(String fileName, String relativePath, String content) {
            this.fileName = fileName;
            this.relativePath = relativePath;
            this.content = content;
        }
    }
    
    /**
     * Types de fichiers supportés.
     */
    private enum FileType {
        JAVA, XML, YAML, PROPERTIES, SQL, SHELL, DOCKERFILE, MARKDOWN, UNKNOWN
    }
    
    /**
     * Rapport de génération.
     */
    public static class GenerationReport {
        int filesCreated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        List<String> createdFiles = new ArrayList<>();
        long startTime;
        long endTime;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("✅ Fichiers créés: ").append(filesCreated).append("\n");
            sb.append("⏭️  Blocs ignorés: ").append(skipped).append("\n");
            sb.append("❌ Erreurs: ").append(errors.size()).append("\n");
            sb.append("⏱️  Durée: ").append((endTime - startTime)).append(" ms\n");
            
            if (!errors.isEmpty()) {
                sb.append("\n❌ ERREURS:\n");
                errors.forEach(e -> sb.append("  - ").append(e).append("\n"));
            }
            
            return sb.toString();
        }
    }
    
    // ========================================================================
    // MAIN - EXEMPLE D'UTILISATION
    // ========================================================================
    
    public static void main(String[] args) {
        try {
            // Vérifier les arguments
            if (args.length < 1) {
                System.err.println("Usage: java FileTreeGenerator <chemin-sortie> [fichier-doc]");
                System.err.println();
                System.err.println("Exemples:");
                System.err.println("  java FileTreeGenerator ./payment-service");
                System.err.println("  java FileTreeGenerator ./payment-service documentation.md");
                System.exit(1);
            }
            
            String outputPath = args[0];
            
            // Créer le générateur
            FileTreeGenerator generator = new FileTreeGenerator(outputPath);
            
            if (args.length >= 2) {
                // Parser depuis un fichier
                File inputFile = new File(args[1]);
                if (!inputFile.exists()) {
                    System.err.println("❌ Fichier introuvable: " + args[1]);
                    System.exit(1);
                }
                generator.parseAndGenerate(inputFile);
            } else {
                // Exemple avec String (pour test)
                String sampleContent = """
                    # Exemple de documentation
                    
                    ```java
                    package com.nexusai.payment.domain;
                    
                    public class Subscription {
                        private UUID id;
                        private String plan;
                    }
                    ```
                    
                    ```xml
                    <?xml version="1.0"?>
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                    </project>
                    ```
                    """;
                
                generator.parseAndGenerateFromString(sampleContent);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur fatale: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

/**
 * Classe utilitaire pour parser des fichiers de documentation spécifiques.
 * 
 * <p>Simplifie l'utilisation du FileTreeGenerator pour des cas courants.</p>
 */
class DocumentationParser {
    
    /**
     * Parse une documentation complète de module et génère l'arborescence.
     * 
     * @param documentationContent Contenu de la documentation
     * @param outputPath Chemin de sortie
     * @return Rapport de génération
     */
    public static FileTreeGenerator.GenerationReport parseModuleDocumentation(
            String documentationContent, 
            String outputPath) throws IOException {
        
        FileTreeGenerator generator = new FileTreeGenerator(outputPath);
        generator.parseAndGenerateFromString(documentationContent);
        return generator.getLastReport();
    }
    
    /**
     * Parse plusieurs fichiers de documentation et les fusionne.
     * 
     * @param documentationFiles Liste de fichiers
     * @param outputPath Chemin de sortie
     * @return Rapport de génération
     */
    public static FileTreeGenerator.GenerationReport parseMultipleDocuments(
            List<File> documentationFiles,
            String outputPath) throws IOException {
        
        StringBuilder combined = new StringBuilder();
        for (File file : documentationFiles) {
            combined.append(Files.readString(file.toPath())).append("\n\n");
        }
        
        return parseModuleDocumentation(combined.toString(), outputPath);
    }
}