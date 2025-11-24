package com.nexusai.tools;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Parser de blocs de code depuis des fichiers Markdown ou documentation.
 * 
 * Supporte plusieurs formats:
 * - Markdown code blocks (```language)
 * - XML/HTML comments avec balises
 * - Sections délimitées par des commentaires
 * 
 * @author NexusAI Team
 * @version 1.0
 */
@Slf4j
public class CodeBlockParser {

    private static final Pattern MARKDOWN_CODE_BLOCK = Pattern.compile(
        "```(\\w+)\\s*(?://\\s*Fichier:\\s*(.+?))?\\s*\\n(.*?)```",
        Pattern.DOTALL
    );

    private static final Pattern XML_COMMENT_FILE = Pattern.compile(
        "<!--\\s*Fichier:\\s*(.+?)\\s*-->\\s*\\n(.*?)(?=<!--\\s*Fichier:|\\z)",
        Pattern.DOTALL
    );

    private static final Pattern HASH_COMMENT_FILE = Pattern.compile(
        "#\\s*Fichier:\\s*(.+?)\\s*\\n(.*?)(?=#\\s*Fichier:|\\z)",
        Pattern.DOTALL
    );

    /**
     * Extrait tous les blocs de code d'un contenu.
     * 
     * @param content Contenu à parser
     * @return Liste des blocs de code trouvés
     */
    public List<CodeBlock> extractCodeBlocks(String content) {
        List<CodeBlock> blocks = new ArrayList<>();

        // 1. Extraire les blocs Markdown
        blocks.addAll(extractMarkdownBlocks(content));

        // 2. Extraire les sections avec commentaires XML
        blocks.addAll(extractXmlCommentBlocks(content));

        // 3. Extraire les sections avec commentaires hash (#)
        blocks.addAll(extractHashCommentBlocks(content));

        log.debug("Blocs extraits: {}", blocks.size());
        return blocks;
    }

    /**
     * Extrait les blocs de code Markdown (```language).
     */
    private List<CodeBlock> extractMarkdownBlocks(String content) {
        List<CodeBlock> blocks = new ArrayList<>();
        Matcher matcher = MARKDOWN_CODE_BLOCK.matcher(content);

        int lineNumber = 1;
        while (matcher.find()) {
            String language = matcher.group(1);
            String filename = matcher.group(2); // Peut être null
            String code = matcher.group(3).trim();

            // Calculer le numéro de ligne
            String beforeMatch = content.substring(0, matcher.start());
            lineNumber = (int) beforeMatch.chars().filter(ch -> ch == '\n').count() + 1;

            CodeBlock block = CodeBlock.builder()
                .language(language)
                .content(code)
                .filename(filename)
                .lineNumber(lineNumber)
                .metadata(extractMetadata(code))
                .build();

            blocks.add(block);
        }

        return blocks;
    }

    /**
     * Extrait les blocs avec commentaires XML/HTML.
     */
    private List<CodeBlock> extractXmlCommentBlocks(String content) {
        List<CodeBlock> blocks = new ArrayList<>();
        Matcher matcher = XML_COMMENT_FILE.matcher(content);

        while (matcher.find()) {
            String filename = matcher.group(1).trim();
            String code = matcher.group(2).trim();

            // Déterminer le langage depuis l'extension
            String language = detectLanguageFromFilename(filename);

            CodeBlock block = CodeBlock.builder()
                .language(language)
                .content(code)
                .filename(filename)
                .lineNumber(0)
                .build();

            blocks.add(block);
        }

        return blocks;
    }

    /**
     * Extrait les blocs avec commentaires hash (#).
     */
    private List<CodeBlock> extractHashCommentBlocks(String content) {
        List<CodeBlock> blocks = new ArrayList<>();
        Matcher matcher = HASH_COMMENT_FILE.matcher(content);

        while (matcher.find()) {
            String filename = matcher.group(1).trim();
            String code = matcher.group(2).trim();

            String language = detectLanguageFromFilename(filename);

            CodeBlock block = CodeBlock.builder()
                .language(language)
                .content(code)
                .filename(filename)
                .lineNumber(0)
                .build();

            blocks.add(block);
        }

        return blocks;
    }

    /**
     * Détecte le langage depuis le nom de fichier.
     */
    private String detectLanguageFromFilename(String filename) {
        if (filename == null) {
            return "text";
        }

        String lower = filename.toLowerCase();
        
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".xml")) return "xml";
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return "yaml";
        if (lower.endsWith(".sql")) return "sql";
        if (lower.endsWith(".py")) return "python";
        if (lower.endsWith(".js")) return "javascript";
        if (lower.endsWith(".ts")) return "typescript";
        if (lower.endsWith(".sh")) return "bash";
        if (lower.endsWith(".md")) return "markdown";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".properties")) return "properties";
        if (lower.contains("dockerfile")) return "dockerfile";
        
        return "text";
    }

    /**
     * Extrait les métadonnées d'un bloc de code.
     */
    private Map<String, String> extractMetadata(String content) {
        Map<String, String> metadata = new HashMap<>();

        // Extraire le package Java
        Pattern packagePattern = Pattern.compile("package\\s+([\\w.]+)\\s*;");
        Matcher packageMatcher = packagePattern.matcher(content);
        if (packageMatcher.find()) {
            metadata.put("package", packageMatcher.group(1));
        }

        // Extraire le nom de classe Java
        Pattern classPattern = Pattern.compile("(?:public\\s+)?(?:class|interface|enum)\\s+(\\w+)");
        Matcher classMatcher = classPattern.matcher(content);
        if (classMatcher.find()) {
            metadata.put("className", classMatcher.group(1));
        }

        // Extraire les imports
        Pattern importPattern = Pattern.compile("import\\s+([\\w.]+)\\s*;");
        Matcher importMatcher = importPattern.matcher(content);
        int importCount = 0;
        while (importMatcher.find()) {
            importCount++;
        }
        if (importCount > 0) {
            metadata.put("importCount", String.valueOf(importCount));
        }

        return metadata;
    }
}

// ============================================================================
// CLASSE: FileTypeDetector
// ============================================================================

/**
 * Détecteur de type de fichier basé sur le contenu et le langage.
 * 
 * @author NexusAI Team
 * @version 1.0
 */
@Slf4j
public class FileTypeDetector {

    /**
     * Détecte le type de fichier d'un bloc de code.
     * 
     * @param block Bloc de code à analyser
     * @return Type de fichier détecté
     */
    public FileType detectFileType(CodeBlock block) {
        // 1. Détecter depuis le langage explicite
        if (block.getLanguage() != null) {
            FileType fromLanguage = FileType.fromExtension(block.getLanguage());
            if (fromLanguage != FileType.UNKNOWN) {
                return fromLanguage;
            }
        }

        // 2. Détecter depuis le nom de fichier
        if (block.getFilename() != null) {
            String extension = getFileExtension(block.getFilename());
            FileType fromExtension = FileType.fromExtension(extension);
            if (fromExtension != FileType.UNKNOWN) {
                return fromExtension;
            }
        }

        // 3. Détecter depuis le contenu
        return detectFromContent(block.getContent());
    }

    /**
     * Détecte le type depuis le contenu du fichier.
     */
    private FileType detectFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return FileType.UNKNOWN;
        }

        String trimmed = content.trim();

        // Java
        if (trimmed.contains("package ") && trimmed.contains("class ")) {
            return FileType.JAVA;
        }

        // XML/POM
        if (trimmed.startsWith("<?xml") || trimmed.startsWith("<project")) {
            return FileType.XML;
        }

        // YAML
        if (trimmed.matches("^\\w+:\\s*.*") && !trimmed.contains("{")) {
            return FileType.YAML;
        }

        // SQL
        if (trimmed.toUpperCase().contains("CREATE TABLE") || 
            trimmed.toUpperCase().contains("SELECT ") ||
            trimmed.toUpperCase().contains("INSERT INTO")) {
            return FileType.SQL;
        }

        // Python
        if (trimmed.contains("def ") && trimmed.contains("import ")) {
            return FileType.PYTHON;
        }

        // Bash
        if (trimmed.startsWith("#!/bin/bash") || trimmed.startsWith("#!/bin/sh")) {
            return FileType.BASH;
        }

        // Dockerfile
        if (trimmed.startsWith("FROM ") && trimmed.contains("RUN ")) {
            return FileType.DOCKERFILE;
        }

        // JSON
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return FileType.JSON;
        }

        // Markdown
        if (trimmed.contains("# ") && trimmed.contains("## ")) {
            return FileType.MARKDOWN;
        }

        return FileType.UNKNOWN;
    }

    /**
     * Extrait l'extension d'un nom de fichier.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        
        int lastDot = filename.lastIndexOf('.');
        return filename.substring(lastDot + 1).toLowerCase();
    }
}

// ============================================================================
// CLASSE: FileStructureWriter
// ============================================================================

/**
 * Écrivain de fichiers avec gestion de l'arborescence.
 * 
 * Fonctionnalités:
 * - Création automatique des répertoires
 * - Backup des fichiers existants
 * - Gestion de l'overwrite
 * - Permissions et encodage
 * 
 * @author NexusAI Team
 * @version 1.0
 */
@Slf4j
public class FileStructureWriter {

    private static final DateTimeFormatter BACKUP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Écrit un fichier dans l'arborescence.
     * 
     * @param basePath Chemin de base du projet
     * @param relativePath Chemin relatif du fichier
     * @param content Contenu à écrire
     * @param overwrite Autoriser l'overwrite
     * @param createBackup Créer un backup si le fichier existe
     * @throws IOException Si erreur d'écriture
     */
    public void writeFile(
            Path basePath, 
            Path relativePath, 
            String content, 
            boolean overwrite,
            boolean createBackup) throws IOException {

        // Construire le chemin complet
        Path fullPath = basePath.resolve(relativePath);

        // Créer les répertoires parents si nécessaire
        Path parentDir = fullPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            log.debug("Répertoire créé: {}", parentDir);
        }

        // Vérifier si le fichier existe
        if (Files.exists(fullPath)) {
            if (!overwrite) {
                log.warn("Fichier existe déjà (non écrasé): {}", fullPath);
                return;
            }

            if (createBackup) {
                createBackupFile(fullPath);
            }
        }

        // Écrire le fichier
        Files.writeString(fullPath, content, StandardOpenOption.CREATE, 
                         StandardOpenOption.TRUNCATE_EXISTING);

        // Définir les permissions (Unix/Linux)
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            setFilePermissions(fullPath);
        }

        log.info("Fichier écrit: {}", fullPath);
    }

    /**
     * Crée un backup d'un fichier existant.
     */
    private void createBackupFile(Path originalFile) throws IOException {
        String timestamp = LocalDateTime.now().format(BACKUP_FORMATTER);
        String filename = originalFile.getFileName().toString();
        String backupFilename = filename + ".backup_" + timestamp;
        
        Path backupPath = originalFile.getParent().resolve(backupFilename);
        
        Files.copy(originalFile, backupPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Backup créé: {}", backupPath);
    }

    /**
     * Définit les permissions d'un fichier selon son type.
     */
    private void setFilePermissions(Path file) throws IOException {
        String filename = file.getFileName().toString();
        
        if (filename.endsWith(".sh") || filename.endsWith(".py")) {
            // Scripts exécutables: rwxr-xr-x (755)
            Files.setPosixFilePermissions(file, 
                PosixFilePermissions.fromString("rwxr-xr-x"));
        } else {
            // Fichiers normaux: rw-r--r-- (644)
            Files.setPosixFilePermissions(file, 
                PosixFilePermissions.fromString("rw-r--r--"));
        }
    }

    /**
     * Crée toute l'arborescence de répertoires d'un projet type.
     */
    public void createProjectStructure(Path basePath) throws IOException {
        List<String> directories = Arrays.asList(
            "nexus-video-generation/src/main/java/com/nexusai/video",
            "nexus-video-generation/src/main/java/com/nexusai/video/controller",
            "nexus-video-generation/src/main/java/com/nexusai/video/service",
            "nexus-video-generation/src/main/java/com/nexusai/video/repository",
            "nexus-video-generation/src/main/java/com/nexusai/video/domain/entity",
            "nexus-video-generation/src/main/java/com/nexusai/video/dto",
            "nexus-video-generation/src/main/java/com/nexusai/video/messaging",
            "nexus-video-generation/src/main/java/com/nexusai/video/config",
            "nexus-video-generation/src/main/java/com/nexusai/video/exception",
            "nexus-video-generation/src/main/resources",
            "nexus-video-generation/src/main/resources/db/migration",
            "nexus-video-generation/src/test/java/com/nexusai/video",
            "video-worker",
            "video-worker/tests",
            "k8s/production",
            "k8s/staging",
            "k8s/development",
            "scripts",
            "docs",
            "monitoring",
            "logs"
        );

        for (String dir : directories) {
            Path dirPath = basePath.resolve(dir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                log.debug("Répertoire créé: {}", dirPath);
            }
        }

        log.info("Structure de répertoires créée dans: {}", basePath);
    }
}
