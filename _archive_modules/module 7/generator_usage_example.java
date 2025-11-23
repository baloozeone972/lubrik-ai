package com.nexusai.tools;

import java.nio.file.*;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Classe principale avec exemples d'utilisation du générateur.
 * 
 * @author NexusAI Team
 * @version 1.0
 */
@Slf4j
public class GeneratorUsageExample {

    /**
     * Exemple 1: Génération depuis un seul fichier.
     */
    public static void example1_SingleFile() throws Exception {
        log.info("=== Exemple 1: Génération depuis un fichier unique ===");

        ProjectStructureGenerator generator = new ProjectStructureGenerator();
        generator.setOutputPath(Paths.get("./output"));
        generator.setOverwriteExisting(false);
        generator.setCreateBackup(true);

        // Parser le fichier
        generator.parseAndGenerate(Paths.get("./docs/module-7-complete.md"));

        log.info("Génération terminée!");
    }

    /**
     * Exemple 2: Génération depuis plusieurs fichiers.
     */
    public static void example2_MultipleFiles() throws Exception {
        log.info("=== Exemple 2: Génération depuis plusieurs fichiers ===");

        List<Path> inputFiles = Arrays.asList(
            Paths.get("./docs/module-7-pom.md"),
            Paths.get("./docs/module-7-entities.md"),
            Paths.get("./docs/module-7-services.md"),
            Paths.get("./docs/module-7-controllers.md")
        );

        ProjectStructureGenerator generator = new ProjectStructureGenerator();
        generator.setOutputPath(Paths.get("./output"));
        generator.parseAndGenerate(inputFiles);
    }

    /**
     * Exemple 3: Génération avec overwrite et sans backup.
     */
    public static void example3_OverwriteNoBackup() throws Exception {
        log.info("=== Exemple 3: Overwrite sans backup ===");

        ProjectStructureGenerator generator = new ProjectStructureGenerator();
        generator.setOutputPath(Paths.get("./output"));
        generator.setOverwriteExisting(true);
        generator.setCreateBackup(false);

        generator.parseAndGenerate(Paths.get("./docs/module-7.md"));
    }

    /**
     * Exemple 4: Création de la structure vide puis génération.
     */
    public static void example4_CreateStructureThenGenerate() throws Exception {
        log.info("=== Exemple 4: Création structure + génération ===");

        Path outputPath = Paths.get("./nexusai-project");

        // Créer la structure de base
        FileStructureWriter writer = new FileStructureWriter();
        writer.createProjectStructure(outputPath);

        // Générer les fichiers
        ProjectStructureGenerator generator = new ProjectStructureGenerator();
        generator.setOutputPath(outputPath);
        generator.parseAndGenerate(Paths.get("./docs/module-7.md"));
    }

    /**
     * Exemple 5: Parser un texte directement (sans fichier).
     */
    public static void example5_ParseText() throws Exception {
        log.info("=== Exemple 5: Parser du texte directement ===");

        String markdownContent = """
            # Documentation Module Test
            
            Voici un exemple de classe Java:
            
            ```java
            // Fichier: nexus-video-generation/src/main/java/com/nexusai/Test.java
            package com.nexusai;
            
            public class Test {
                public static void main(String[] args) {
                    System.out.println("Hello World!");
                }
            }
            ```
            
            Et un fichier YAML:
            
            ```yaml
            # Fichier: config/application.yml
            server:
              port: 8080
            spring:
              application:
                name: test-app
            ```
            """;

        // Créer un fichier temporaire
        Path tempFile = Files.createTempFile("temp-doc", ".md");
        Files.writeString(tempFile, markdownContent);

        // Générer
        ProjectStructureGenerator generator = new ProjectStructureGenerator();
        generator.setOutputPath(Paths.get("./output-test"));
        generator.parseAndGenerate(tempFile);

        // Nettoyer
        Files.deleteIfExists(tempFile);
    }

    /**
     * Point d'entrée principal avec menu interactif.
     */
    public static void main(String[] args) {
        try {
            if (args.length > 0 && args[0].equals("--example")) {
                int exampleNumber = args.length > 1 ? Integer.parseInt(args[1]) : 1;
                
                switch (exampleNumber) {
                    case 1 -> example1_SingleFile();
                    case 2 -> example2_MultipleFiles();
                    case 3 -> example3_OverwriteNoBackup();
                    case 4 -> example4_CreateStructureThenGenerate();
                    case 5 -> example5_ParseText();
                    default -> {
                        System.err.println("Exemple invalide: " + exampleNumber);
                        System.exit(1);
                    }
                }
            } else {
                // Mode normal
                ProjectStructureGenerator.main(args);
            }

        } catch (Exception e) {
            log.error("Erreur lors de l'exécution", e);
            System.exit(1);
        }
    }
}

// ============================================================================
// TESTS UNITAIRES
// ============================================================================

/**
 * Tests unitaires pour le générateur.
 */
class ProjectStructureGeneratorTest {

    @org.junit.jupiter.api.Test
    void testParseMarkdownCodeBlock() {
        String content = """
            ```java
            package com.test;
            public class Hello {}
            ```
            """;

        CodeBlockParser parser = new CodeBlockParser();
        List<CodeBlock> blocks = parser.extractCodeBlocks(content);

        assert blocks.size() == 1;
        assert blocks.get(0).getLanguage().equals("java");
        assert blocks.get(0).getContent().contains("public class Hello");
    }

    @org.junit.jupiter.api.Test
    void testDetectJavaFileType() {
        CodeBlock block = CodeBlock.builder()
            .language("java")
            .content("package com.test;\npublic class Test {}")
            .build();

        FileTypeDetector detector = new FileTypeDetector();
        FileType type = detector.detectFileType(block);

        assert type == FileType.JAVA;
    }

    @org.junit.jupiter.api.Test
    void testDetectYamlFromContent() {
        CodeBlock block = CodeBlock.builder()
            .content("spring:\n  application:\n    name: test")
            .build();

        FileTypeDetector detector = new FileTypeDetector();
        FileType type = detector.detectFileType(block);

        assert type == FileType.YAML;
    }

    @org.junit.jupiter.api.Test
    void testExtractPackageFromJavaCode() {
        String code = """
            package com.nexusai.video.service;
            
            import java.util.*;
            
            public class VideoService {
                // ...
            }
            """;

        CodeBlockParser parser = new CodeBlockParser();
        Map<String, String> metadata = parser.extractMetadata(code);

        assert metadata.get("package").equals("com.nexusai.video.service");
        assert metadata.get("className").equals("VideoService");
    }
}
