package com.nexusai.generator;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Parser et générateur d'arborescence pour le module NexusAI.
 * 
 * Cette classe analyse un document markdown contenant la structure
 * complète d'un projet et génère automatiquement tous les fichiers
 * et dossiers dans l'arborescence spécifiée.
 * 
 * @author NexusAI Team
 * @version 1.0
 */
public class NexusAIModuleParser {
    
    private static final Pattern PATH_PATTERN = Pattern.compile("\\*\\*Chemin:\\*\\*\\s*`(.*?)`");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(\\w+)\\n(.*?)\\n```", Pattern.DOTALL);
    private static final String DEFAULT_OUTPUT_DIR = "./nexus-ai-generated";
    
    private final String outputDirectory;
    private final Map<String, FileInfo> extractedFiles;
    private int filesCreated = 0;
    private int directoriesCreated = 0;
    
    /**
     * Information sur un fichier à créer.
     */
    private static class FileInfo {
        String path;
        String content;
        String language;
        
        FileInfo(String path, String content, String language) {
            this.path = path;
            this.content = content;
            this.language = language;
        }
    }
    
    /**
     * Constructeur avec chemin de sortie personnalisé.
     * 
     * @param outputDirectory Chemin du répertoire de sortie
     */
    public NexusAIModuleParser(String outputDirectory) {
        this.outputDirectory = outputDirectory != null ? outputDirectory : DEFAULT_OUTPUT_DIR;
        this.extractedFiles = new LinkedHashMap<>();
    }
    
    /**
     * Constructeur par défaut (utilise le répertoire par défaut).
     */
    public NexusAIModuleParser() {
        this(DEFAULT_OUTPUT_DIR);
    }
    
    /**
     * Point d'entrée principal pour générer l'arborescence.
     * 
     * @param markdownFilePath Chemin vers le fichier markdown source
     * @throws IOException En cas d'erreur de lecture/écriture
     */
    public void generateProjectStructure(String markdownFilePath) throws IOException {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║     NEXUSAI MODULE GENERATOR - Début de génération        ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        // 1. Lire le fichier markdown
        String markdownContent = readFile(markdownFilePath);
        System.out.println("✓ Fichier markdown lu : " + markdownFilePath);
        
        // 2. Parser et extraire les fichiers
        parseMarkdown(markdownContent);
        System.out.println("✓ Fichiers extraits : " + extractedFiles.size() + " fichiers détectés\n");
        
        // 3. Créer l'arborescence
        createDirectoryStructure();
        System.out.println("✓ Arborescence créée : " + directoriesCreated + " répertoires\n");
        
        // 4. Écrire tous les fichiers
        writeAllFiles();
        System.out.println("✓ Fichiers écrits : " + filesCreated + " fichiers créés\n");
        
        // 5. Afficher le résumé
        printSummary();
    }
    
    /**
     * Lit le contenu d'un fichier.
     */
    private String readFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
    
    /**
     * Parse le markdown et extrait tous les fichiers.
     */
    private void parseMarkdown(String content) {
        String[] sections = content.split("###");
        
        for (String section : sections) {
            // Chercher le chemin du fichier
            Matcher pathMatcher = PATH_PATTERN.matcher(section);
            if (!pathMatcher.find()) {
                continue;
            }
            
            String filePath = pathMatcher.group(1).trim();
            
            // Chercher le bloc de code correspondant
            Matcher codeMatcher = CODE_BLOCK_PATTERN.matcher(section);
            if (codeMatcher.find()) {
                String language = codeMatcher.group(1);
                String code = codeMatcher.group(2);
                
                // Nettoyer le chemin
                filePath = cleanPath(filePath);
                
                // Stocker les informations du fichier
                extractedFiles.put(filePath, new FileInfo(filePath, code, language));
            }
        }
    }
    
    /**
     * Nettoie le chemin du fichier.
     */
    private String cleanPath(String path) {
        // Supprimer les préfixes comme "./"
        path = path.replaceFirst("^\\./", "");
        return path;
    }
    
    /**
     * Crée toute l'arborescence de répertoires.
     */
    private void createDirectoryStructure() throws IOException {
        Set<String> directories = new HashSet<>();
        
        // Extraire tous les répertoires uniques
        for (String filePath : extractedFiles.keySet()) {
            Path path = Paths.get(outputDirectory, filePath);
            Path parent = path.getParent();
            
            if (parent != null) {
                directories.add(parent.toString());
            }
        }
        
        // Créer les répertoires
        for (String dir : directories) {
            Path dirPath = Paths.get(dir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                directoriesCreated++;
                System.out.println("  📁 " + dirPath);
            }
        }
    }
    
    /**
     * Écrit tous les fichiers extraits.
     */
    private void writeAllFiles() throws IOException {
        System.out.println("Écriture des fichiers :");
        System.out.println("─────────────────────────────────────────────────────────────");
        
        for (Map.Entry<String, FileInfo> entry : extractedFiles.entrySet()) {
            FileInfo fileInfo = entry.getValue();
            Path targetPath = Paths.get(outputDirectory, fileInfo.path);
            
            // Écrire le fichier
            Files.write(targetPath, fileInfo.content.getBytes());
            filesCreated++;
            
            // Afficher avec icône selon le type
            String icon = getFileIcon(fileInfo.language);
            System.out.println("  " + icon + " " + fileInfo.path);
        }
    }
    
    /**
     * Retourne une icône selon le type de fichier.
     */
    private String getFileIcon(String language) {
        return switch (language.toLowerCase()) {
            case "java" -> "☕";
            case "xml" -> "📋";
            case "yaml", "yml" -> "⚙️";
            case "markdown", "md" -> "📝";
            case "properties" -> "🔧";
            default -> "📄";
        };
    }
    
    /**
     * Affiche le résumé de la génération.
     */
    private void printSummary() {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    RÉSUMÉ DE GÉNÉRATION                    ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.printf("║  📁 Répertoires créés    : %-30d ║%n", directoriesCreated);
        System.out.printf("║  📄 Fichiers créés       : %-30d ║%n", filesCreated);
        System.out.printf("║  📍 Répertoire de sortie : %-30s ║%n", 
                         outputDirectory.substring(0, Math.min(30, outputDirectory.length())));
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║               ✓ GÉNÉRATION TERMINÉE AVEC SUCCÈS             ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        // Statistiques par type de fichier
        printFileStatistics();
        
        // Prochaines étapes
        printNextSteps();
    }
    
    /**
     * Affiche les statistiques par type de fichier.
     */
    private void printFileStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        
        for (FileInfo info : extractedFiles.values()) {
            stats.merge(info.language, 1, Integer::sum);
        }
        
        System.out.println("📊 Statistiques par type de fichier :");
        System.out.println("─────────────────────────────────────────────────────────────");
        
        stats.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> {
                String icon = getFileIcon(entry.getKey());
                System.out.printf("  %s %-15s : %3d fichier(s)%n", 
                                icon, entry.getKey(), entry.getValue());
            });
        
        System.out.println();
    }
    
    /**
     * Affiche les prochaines étapes.
     */
    private void printNextSteps() {
        System.out.println("🚀 PROCHAINES ÉTAPES :");
        System.out.println("─────────────────────────────────────────────────────────────");
        System.out.println("  1. cd " + outputDirectory);
        System.out.println("  2. docker-compose up -d");
        System.out.println("  3. mvn clean install");
        System.out.println("  4. cd nexus-auth && mvn spring-boot:run");
        System.out.println("  5. Ouvrir http://localhost:8081/swagger-ui.html");
        System.out.println();
    }
    
    /**
     * Génère également un fichier de structure.
     */
    public void generateTreeStructure() throws IOException {
        Path treePath = Paths.get(outputDirectory, "PROJECT_STRUCTURE.txt");
        
        StringBuilder tree = new StringBuilder();
        tree.append("NEXUSAI PROJECT STRUCTURE\n");
        tree.append("═════════════════════════════════════════\n\n");
        
        // Construire l'arbre
        Map<String, List<String>> directoryTree = buildDirectoryTree();
        
        for (Map.Entry<String, List<String>> entry : directoryTree.entrySet()) {
            tree.append("📁 ").append(entry.getKey()).append("\n");
            
            List<String> files = entry.getValue();
            for (int i = 0; i < files.size(); i++) {
                boolean isLast = (i == files.size() - 1);
                String prefix = isLast ? "└── " : "├── ";
                tree.append("   ").append(prefix).append(files.get(i)).append("\n");
            }
            tree.append("\n");
        }
        
        Files.write(treePath, tree.toString().getBytes());
        System.out.println("✓ Structure de l'arbre générée : " + treePath);
    }
    
    /**
     * Construit un arbre de répertoires.
     */
    private Map<String, List<String>> buildDirectoryTree() {
        Map<String, List<String>> tree = new TreeMap<>();
        
        for (String filePath : extractedFiles.keySet()) {
            Path path = Paths.get(filePath);
            String directory = path.getParent() != null ? 
                              path.getParent().toString() : ".";
            String fileName = path.getFileName().toString();
            
            tree.computeIfAbsent(directory, k -> new ArrayList<>()).add(fileName);
        }
        
        return tree;
    }
    
    /**
     * Valide la structure générée.
     */
    public ValidationResult validateStructure() {
        ValidationResult result = new ValidationResult();
        
        System.out.println("\n🔍 VALIDATION DE LA STRUCTURE :");
        System.out.println("─────────────────────────────────────────────────────────────");
        
        // Vérifier les fichiers critiques
        String[] criticalFiles = {
            "pom.xml",
            "nexus-core/pom.xml",
            "nexus-auth/pom.xml",
            "nexus-auth/src/main/java/com/nexusai/auth/NexusAuthApplication.java",
            "docker-compose.yml",
            "README.md"
        };
        
        for (String criticalFile : criticalFiles) {
            Path filePath = Paths.get(outputDirectory, criticalFile);
            boolean exists = Files.exists(filePath);
            result.addCheck(criticalFile, exists);
            
            String status = exists ? "✓" : "✗";
            System.out.println("  " + status + " " + criticalFile);
        }
        
        System.out.println("\n  Résultat : " + result.passedChecks + "/" + result.totalChecks + " validé(s)\n");
        
        return result;
    }
    
    /**
     * Résultat de validation.
     */
    public static class ValidationResult {
        int totalChecks = 0;
        int passedChecks = 0;
        List<String> failedFiles = new ArrayList<>();
        
        void addCheck(String file, boolean passed) {
            totalChecks++;
            if (passed) {
                passedChecks++;
            } else {
                failedFiles.add(file);
            }
        }
        
        public boolean isValid() {
            return passedChecks == totalChecks;
        }
    }
    
    /**
     * Affiche l'aide.
     */
    private static void printHelp() {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║        NEXUSAI MODULE GENERATOR - Guide d'utilisation     ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        System.out.println("UTILISATION :");
        System.out.println("  java NexusAIModuleParser [options] <fichier-markdown> [répertoire-sortie]");
        System.out.println();
        System.out.println("ARGUMENTS :");
        System.out.println("  <fichier-markdown>     Chemin vers le fichier .md source (requis)");
        System.out.println("  [répertoire-sortie]    Répertoire de sortie (défaut: ./nexus-ai-generated)");
        System.out.println();
        System.out.println("OPTIONS :");
        System.out.println("  -h, --help             Affiche cette aide");
        System.out.println("  -v, --validate         Valide uniquement sans générer");
        System.out.println("  --dry-run              Simule la génération sans créer les fichiers");
        System.out.println("  --tree                 Génère uniquement l'arbre de structure");
        System.out.println();
        System.out.println("EXEMPLES :");
        System.out.println("  java NexusAIModuleParser nexusai-module.md");
        System.out.println("  java NexusAIModuleParser nexusai-module.md ./output");
        System.out.println("  java NexusAIModuleParser --tree nexusai-module.md");
        System.out.println("  java NexusAIModuleParser --dry-run nexusai-module.md");
        System.out.println();
    }
    
    /**
     * Mode dry-run (simulation).
     */
    public void dryRun(String markdownFilePath) throws IOException {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║     MODE DRY-RUN - Simulation sans création de fichiers   ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        String markdownContent = readFile(markdownFilePath);
        parseMarkdown(markdownContent);
        
        System.out.println("✓ Analyse terminée : " + extractedFiles.size() + " fichiers détectés\n");
        
        System.out.println("📋 APERÇU DE LA STRUCTURE À CRÉER :");
        System.out.println("─────────────────────────────────────────────────────────────");
        
        Map<String, List<String>> tree = buildDirectoryTree();
        
        for (Map.Entry<String, List<String>> entry : tree.entrySet()) {
            System.out.println("\n📁 " + entry.getKey() + "/");
            for (String file : entry.getValue()) {
                FileInfo info = extractedFiles.get(entry.getKey() + "/" + file);
                if (info == null) {
                    info = extractedFiles.get(file);
                }
                String icon = info != null ? getFileIcon(info.language) : "📄";
                long lines = info != null ? info.content.split("\n").length : 0;
                System.out.printf("   %s %-40s (%d lignes)%n", icon, file, lines);
            }
        }
        
        System.out.println("\n📊 STATISTIQUES :");
        System.out.println("─────────────────────────────────────────────────────────────");
        System.out.println("  Répertoires à créer : " + tree.size());
        System.out.println("  Fichiers à créer    : " + extractedFiles.size());
        
        long totalLines = extractedFiles.values().stream()
            .mapToLong(f -> f.content.split("\n").length)
            .sum();
        System.out.println("  Lignes de code      : " + totalLines);
        System.out.println();
    }
    
    /**
     * Copie un fichier template.
     */
    public void copyTemplate(String templateName, String destination) throws IOException {
        // Templates prédéfinis
        Map<String, String> templates = new HashMap<>();
        
        templates.put("gitignore", 
            "# Compiled class file\n*.class\n\n" +
            "# Log files\n*.log\n\n" +
            "# Package Files\n*.jar\n*.war\n*.ear\n\n" +
            "# Maven\ntarget/\npom.xml.tag\n\n" +
            "# IDE\n.idea/\n*.iml\n.vscode/\n\n" +
            "# OS\n.DS_Store\nThumbs.db\n");
        
        templates.put("editorconfig",
            "root = true\n\n" +
            "[*]\ncharset = utf-8\nindent_style = space\nindent_size = 4\n" +
            "end_of_line = lf\ninsert_final_newline = true\ntrim_trailing_whitespace = true\n\n" +
            "[*.{yml,yaml}]\nindent_size = 2\n\n" +
            "[*.md]\ntrim_trailing_whitespace = false\n");
        
        if (templates.containsKey(templateName)) {
            Path targetPath = Paths.get(outputDirectory, destination);
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, templates.get(templateName).getBytes());
            System.out.println("✓ Template copié : " + destination);
        } else {
            System.err.println("⚠ Template non trouvé : " + templateName);
        }
    }
    
    /**
     * Génère les fichiers auxiliaires (.gitignore, .editorconfig, etc.)
     */
    public void generateAuxiliaryFiles() throws IOException {
        System.out.println("\n📝 Génération des fichiers auxiliaires :");
        System.out.println("─────────────────────────────────────────────────────────────");
        
        copyTemplate("gitignore", ".gitignore");
        copyTemplate("editorconfig", ".editorconfig");
        
        // Créer un script de démarrage
        String startScript = "#!/bin/bash\n\n" +
            "echo \"🚀 Démarrage de NexusAI...\"\n\n" +
            "# Démarrer les services Docker\n" +
            "echo \"📦 Démarrage PostgreSQL et Redis...\"\n" +
            "docker-compose up -d\n\n" +
            "# Attendre que les services soient prêts\n" +
            "echo \"⏳ Attente des services...\"\n" +
            "sleep 5\n\n" +
            "# Compiler le projet\n" +
            "echo \"🔨 Compilation du projet...\"\n" +
            "mvn clean install\n\n" +
            "# Démarrer l'application\n" +
            "echo \"🌟 Démarrage de l'application...\"\n" +
            "cd nexus-auth\n" +
            "mvn spring-boot:run\n";
        
        Path scriptPath = Paths.get(outputDirectory, "start.sh");
        Files.write(scriptPath, startScript.getBytes());
        
        // Rendre le script exécutable sur Unix
        try {
            scriptPath.toFile().setExecutable(true);
        } catch (Exception e) {
            // Ignorer sur Windows
        }
        
        System.out.println("✓ Script de démarrage créé : start.sh");
        System.out.println();
    }
    
    /**
     * Méthode principale avec gestion CLI complète.
     */
    public static void main(String[] args) {
        try {
            // Vérifier les options
            boolean validateOnly = false;
            boolean dryRun = false;
            boolean treeOnly = false;
            String inputFile = null;
            String outputDir = DEFAULT_OUTPUT_DIR;
            
            // Parser les arguments
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                
                switch (arg) {
                    case "-h", "--help" -> {
                        printHelp();
                        return;
                    }
                    case "-v", "--validate" -> validateOnly = true;
                    case "--dry-run" -> dryRun = true;
                    case "--tree" -> treeOnly = true;
                    default -> {
                        if (inputFile == null) {
                            inputFile = arg;
                        } else if (i == args.length - 1) {
                            outputDir = arg;
                        }
                    }
                }
            }
            
            // Vérifier le fichier d'entrée
            if (inputFile == null) {
                System.err.println("❌ ERREUR : Fichier markdown non spécifié");
                printHelp();
                System.exit(1);
            }
            
            if (!Files.exists(Paths.get(inputFile))) {
                System.err.println("❌ ERREUR : Fichier introuvable : " + inputFile);
                System.exit(1);
            }
            
            // Créer le parser
            NexusAIModuleParser parser = new NexusAIModuleParser(outputDir);
            
            // Exécuter selon le mode
            if (dryRun) {
                parser.dryRun(inputFile);
            } else if (treeOnly) {
                String content = parser.readFile(inputFile);
                parser.parseMarkdown(content);
                parser.generateTreeStructure();
            } else if (validateOnly) {
                parser.generateProjectStructure(inputFile);
                ValidationResult validation = parser.validateStructure();
                System.exit(validation.isValid() ? 0 : 1);
            } else {
                // Mode complet
                parser.generateProjectStructure(inputFile);
                parser.generateTreeStructure();
                parser.generateAuxiliaryFiles();
                
                ValidationResult validation = parser.validateStructure();
                
                if (!validation.isValid()) {
                    System.err.println("\n⚠ ATTENTION : Certains fichiers critiques sont manquants !");
                    validation.failedFiles.forEach(f -> System.err.println("  ✗ " + f));
                }
                
                System.exit(validation.isValid() ? 0 : 1);
            }
            
        } catch (IOException e) {
            System.err.println("\n❌ ERREUR : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("\n❌ ERREUR INATTENDUE : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

/* 
 * ════════════════════════════════════════════════════════════════════════
 * GUIDE D'UTILISATION RAPIDE
 * ════════════════════════════════════════════════════════════════════════
 * 
 * 1. COMPILATION :
 *    javac NexusAIModuleParser.java
 * 
 * 2. GÉNÉRATION COMPLÈTE :
 *    java NexusAIModuleParser nexusai-module.md ./mon-projet
 * 
 * 3. SIMULATION (DRY-RUN) :
 *    java NexusAIModuleParser --dry-run nexusai-module.md
 * 
 * 4. GÉNÉRATION ARBRE SEULEMENT :
 *    java NexusAIModuleParser --tree nexusai-module.md
 * 
 * 5. AVEC VALIDATION :
 *    java NexusAIModuleParser --validate nexusai-module.md ./output
 * 
 * ════════════════════════════════════════════════════════════════════════
 */
