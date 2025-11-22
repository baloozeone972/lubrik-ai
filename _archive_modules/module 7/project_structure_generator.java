package com.nexusai.tools;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Classe principale pour générer l'arborescence du projet à partir de fichiers de documentation.
 * 
 * Cette classe parse des fichiers contenant plusieurs types de code (Java, XML, YAML, etc.)
 * et les extrait dans une structure de répertoires appropriée.
 * 
 * Usage:
 * <pre>
 * ProjectStructureGenerator generator = new ProjectStructureGenerator();
 * generator.setOutputPath(Paths.get("./output"));
 * generator.parseAndGenerate(Paths.get("./docs/module-7-artifacts.md"));
 * </pre>
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01
 */
@Slf4j
@Data
public class ProjectStructureGenerator {

    private Path outputPath;
    private boolean overwriteExisting = false;
    private boolean createBackup = true;
    private FileTypeDetector fileTypeDetector;
    private CodeBlockParser codeBlockParser;
    private FileStructureWriter fileWriter;
    
    private Map<String, Integer> statistics = new HashMap<>();

    public ProjectStructureGenerator() {
        this.fileTypeDetector = new FileTypeDetector();
        this.codeBlockParser = new CodeBlockParser();
        this.fileWriter = new FileStructureWriter();
    }

    /**
     * Parse un fichier de documentation et génère l'arborescence.
     * 
     * @param inputFile Fichier d'entrée contenant la documentation
     * @throws IOException Si erreur de lecture/écriture
     */
    public void parseAndGenerate(Path inputFile) throws IOException {
        log.info("Début du parsing du fichier: {}", inputFile);
        
        if (!Files.exists(inputFile)) {
            throw new FileNotFoundException("Fichier non trouvé: " + inputFile);
        }

        // Validation du répertoire de sortie
        validateOutputPath();

        // Lecture du contenu
        String content = Files.readString(inputFile);
        
        // Extraction des blocs de code
        List<CodeBlock> codeBlocks = codeBlockParser.extractCodeBlocks(content);
        log.info("Nombre de blocs de code trouvés: {}", codeBlocks.size());

        // Traitement de chaque bloc
        for (CodeBlock block : codeBlocks) {
            processCodeBlock(block);
        }

        // Affichage des statistiques
        displayStatistics();
    }

    /**
     * Parse plusieurs fichiers et génère l'arborescence.
     * 
     * @param inputFiles Liste des fichiers d'entrée
     * @throws IOException Si erreur de lecture/écriture
     */
    public void parseAndGenerate(List<Path> inputFiles) throws IOException {
        for (Path inputFile : inputFiles) {
            parseAndGenerate(inputFile);
        }
    }

    /**
     * Traite un bloc de code individuel.
     */
    private void processCodeBlock(CodeBlock block) {
        try {
            // Détection du type de fichier
            FileType fileType = fileTypeDetector.detectFileType(block);
            
            if (fileType == FileType.UNKNOWN) {
                log.warn("Type de fichier inconnu pour le bloc à la ligne {}", block.getLineNumber());
                incrementStatistic("unknown");
                return;
            }

            // Détermination du chemin de destination
            Path targetPath = determineTargetPath(block, fileType);
            
            if (targetPath == null) {
                log.warn("Impossible de déterminer le chemin cible pour: {}", block.getFilename());
                incrementStatistic("skipped");
                return;
            }

            // Écriture du fichier
            fileWriter.writeFile(outputPath, targetPath, block.getContent(), overwriteExisting, createBackup);
            
            log.info("Fichier créé: {}", targetPath);
            incrementStatistic(fileType.name().toLowerCase());
            incrementStatistic("total");

        } catch (Exception e) {
            log.error("Erreur lors du traitement du bloc: {}", block.getFilename(), e);
            incrementStatistic("errors");
        }
    }

    /**
     * Détermine le chemin de destination d'un fichier.
     */
    private Path determineTargetPath(CodeBlock block, FileType fileType) {
        // Si un chemin explicite est fourni dans le bloc
        if (block.getFilename() != null && !block.getFilename().isEmpty()) {
            return Paths.get(block.getFilename());
        }

        // Si un commentaire de chemin existe dans le contenu
        String pathFromComment = extractPathFromComment(block.getContent(), fileType);
        if (pathFromComment != null) {
            return Paths.get(pathFromComment);
        }

        // Détermination automatique selon le type
        return determinePathByType(block, fileType);
    }

    /**
     * Extrait le chemin depuis un commentaire dans le code.
     */
    private String extractPathFromComment(String content, FileType fileType) {
        Pattern pattern = switch (fileType) {
            case JAVA -> Pattern.compile("^\\s*//\\s*Fichier:\\s*(.+)$", Pattern.MULTILINE);
            case XML, HTML -> Pattern.compile("^\\s*<!--\\s*Fichier:\\s*(.+)\\s*-->$", Pattern.MULTILINE);
            case YAML, PYTHON, BASH -> Pattern.compile("^\\s*#\\s*Fichier:\\s*(.+)$", Pattern.MULTILINE);
            case SQL -> Pattern.compile("^\\s*--\\s*Fichier:\\s*(.+)$", Pattern.MULTILINE);
            default -> null;
        };

        if (pattern == null) {
            return null;
        }

        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    /**
     * Détermine le chemin automatiquement selon le type de fichier.
     */
    private Path determinePathByType(CodeBlock block, FileType fileType) {
        String content = block.getContent();
        
        return switch (fileType) {
            case JAVA -> determineJavaPath(content);
            case XML -> determineXmlPath(content, block);
            case YAML -> determineYamlPath(content, block);
            case SQL -> determineSqlPath(content);
            case PYTHON -> Paths.get("video-worker", "worker.py");
            case BASH -> Paths.get("scripts", "script.sh");
            case MARKDOWN -> Paths.get("docs", "README.md");
            default -> null;
        };
    }

    /**
     * Détermine le chemin pour un fichier Java.
     */
    private Path determineJavaPath(String content) {
        // Extraire le package
        Pattern packagePattern = Pattern.compile("package\\s+([\\w.]+)\\s*;");
        Matcher packageMatcher = packagePattern.matcher(content);
        
        if (!packageMatcher.find()) {
            return null;
        }
        
        String packageName = packageMatcher.group(1);
        
        // Extraire le nom de la classe
        Pattern classPattern = Pattern.compile("(?:public\\s+)?(?:class|interface|enum)\\s+(\\w+)");
        Matcher classMatcher = classPattern.matcher(content);
        
        if (!classMatcher.find()) {
            return null;
        }
        
        String className = classMatcher.group(1);
        
        // Construire le chemin
        String packagePath = packageName.replace('.', '/');
        return Paths.get("nexus-video-generation/src/main/java", packagePath, className + ".java");
    }

    /**
     * Détermine le chemin pour un fichier XML.
     */
    private Path determineXmlPath(String content, CodeBlock block) {
        if (content.contains("<project") && content.contains("xmlns=\"http://maven.apache.org")) {
            return Paths.get("nexus-video-generation/pom.xml");
        }
        
        if (block.getFilename() != null) {
            return Paths.get(block.getFilename());
        }
        
        return Paths.get("config.xml");
    }

    /**
     * Détermine le chemin pour un fichier YAML.
     */
    private Path determineYamlPath(String content, CodeBlock block) {
        if (content.contains("spring:") && content.contains("application:")) {
            return Paths.get("nexus-video-generation/src/main/resources/application.yml");
        }
        
        if (content.contains("version:") && content.contains("services:")) {
            return Paths.get("docker-compose.yml");
        }
        
        if (content.contains("apiVersion:") && content.contains("kind:")) {
            // Kubernetes manifest
            String kind = extractKubernetesKind(content);
            String name = extractKubernetesName(content);
            return Paths.get("k8s/production", kind.toLowerCase() + "-" + name + ".yaml");
        }
        
        return Paths.get("config.yml");
    }

    /**
     * Détermine le chemin pour un fichier SQL.
     */
    private Path determineSqlPath(String content) {
        if (content.contains("CREATE TABLE") || content.contains("CREATE EXTENSION")) {
            return Paths.get("nexus-video-generation/src/main/resources/db/migration/V1_0__create_video_tables.sql");
        }
        
        return Paths.get("scripts.sql");
    }

    /**
     * Extrait le kind d'un manifest Kubernetes.
     */
    private String extractKubernetesKind(String content) {
        Pattern pattern = Pattern.compile("kind:\\s*(\\w+)");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "resource";
    }

    /**
     * Extrait le nom d'un manifest Kubernetes.
     */
    private String extractKubernetesName(String content) {
        Pattern pattern = Pattern.compile("name:\\s*(\\S+)");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : "unnamed";
    }

    /**
     * Valide le répertoire de sortie.
     */
    private void validateOutputPath() throws IOException {
        if (outputPath == null) {
            throw new IllegalStateException("Le chemin de sortie n'est pas défini");
        }

        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
            log.info("Répertoire de sortie créé: {}", outputPath);
        }

        if (!Files.isDirectory(outputPath)) {
            throw new IllegalArgumentException("Le chemin de sortie n'est pas un répertoire: " + outputPath);
        }
    }

    /**
     * Incrémente une statistique.
     */
    private void incrementStatistic(String key) {
        statistics.merge(key, 1, Integer::sum);
    }

    /**
     * Affiche les statistiques de génération.
     */
    private void displayStatistics() {
        log.info("═══════════════════════════════════════════════════");
        log.info("  STATISTIQUES DE GÉNÉRATION");
        log.info("═══════════════════════════════════════════════════");
        
        statistics.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> log.info("  {}: {}", entry.getKey(), entry.getValue()));
        
        log.info("═══════════════════════════════════════════════════");
    }

    /**
     * Point d'entrée principal.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ProjectStructureGenerator <input-file> <output-path>");
            System.err.println("Exemple: java ProjectStructureGenerator ./docs/module-7.md ./output");
            System.exit(1);
        }

        try {
            Path inputFile = Paths.get(args[0]);
            Path outputPath = Paths.get(args[1]);

            ProjectStructureGenerator generator = new ProjectStructureGenerator();
            generator.setOutputPath(outputPath);
            generator.setOverwriteExisting(args.length > 2 && args[2].equals("--overwrite"));
            generator.parseAndGenerate(inputFile);

            System.out.println("\n✓ Génération terminée avec succès!");
            System.out.println("  Répertoire de sortie: " + outputPath.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("✗ Erreur lors de la génération: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

// ============================================================================
// CLASSE: CodeBlock
// ============================================================================

/**
 * Représente un bloc de code extrait de la documentation.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class CodeBlock {
    private String language;
    private String content;
    private String filename;
    private int lineNumber;
    private Map<String, String> metadata;
    
    public String getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
}

// ============================================================================
// ENUM: FileType
// ============================================================================

/**
 * Types de fichiers supportés.
 */
enum FileType {
    JAVA("java"),
    XML("xml"),
    YAML("yaml", "yml"),
    SQL("sql"),
    PYTHON("python", "py"),
    JAVASCRIPT("javascript", "js"),
    TYPESCRIPT("typescript", "ts"),
    BASH("bash", "sh"),
    MARKDOWN("markdown", "md"),
    JSON("json"),
    HTML("html"),
    CSS("css"),
    PROPERTIES("properties"),
    DOCKERFILE("dockerfile"),
    UNKNOWN("unknown");

    private final List<String> extensions;

    FileType(String... extensions) {
        this.extensions = Arrays.asList(extensions);
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public String getPrimaryExtension() {
        return extensions.isEmpty() ? "" : extensions.get(0);
    }

    public static FileType fromExtension(String extension) {
        if (extension == null) {
            return UNKNOWN;
        }
        
        String ext = extension.toLowerCase().trim();
        
        for (FileType type : values()) {
            if (type.getExtensions().contains(ext)) {
                return type;
            }
        }
        
        return UNKNOWN;
    }
}
