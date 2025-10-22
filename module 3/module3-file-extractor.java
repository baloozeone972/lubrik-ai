// ============================================================================
// FICHIER: ModuleFileExtractor.java
// Description: Utilitaire pour extraire les fichiers depuis les artifacts
//              et les placer dans l'arborescence correcte
// ============================================================================

package com.nexusai.companion.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * Utilitaire pour extraire automatiquement les fichiers depuis les artifacts
 * et les placer dans l'arborescence du projet.
 * 
 * Gère les types de fichiers : Java, XML, YAML, Properties, JavaScript, Shell, etc.
 * 
 * Usage:
 * <pre>
 * ModuleFileExtractor extractor = new ModuleFileExtractor("./companion-service");
 * extractor.extractFromFile("artifacts.txt");
 * </pre>
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
public class ModuleFileExtractor {
    
    private final Path outputDirectory;
    private final Map<String, Integer> statistics;
    private final boolean verbose;
    
    // Patterns pour détecter les fichiers
    private static final Pattern FILE_PATTERN = Pattern.compile(
        "^(?://|#)\\s*(?:FICHIER|FILE)\\s*:\\s*(.+?)\\s*$",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
        "^package\\s+([a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*);\\s*$"
    );
    
    /**
     * Constructeur avec répertoire de sortie par défaut.
     */
    public ModuleFileExtractor() {
        this(Paths.get("."));
    }
    
    /**
     * Constructeur avec répertoire de sortie spécifié.
     * 
     * @param outputDirectory Répertoire de sortie pour l'arborescence
     */
    public ModuleFileExtractor(String outputDirectory) {
        this(Paths.get(outputDirectory));
    }
    
    /**
     * Constructeur avec Path.
     * 
     * @param outputDirectory Path du répertoire de sortie
     */
    public ModuleFileExtractor(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
        this.statistics = new HashMap<>();
        this.verbose = true;
    }
    
    /**
     * Constructeur avec mode verbose.
     */
    public ModuleFileExtractor(String outputDirectory, boolean verbose) {
        this(Paths.get(outputDirectory));
        this.verbose = verbose;
    }
    
    /**
     * Extrait tous les fichiers depuis un fichier d'artifact.
     * 
     * @param artifactFile Chemin du fichier d'artifact
     * @throws IOException Si erreur de lecture
     */
    public void extractFromFile(String artifactFile) throws IOException {
        extractFromFile(Paths.get(artifactFile));
    }
    
    /**
     * Extrait tous les fichiers depuis un fichier d'artifact.
     * 
     * @param artifactFile Path du fichier d'artifact
     * @throws IOException Si erreur de lecture
     */
    public void extractFromFile(Path artifactFile) throws IOException {
        log("📂 Lecture de l'artifact: " + artifactFile);
        
        String content = Files.readString(artifactFile, StandardCharsets.UTF_8);
        extractFromContent(content);
    }
    
    /**
     * Extrait tous les fichiers depuis le contenu brut.
     * 
     * @param content Contenu de l'artifact
     * @throws IOException Si erreur d'écriture
     */
    public void extractFromContent(String content) throws IOException {
        log("🔍 Parsing du contenu...");
        
        List<FileContent> files = parseContent(content);
        
        log("📝 " + files.size() + " fichiers détectés");
        
        for (FileContent file : files) {
            extractFile(file);
        }
        
        printStatistics();
    }
    
    /**
     * Parse le contenu pour extraire les fichiers.
     * 
     * @param content Contenu brut
     * @return Liste des fichiers détectés
     */
    private List<FileContent> parseContent(String content) {
        List<FileContent> files = new ArrayList<>();
        
        String[] lines = content.split("\n");
        
        FileContent currentFile = null;
        StringBuilder currentContent = new StringBuilder();
        
        for (String line : lines) {
            // Détecter un nouveau fichier
            Matcher fileMatcher = FILE_PATTERN.matcher(line);
            
            if (fileMatcher.find()) {
                // Sauvegarder le fichier précédent
                if (currentFile != null) {
                    currentFile.setContent(currentContent.toString().trim());
                    files.add(currentFile);
                }
                
                // Nouveau fichier
                String filePath = fileMatcher.group(1).trim();
                currentFile = new FileContent(filePath);
                currentContent = new StringBuilder();
                
            } else if (currentFile != null) {
                // Accumuler le contenu
                currentContent.append(line).append("\n");
            }
        }
        
        // Ajouter le dernier fichier
        if (currentFile != null) {
            currentFile.setContent(currentContent.toString().trim());
            files.add(currentFile);
        }
        
        return files;
    }
    
    /**
     * Extrait et écrit un fichier.
     * 
     * @param file Contenu du fichier
     * @throws IOException Si erreur d'écriture
     */
    private void extractFile(FileContent file) throws IOException {
        Path targetPath = resolveTargetPath(file);
        
        // Créer les répertoires parents
        Files.createDirectories(targetPath.getParent());
        
        // Écrire le fichier
        Files.writeString(targetPath, file.getContent(), StandardCharsets.UTF_8);
        
        // Rendre exécutable si script shell
        if (file.getPath().endsWith(".sh")) {
            targetPath.toFile().setExecutable(true);
        }
        
        // Statistiques
        String extension = getExtension(file.getPath());
        statistics.merge(extension, 1, Integer::sum);
        
        log("✅ Créé: " + targetPath);
    }
    
    /**
     * Résout le chemin de destination du fichier.
     * 
     * @param file Fichier à extraire
     * @return Path de destination
     */
    private Path resolveTargetPath(FileContent file) {
        String filePath = file.getPath();
        
        // Cas spécial pour les fichiers Java avec package
        if (filePath.endsWith(".java")) {
            String packagePath = extractPackagePath(file.getContent());
            if (packagePath != null) {
                String fileName = Paths.get(filePath).getFileName().toString();
                filePath = "src/main/java/" + packagePath + "/" + fileName;
            }
        }
        
        return outputDirectory.resolve(filePath);
    }
    
    /**
     * Extrait le chemin du package depuis un fichier Java.
     * 
     * @param content Contenu du fichier Java
     * @return Chemin du package (ex: com/nexusai/companion)
     */
    private String extractPackagePath(String content) {
        Matcher matcher = PACKAGE_PATTERN.matcher(content);
        if (matcher.find()) {
            String packageName = matcher.group(1);
            return packageName.replace('.', '/');
        }
        return null;
    }
    
    /**
     * Retourne l'extension d'un fichier.
     * 
     * @param filePath Chemin du fichier
     * @return Extension (ex: "java", "xml")
     */
    private String getExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filePath.length() - 1) {
            return filePath.substring(lastDot + 1).toLowerCase();
        }
        return "other";
    }
    
    /**
     * Affiche les statistiques d'extraction.
     */
    private void printStatistics() {
        log("\n" + "=".repeat(60));
        log("📊 STATISTIQUES D'EXTRACTION");
        log("=".repeat(60));
        
        int total = statistics.values().stream().mapToInt(Integer::intValue).sum();
        
        statistics.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> {
                String ext = entry.getKey();
                int count = entry.getValue();
                double percentage = (count * 100.0) / total;
                log(String.format("  %-10s : %3d fichiers (%.1f%%)", 
                    ext, count, percentage));
            });
        
        log("-".repeat(60));
        log(String.format("  TOTAL      : %3d fichiers", total));
        log("=".repeat(60));
    }
    
    /**
     * Affiche un message (si verbose).
     */
    private void log(String message) {
        if (verbose) {
            System.out.println(message);
        }
    }
    
    /**
     * Classe interne représentant un fichier.
     */
    private static class FileContent {
        private final String path;
        private String content;
        
        public FileContent(String path) {
            this.path = path;
        }
        
        public String getPath() {
            return path;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
    
    /**
     * Méthode principale pour tester l'extracteur.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ModuleFileExtractor <output-directory> [artifact-file]");
            System.err.println("  ou : java ModuleFileExtractor <output-directory> < artifact-content.txt");
            System.exit(1);
        }
        
        try {
            String outputDir = args[0];
            ModuleFileExtractor extractor = new ModuleFileExtractor(outputDir);
            
            if (args.length >= 2) {
                // Extraction depuis un fichier
                extractor.extractFromFile(args[1]);
            } else {
                // Extraction depuis stdin
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8)
                );
                String content = reader.lines().collect(Collectors.joining("\n"));
                extractor.extractFromContent(content);
            }
            
            System.out.println("\n✅ Extraction terminée avec succès!");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

// ============================================================================
// FICHIER: ModuleFileExtractorTest.java
// Description: Tests unitaires pour ModuleFileExtractor
// ============================================================================

package com.nexusai.companion.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour ModuleFileExtractor.
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
class ModuleFileExtractorTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    @DisplayName("Extraction d'un fichier Java simple")
    void testExtractSimpleJavaFile() throws IOException {
        String content = """
            // FICHIER: HelloWorld.java
            package com.example;
            
            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println("Hello, World!");
                }
            }
            """;
        
        ModuleFileExtractor extractor = new ModuleFileExtractor(tempDir, false);
        extractor.extractFromContent(content);
        
        Path expectedPath = tempDir.resolve("src/main/java/com/example/HelloWorld.java");
        assertTrue(Files.exists(expectedPath), "Le fichier devrait exister");
        
        String extractedContent = Files.readString(expectedPath);
        assertTrue(extractedContent.contains("public class HelloWorld"));
    }
    
    @Test
    @DisplayName("Extraction de plusieurs fichiers")
    void testExtractMultipleFiles() throws IOException {
        String content = """
            // FICHIER: pom.xml
            <project>
                <modelVersion>4.0.0</modelVersion>
            </project>
            
            // FICHIER: README.md
            # My Project
            This is a test project.
            
            // FICHIER: config.yml
            server:
              port: 8080
            """;
        
        ModuleFileExtractor extractor = new ModuleFileExtractor(tempDir, false);
        extractor.extractFromContent(content);
        
        assertTrue(Files.exists(tempDir.resolve("pom.xml")));
        assertTrue(Files.exists(tempDir.resolve("README.md")));
        assertTrue(Files.exists(tempDir.resolve("config.yml")));
    }
    
    @Test
    @DisplayName("Extraction avec création de répertoires")
    void testExtractWithDirectoryCreation() throws IOException {
        String content = """
            // FICHIER: src/main/resources/application.yml
            spring:
              application:
                name: test-app
            """;
        
        ModuleFileExtractor extractor = new ModuleFileExtractor(tempDir, false);
        extractor.extractFromContent(content);
        
        Path expectedPath = tempDir.resolve("src/main/resources/application.yml");
        assertTrue(Files.exists(expectedPath));
        assertTrue(Files.isDirectory(expectedPath.getParent()));
    }
    
    @Test
    @DisplayName("Extraction de script shell exécutable")
    void testExtractShellScript() throws IOException {
        String content = """
            // FICHIER: scripts/deploy.sh
            #!/bin/bash
            echo "Deploying..."
            """;
        
        ModuleFileExtractor extractor = new ModuleFileExtractor(tempDir, false);
        extractor.extractFromContent(content);
        
        Path scriptPath = tempDir.resolve("scripts/deploy.sh");
        assertTrue(Files.exists(scriptPath));
        assertTrue(Files.isExecutable(scriptPath), "Le script devrait être exécutable");
    }
    
    @Test
    @DisplayName("Gestion des commentaires de différents formats")
    void testDifferentCommentFormats() throws IOException {
        String content = """
            // FICHIER: Test1.java
            public class Test1 {}
            
            # FICHIER: Test2.py
            print("hello")
            
            # FILE: test3.txt
            Some text content
            """;
        
        ModuleFileExtractor extractor = new ModuleFileExtractor(tempDir, false);
        extractor.extractFromContent(content);
        
        assertTrue(Files.exists(tempDir.resolve("src/main/java/Test1.java")));
        assertTrue(Files.exists(tempDir.resolve("Test2.py")));
        assertTrue(Files.exists(tempDir.resolve("test3.txt")));
    }
}

// ============================================================================
// FICHIER: ExtractorCLI.java
// Description: Interface en ligne de commande pour l'extracteur
// ============================================================================

package com.nexusai.companion.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

/**
 * Interface en ligne de commande pour ModuleFileExtractor.
 * 
 * Usage:
 * <pre>
 * # Extraire depuis un fichier
 * java ExtractorCLI --output ./companion-service --input artifacts.txt
 * 
 * # Extraire depuis stdin
 * cat artifacts.txt | java ExtractorCLI --output ./companion-service
 * 
 * # Mode interactif
 * java ExtractorCLI
 * </pre>
 * 
 * @author NexusAI Dev Team
 * @version 1.0
 */
public class ExtractorCLI {
    
    private static final String VERSION = "1.0.0";
    
    public static void main(String[] args) {
        try {
            // Parser les arguments
            Arguments arguments = parseArguments(args);
            
            if (arguments.showHelp) {
                printHelp();
                return;
            }
            
            if (arguments.showVersion) {
                printVersion();
                return;
            }
            
            // Créer l'extracteur
            ModuleFileExtractor extractor = new ModuleFileExtractor(
                arguments.outputDirectory,
                arguments.verbose
            );
            
            // Extraire
            if (arguments.inputFile != null) {
                // Depuis un fichier
                extractor.extractFromFile(arguments.inputFile);
            } else if (!arguments.interactive) {
                // Depuis stdin
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8)
                );
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                extractor.extractFromContent(content.toString());
            } else {
                // Mode interactif
                runInteractive(extractor);
            }
            
            System.out.println("\n✅ Extraction terminée avec succès!");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            if (Arrays.asList(args).contains("--debug")) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
    
    /**
     * Parse les arguments de ligne de commande.
     */
    private static Arguments parseArguments(String[] args) {
        Arguments arguments = new Arguments();
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o", "--output":
                    if (i + 1 < args.length) {
                        arguments.outputDirectory = args[++i];
                    }
                    break;
                    
                case "-i", "--input":
                    if (i + 1 < args.length) {
                        arguments.inputFile = args[++i];
                    }
                    break;
                    
                case "-h", "--help":
                    arguments.showHelp = true;
                    break;
                    
                case "-v", "--version":
                    arguments.showVersion = true;
                    break;
                    
                case "--quiet":
                    arguments.verbose = false;
                    break;
                    
                case "--interactive":
                    arguments.interactive = true;
                    break;
            }
        }
        
        // Valeurs par défaut
        if (arguments.outputDirectory == null) {
            arguments.outputDirectory = "./companion-service";
        }
        
        return arguments;
    }
    
    /**
     * Affiche l'aide.
     */
    private static void printHelp() {
        System.out.println("""
            ModuleFileExtractor - Extracteur de fichiers depuis artifacts
            
            Usage:
              java ExtractorCLI [options]
            
            Options:
              -o, --output <dir>     Répertoire de sortie (défaut: ./companion-service)
              -i, --input <file>     Fichier d'artifact en entrée
              --interactive          Mode interactif
              --quiet                Mode silencieux
              -h, --help             Afficher cette aide
              -v, --version          Afficher la version
            
            Exemples:
              # Extraire depuis un fichier
              java ExtractorCLI --output ./my-project --input artifacts.txt
              
              # Extraire depuis stdin
              cat artifacts.txt | java ExtractorCLI --output ./my-project
              
              # Mode interactif
              java ExtractorCLI --interactive
            """);
    }
    
    /**
     * Affiche la version.
     */
    private static void printVersion() {
        System.out.println("ModuleFileExtractor version " + VERSION);
    }
    
    /**
     * Mode interactif.
     */
    private static void runInteractive(ModuleFileExtractor extractor) throws IOException {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(System.in, StandardCharsets.UTF_8)
        );
        
        System.out.println("=".repeat(60));
        System.out.println("MODE INTERACTIF - ModuleFileExtractor");
        System.out.println("=".repeat(60));
        System.out.println("Collez le contenu de l'artifact ci-dessous.");
        System.out.println("Terminez avec une ligne contenant uniquement 'END'");
        System.out.println("-".repeat(60));
        
        StringBuilder content = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            if ("END".equals(line.trim())) {
                break;
            }
            content.append(line).append("\n");
        }
        
        if (content.length() == 0) {
            System.out.println("Aucun contenu fourni. Abandon.");
            return;
        }
        
        extractor.extractFromContent(content.toString());
    }
    
    /**
     * Classe pour stocker les arguments.
     */
    private static class Arguments {
        String outputDirectory;
        String inputFile;
        boolean showHelp = false;
        boolean showVersion = false;
        boolean verbose = true;
        boolean interactive = false;
    }
}