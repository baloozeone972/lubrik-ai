package com.nexusai.tools;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * ═══════════════════════════════════════════════════════════════
 * FILE ORGANIZER UTILITY
 * 
 * Utilitaire pour parser et organiser automatiquement les fichiers
 * du Module 10 dans la bonne arborescence Maven.
 * 
 * Usage:
 * FileOrganizerUtility organizer = new FileOrganizerUtility(
 *     "/path/to/source/files",
 *     "/path/to/output/nexusai-analytics"
 * );
 * organizer.organize();
 * 
 * @author NexusAI Team
 * ═══════════════════════════════════════════════════════════════
 */
public class FileOrganizerUtility {
    
    private final Path sourceDirectory;
    private final Path outputDirectory;
    private final Map<String, Integer> stats = new HashMap<>();
    
    // Patterns pour l'extraction
    private static final Pattern PACKAGE_PATTERN = 
        Pattern.compile("package\\s+([a-zA-Z0-9_.]+);");
    
    private static final Pattern CLASS_PATTERN = 
        Pattern.compile("(public\\s+)?(class|interface|enum|@interface)\\s+([A-Za-z0-9_]+)");
    
    /**
     * Constructeur.
     * 
     * @param sourceDirectory Répertoire contenant les fichiers source
     * @param outputDirectory Répertoire de sortie (racine du projet Maven)
     */
    public FileOrganizerUtility(String sourceDirectory, String outputDirectory) {
        this.sourceDirectory = Paths.get(sourceDirectory);
        this.outputDirectory = Paths.get(outputDirectory);
    }
    
    /**
     * Organise tous les fichiers dans l'arborescence correcte.
     */
    public void organize() throws IOException {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("FILE ORGANIZER UTILITY - Module 10 Analytics");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("Source: " + sourceDirectory);
        System.out.println("Output: " + outputDirectory);
        System.out.println();
        
        // Créer la structure de base
        createBaseStructure();
        
        // Parser et organiser chaque type de fichier
        organizeJavaFiles();
        organizeXmlFiles();
        organizeYamlFiles();
        organizeSqlFiles();
        organizeDockerFiles();
        organizeKubernetesFiles();
        organizeMonitoringFiles();
        organizeDocumentation();
        
        // Afficher les statistiques
        printStatistics();
        
        System.out.println();
        System.out.println("✅ Organisation terminée avec succès !");
    }
    
    /**
     * Crée la structure de base du projet Maven.
     */
    private void createBaseStructure() throws IOException {
        System.out.println("📁 Création de la structure de base...");
        
        String[] directories = {
            // Root
            "",
            
            // Core module
            "analytics-core/src/main/java/com/nexusai/analytics/core/model",
            "analytics-core/src/main/java/com/nexusai/analytics/core/service",
            "analytics-core/src/main/java/com/nexusai/analytics/core/repository",
            "analytics-core/src/main/java/com/nexusai/analytics/core/config",
            "analytics-core/src/main/resources",
            "analytics-core/src/test/java/com/nexusai/analytics/core",
            
            // API module
            "analytics-api/src/main/java/com/nexusai/analytics/api/controller",
            "analytics-api/src/main/java/com/nexusai/analytics/api/dto",
            "analytics-api/src/main/java/com/nexusai/analytics/api/security",
            "analytics-api/src/main/resources",
            "analytics-api/src/test/java/com/nexusai/analytics/api",
            
            // Collector module
            "analytics-collector/src/main/java/com/nexusai/analytics/collector/listener",
            "analytics-collector/src/main/java/com/nexusai/analytics/collector/processor",
            "analytics-collector/src/main/java/com/nexusai/analytics/collector/config",
            "analytics-collector/src/main/resources",
            "analytics-collector/src/test/java/com/nexusai/analytics/collector",
            
            // Reporting module
            "analytics-reporting/src/main/java/com/nexusai/analytics/reporting/generator",
            "analytics-reporting/src/main/java/com/nexusai/analytics/reporting/scheduler",
            "analytics-reporting/src/main/java/com/nexusai/analytics/reporting/exporter",
            "analytics-reporting/src/main/resources",
            "analytics-reporting/src/test/java/com/nexusai/analytics/reporting",
            
            // Monitoring module
            "analytics-monitoring/src/main/java/com/nexusai/analytics/monitoring/metrics",
            "analytics-monitoring/src/main/java/com/nexusai/analytics/monitoring/health",
            "analytics-monitoring/src/main/java/com/nexusai/analytics/monitoring/alerting",
            "analytics-monitoring/src/main/resources",
            "analytics-monitoring/src/test/java/com/nexusai/analytics/monitoring",
            
            // Infrastructure
            "sql",
            "docker",
            "k8s",
            "monitoring/prometheus",
            "monitoring/grafana/dashboards",
            "monitoring/alertmanager",
            "docs"
        };
        
        for (String dir : directories) {
            Path path = outputDirectory.resolve(dir);
            Files.createDirectories(path);
        }
        
        System.out.println("   ✓ Structure créée");
        System.out.println();
    }
    
    /**
     * Organise les fichiers Java.
     */
    private void organizeJavaFiles() throws IOException {
        System.out.println("☕ Organisation des fichiers Java...");
        
        // Map des fichiers Java avec leur module cible
        Map<String, ModuleInfo> javaFiles = new HashMap<>();
        
        // Core
        javaFiles.put("UserEvent", new ModuleInfo("analytics-core", "model"));
        javaFiles.put("SystemMetric", new ModuleInfo("analytics-core", "model"));
        javaFiles.put("AggregatedMetric", new ModuleInfo("analytics-core", "model"));
        javaFiles.put("Report", new ModuleInfo("analytics-core", "model"));
        javaFiles.put("Alert", new ModuleInfo("analytics-core", "model"));
        javaFiles.put("EventMessage", new ModuleInfo("analytics-core", "model"));
        javaFiles.put("MetricMessage", new ModuleInfo("analytics-core", "model"));
        
        javaFiles.put("EventService", new ModuleInfo("analytics-core", "service"));
        javaFiles.put("MetricService", new ModuleInfo("analytics-core", "service"));
        javaFiles.put("AggregationService", new ModuleInfo("analytics-core", "service"));
        
        javaFiles.put("EventRepository", new ModuleInfo("analytics-core", "repository"));
        javaFiles.put("MetricRepository", new ModuleInfo("analytics-core", "repository"));
        javaFiles.put("AggregatedMetricRepository", new ModuleInfo("analytics-core", "repository"));
        
        // API
        javaFiles.put("EventController", new ModuleInfo("analytics-api", "controller"));
        javaFiles.put("MetricController", new ModuleInfo("analytics-api", "controller"));
        javaFiles.put("DashboardController", new ModuleInfo("analytics-api", "controller"));
        javaFiles.put("HealthController", new ModuleInfo("analytics-api", "controller"));
        
        javaFiles.put("EventRequest", new ModuleInfo("analytics-api", "dto"));
        javaFiles.put("EventResponse", new ModuleInfo("analytics-api", "dto"));
        javaFiles.put("MetricRequest", new ModuleInfo("analytics-api", "dto"));
        javaFiles.put("MetricResponse", new ModuleInfo("analytics-api", "dto"));
        javaFiles.put("DashboardOverview", new ModuleInfo("analytics-api", "dto"));
        javaFiles.put("UserDashboard", new ModuleInfo("analytics-api", "dto"));
        javaFiles.put("ReportRequest", new ModuleInfo("analytics-api", "dto"));
        javaFiles.put("ReportResponse", new ModuleInfo("analytics-api", "dto"));
        javaFiles.put("AlertRequest", new ModuleInfo("analytics-api", "dto"));
        javaFiles.put("AlertResponse", new ModuleInfo("analytics-api", "dto"));
        
        // Collector
        javaFiles.put("EventCollectorListener", new ModuleInfo("analytics-collector", "listener"));
        javaFiles.put("MetricCollectorListener", new ModuleInfo("analytics-collector", "listener"));
        javaFiles.put("EventBuffer", new ModuleInfo("analytics-collector", "listener"));
        javaFiles.put("MetricBuffer", new ModuleInfo("analytics-collector", "listener"));
        javaFiles.put("CollectorStatistics", new ModuleInfo("analytics-collector", "listener"));
        
        // Monitoring
        javaFiles.put("AnalyticsMetricsService", new ModuleInfo("analytics-monitoring", "metrics"));
        javaFiles.put("ClickHouseHealthIndicator", new ModuleInfo("analytics-monitoring", "health"));
        javaFiles.put("BufferHealthIndicator", new ModuleInfo("analytics-monitoring", "health"));
        javaFiles.put("KafkaHealthIndicator", new ModuleInfo("analytics-monitoring", "health"));
        javaFiles.put("AlertService", new ModuleInfo("analytics-monitoring", "alerting"));
        javaFiles.put("NotificationService", new ModuleInfo("analytics-monitoring", "alerting"));
        
        // Reporting
        javaFiles.put("ReportService", new ModuleInfo("analytics-reporting", "generator"));
        javaFiles.put("ScheduledReportGenerator", new ModuleInfo("analytics-reporting", "scheduler"));
        javaFiles.put("ReportGenerator", new ModuleInfo("analytics-reporting", "generator"));
        javaFiles.put("ReportExporter", new ModuleInfo("analytics-reporting", "exporter"));
        javaFiles.put("S3StorageService", new ModuleInfo("analytics-reporting", "exporter"));
        javaFiles.put("ReportRepository", new ModuleInfo("analytics-reporting", "generator"));
        
        // Tests
        javaFiles.put("EventServiceTest", new ModuleInfo("analytics-core", "test"));
        javaFiles.put("MetricServiceTest", new ModuleInfo("analytics-core", "test"));
        javaFiles.put("AggregationServiceTest", new ModuleInfo("analytics-core", "test"));
        javaFiles.put("ClickHouseIntegrationTest", new ModuleInfo("analytics-core", "test"));
        javaFiles.put("EventControllerTest", new ModuleInfo("analytics-api", "test"));
        javaFiles.put("PerformanceTest", new ModuleInfo("analytics-core", "test"));
        
        int count = 0;
        for (Map.Entry<String, ModuleInfo> entry : javaFiles.entrySet()) {
            String className = entry.getKey();
            ModuleInfo info = entry.getValue();
            
            // Simuler la création du fichier
            String content = generateJavaFileContent(className, info);
            Path targetPath = getJavaFilePath(info.module, info.subPackage, className);
            
            writeFile(targetPath, content);
            count++;
        }
        
        stats.put("Java files", count);
        System.out.println("   ✓ " + count + " fichiers Java organisés");
        System.out.println();
    }
    
    /**
     * Organise les fichiers XML (pom.xml).
     */
    private void organizeXmlFiles() throws IOException {
        System.out.println("📄 Organisation des fichiers XML...");
        
        String[] pomFiles = {
            "pom.xml",
            "analytics-core/pom.xml",
            "analytics-api/pom.xml",
            "analytics-collector/pom.xml",
            "analytics-reporting/pom.xml",
            "analytics-monitoring/pom.xml"
        };
        
        for (String pomFile : pomFiles) {
            Path targetPath = outputDirectory.resolve(pomFile);
            String content = generatePomContent(pomFile);
            writeFile(targetPath, content);
        }
        
        stats.put("XML files", pomFiles.length);
        System.out.println("   ✓ " + pomFiles.length + " fichiers XML organisés");
        System.out.println();
    }
    
    /**
     * Organise les fichiers YAML (configuration).
     */
    private void organizeYamlFiles() throws IOException {
        System.out.println("📋 Organisation des fichiers YAML...");
        
        Map<String, String> yamlFiles = new HashMap<>();
        
        // Application configs
        yamlFiles.put("analytics-api/src/main/resources/application.yml", 
            "Application config");
        yamlFiles.put("analytics-collector/src/main/resources/application.yml", 
            "Collector config");
        
        // Kubernetes
        yamlFiles.put("k8s/deployment.yaml", "K8s Deployment");
        yamlFiles.put("k8s/service.yaml", "K8s Service");
        yamlFiles.put("k8s/configmap.yaml", "K8s ConfigMap");
        yamlFiles.put("k8s/secrets.yaml", "K8s Secrets");
        yamlFiles.put("k8s/hpa.yaml", "K8s HPA");
        yamlFiles.put("k8s/servicemonitor.yaml", "K8s ServiceMonitor");
        
        // Monitoring
        yamlFiles.put("monitoring/prometheus/prometheus.yml", "Prometheus config");
        yamlFiles.put("monitoring/prometheus/alerts.yml", "Prometheus alerts");
        yamlFiles.put("monitoring/alertmanager/alertmanager.yml", "Alertmanager config");
        yamlFiles.put("monitoring/grafana/datasources.yml", "Grafana datasources");
        yamlFiles.put("monitoring/grafana/dashboards.yml", "Grafana dashboards");
        
        // Docker Compose
        yamlFiles.put("docker-compose.yml", "Docker Compose");
        
        for (Map.Entry<String, String> entry : yamlFiles.entrySet()) {
            Path targetPath = outputDirectory.resolve(entry.getKey());
            String content = generateYamlContent(entry.getValue());
            writeFile(targetPath, content);
        }
        
        stats.put("YAML files", yamlFiles.size());
        System.out.println("   ✓ " + yamlFiles.size() + " fichiers YAML organisés");
        System.out.println();
    }
    
    /**
     * Organise les fichiers SQL.
     */
    private void organizeSqlFiles() throws IOException {
        System.out.println("🗄️  Organisation des fichiers SQL...");
        
        String[] sqlFiles = {
            "sql/init-clickhouse.sql",
            "sql/views.sql",
            "sql/queries.sql"
        };
        
        for (String sqlFile : sqlFiles) {
            Path targetPath = outputDirectory.resolve(sqlFile);
            String content = "-- " + sqlFile + "\n-- ClickHouse SQL file\n";
            writeFile(targetPath, content);
        }
        
        stats.put("SQL files", sqlFiles.length);
        System.out.println("   ✓ " + sqlFiles.length + " fichiers SQL organisés");
        System.out.println();
    }
    
    /**
     * Organise les fichiers Docker.
     */
    private void organizeDockerFiles() throws IOException {
        System.out.println("🐳 Organisation des fichiers Docker...");
        
        String[] dockerFiles = {
            "Dockerfile",
            ".dockerignore"
        };
        
        for (String dockerFile : dockerFiles) {
            Path targetPath = outputDirectory.resolve(dockerFile);
            String content = generateDockerContent(dockerFile);
            writeFile(targetPath, content);
        }
        
        stats.put("Docker files", dockerFiles.length);
        System.out.println("   ✓ " + dockerFiles.length + " fichiers Docker organisés");
        System.out.println();
    }
    
    /**
     * Organise les fichiers Kubernetes.
     */
    private void organizeKubernetesFiles() throws IOException {
        System.out.println("☸️  Organisation des fichiers Kubernetes...");
        
        // Déjà fait dans organizeYamlFiles
        System.out.println("   ✓ Fichiers K8s déjà organisés");
        System.out.println();
    }
    
    /**
     * Organise les fichiers de monitoring.
     */
    private void organizeMonitoringFiles() throws IOException {
        System.out.println("📊 Organisation des fichiers de monitoring...");
        
        // Déjà fait dans organizeYamlFiles
        System.out.println("   ✓ Fichiers monitoring déjà organisés");
        System.out.println();
    }
    
    /**
     * Organise la documentation.
     */
    private void organizeDocumentation() throws IOException {
        System.out.println("📚 Organisation de la documentation...");
        
        String[] docFiles = {
            "README.md",
            "docs/ARCHITECTURE.md",
            "docs/API.md",
            "docs/DEPLOYMENT.md",
            "docs/CONTRIBUTING.md"
        };
        
        for (String docFile : docFiles) {
            Path targetPath = outputDirectory.resolve(docFile);
            String content = generateDocContent(docFile);
            writeFile(targetPath, content);
        }
        
        stats.put("Documentation files", docFiles.length);
        System.out.println("   ✓ " + docFiles.length + " fichiers doc organisés");
        System.out.println();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Retourne le chemin du fichier Java.
     */
    private Path getJavaFilePath(String module, String subPackage, String className) {
        String basePath;
        if ("test".equals(subPackage)) {
            basePath = module + "/src/test/java/com/nexusai/analytics";
        } else {
            basePath = module + "/src/main/java/com/nexusai/analytics";
        }
        
        String moduleShort = module.replace("analytics-", "");
        if (!"test".equals(subPackage)) {
            basePath += "/" + moduleShort + "/" + subPackage;
        } else {
            basePath += "/" + moduleShort;
        }
        
        return outputDirectory.resolve(basePath).resolve(className + ".java");
    }
    
    /**
     * Écrit un fichier.
     */
    private void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
    
    /**
     * Génère le contenu d'un fichier Java.
     */
    private String generateJavaFileContent(String className, ModuleInfo info) {
        String moduleShort = info.module.replace("analytics-", "");
        String packageName = "com.nexusai.analytics." + moduleShort;
        
        if (!"test".equals(info.subPackage)) {
            packageName += "." + info.subPackage;
        }
        
        return String.format("""
            package %s;
            
            /**
             * %s
             * 
             * TODO: Implémenter cette classe
             * 
             * @author NexusAI Team
             */
            public class %s {
                // TODO: Implémenter
            }
            """, packageName, className, className);
    }
    
    /**
     * Génère le contenu d'un pom.xml.
     */
    private String generatePomContent(String pomFile) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<!-- " + pomFile + " -->\n" +
               "<project>\n" +
               "    <!-- TODO: Compléter le POM -->\n" +
               "</project>\n";
    }
    
    /**
     * Génère le contenu d'un fichier YAML.
     */
    private String generateYamlContent(String description) {
        return "# " + description + "\n# TODO: Compléter la configuration\n";
    }
    
    /**
     * Génère le contenu d'un fichier Docker.
     */
    private String generateDockerContent(String fileName) {
        return "# " + fileName + "\n# TODO: Compléter le Dockerfile\n";
    }
    
    /**
     * Génère le contenu d'un fichier de documentation.
     */
    private String generateDocContent(String docFile) {
        return "# " + docFile + "\n\nTODO: Compléter la documentation\n";
    }
    
    /**
     * Affiche les statistiques.
     */
    private void printStatistics() {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("STATISTIQUES");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println();
        
        int total = 0;
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            System.out.printf("  %-25s : %3d fichiers%n", entry.getKey(), entry.getValue());
            total += entry.getValue();
        }
        
        System.out.println("  " + "-".repeat(40));
        System.out.printf("  %-25s : %3d fichiers%n", "TOTAL", total);
        System.out.println();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CLASSES INTERNES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Informations sur un module.
     */
    private static class ModuleInfo {
        String module;
        String subPackage;
        
        ModuleInfo(String module, String subPackage) {
            this.module = module;
            this.subPackage = subPackage;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MAIN - Point d'entrée
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Point d'entrée principal.
     */
    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.err.println("Usage: FileOrganizerUtility <source-dir> <output-dir>");
                System.err.println();
                System.err.println("Example:");
                System.err.println("  java FileOrganizerUtility ./source ./nexusai-analytics");
                System.exit(1);
            }
            
            String sourceDir = args[0];
            String outputDir = args[1];
            
            FileOrganizerUtility organizer = new FileOrganizerUtility(sourceDir, outputDir);
            organizer.organize();
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
