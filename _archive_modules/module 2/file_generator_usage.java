/**
 * ============================================================================
 * GUIDE D'UTILISATION DU FILE TREE GENERATOR
 * ============================================================================
 */

// ============================================================================
// EXEMPLE 1 : Utilisation Simple
// ============================================================================

import java.io.*;
import java.nio.file.*;

public class SimpleUsageExample {
    
    public static void main(String[] args) throws IOException {
        // Contenu de documentation (peut venir des artifacts Claude)
        String documentation = """
            # Module Payment
            
            Voici le code Java :
            
            ```java
            package com.nexusai.payment.domain.entity;
            
            import javax.persistence.*;
            import java.util.UUID;
            
            /**
             * Entité Subscription.
             */
            @Entity
            @Table(name = "subscriptions")
            public class Subscription {
                
                @Id
                @GeneratedValue(strategy = GenerationType.AUTO)
                private UUID id;
                
                private String plan;
                private String status;
                
                // Getters et setters
            }
            ```
            
            Et le pom.xml :
            
            ```xml
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.nexusai</groupId>
                <artifactId>payment-service</artifactId>
                <version>1.0.0</version>
            </project>
            ```
            
            Configuration application.yml :
            
            ```yaml
            # application.yml
            server:
              port: 8082
            
            spring:
              application:
                name: payment-service
            ```
            """;
        
        // Créer le générateur
        FileTreeGenerator generator = new FileTreeGenerator("./output");
        
        // Parser et générer
        generator.parseAndGenerateFromString(documentation);
        
        // Afficher le rapport
        FileTreeGenerator.GenerationReport report = generator.getLastReport();
        System.out.println("Terminé ! " + report.filesCreated + " fichiers créés.");
    }
}

// ============================================================================
// EXEMPLE 2 : Depuis un Fichier
// ============================================================================

public class FileUsageExample {
    
    public static void main(String[] args) throws IOException {
        // Lire depuis un fichier Markdown
        File docFile = new File("nexusai-payment-module.md");
        
        // Créer le générateur avec chemin de sortie
        FileTreeGenerator generator = new FileTreeGenerator("./payment-service");
        
        // Parser et générer
        generator.parseAndGenerate(docFile);
        
        // Vérifier le résultat
        FileTreeGenerator.GenerationReport report = generator.getLastReport();
        
        if (report.errors.isEmpty()) {
            System.out.println("✅ Génération réussie !");
            System.out.println("📦 " + report.filesCreated + " fichiers créés");
        } else {
            System.err.println("❌ Erreurs détectées:");
            report.errors.forEach(System.err::println);
        }
    }
}

// ============================================================================
// EXEMPLE 3 : Avec Gestion d'Erreurs Avancée
// ============================================================================

public class AdvancedUsageExample {
    
    public static void generateProjectStructure(
            String documentationPath,
            String outputPath) {
        
        try {
            // Valider le chemin de sortie
            Path output = Paths.get(outputPath);
            if (Files.exists(output) && !Files.isDirectory(output)) {
                throw new IllegalArgumentException(
                    "Le chemin de sortie doit être un dossier"
                );
            }
            
            // Créer le dossier si inexistant
            Files.createDirectories(output);
            
            // Lire la documentation
            File docFile = new File(documentationPath);
            if (!docFile.exists()) {
                throw new FileNotFoundException(
                    "Fichier de documentation introuvable: " + documentationPath
                );
            }
            
            // Générer
            System.out.println("🚀 Début de la génération...");
            FileTreeGenerator generator = new FileTreeGenerator(outputPath);
            generator.parseAndGenerate(docFile);
            
            // Rapport détaillé
            FileTreeGenerator.GenerationReport report = generator.getLastReport();
            System.out.println("\n📊 RÉSULTATS:");
            System.out.println("  Fichiers créés: " + report.filesCreated);
            System.out.println("  Blocs ignorés: " + report.skipped);
            System.out.println("  Erreurs: " + report.errors.size());
            System.out.println("  Durée: " + (report.endTime - report.startTime) + " ms");
            
            if (!report.errors.isEmpty()) {
                System.err.println("\n❌ ERREURS:");
                report.errors.forEach(e -> System.err.println("  • " + e));
            }
            
            // Lister les fichiers créés
            if (!report.createdFiles.isEmpty()) {
                System.out.println("\n📁 FICHIERS CRÉÉS:");
                report.createdFiles.stream()
                    .limit(10)
                    .forEach(f -> System.out.println("  • " + f));
                
                if (report.createdFiles.size() > 10) {
                    System.out.println("  ... et " + 
                        (report.createdFiles.size() - 10) + " autres");
                }
            }
            
        } catch (IOException e) {
            System.err.println("❌ Erreur I/O: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java AdvancedUsageExample <doc-file> <output-dir>");
            System.exit(1);
        }
        
        generateProjectStructure(args[0], args[1]);
    }
}

// ============================================================================
// EXEMPLE 4 : Batch Processing (Plusieurs Modules)
// ============================================================================

public class BatchGenerationExample {
    
    public static void main(String[] args) throws IOException {
        // Map de modules et leurs documentations
        Map<String, String> modules = Map.of(
            "payment-service", "docs/module-2-payment.md",
            "companion-service", "docs/module-3-companion.md",
            "conversation-service", "docs/module-4-conversation.md"
        );
        
        System.out.println("🚀 Génération de " + modules.size() + " modules...\n");
        
        int totalFiles = 0;
        
        for (Map.Entry<String, String> entry : modules.entrySet()) {
            String moduleName = entry.getKey();
            String docPath = entry.getValue();
            
            System.out.println("📦 Traitement du module: " + moduleName);
            
            try {
                FileTreeGenerator generator = new FileTreeGenerator("./" + moduleName);
                generator.parseAndGenerate(new File(docPath));
                
                FileTreeGenerator.GenerationReport report = generator.getLastReport();
                System.out.println("  ✅ " + report.filesCreated + " fichiers créés");
                totalFiles += report.filesCreated;
                
            } catch (Exception e) {
                System.err.println("  ❌ Erreur: " + e.getMessage());
            }
            
            System.out.println();
        }
        
        System.out.println("═══════════════════════════════════════");
        System.out.println("✅ TOTAL: " + totalFiles + " fichiers créés");
        System.out.println("═══════════════════════════════════════");
    }
}

// ============================================================================
// EXEMPLE 5 : Avec Interface Graphique Simple (Swing)
// ============================================================================

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class FileGeneratorGUI extends JFrame {
    
    private JTextArea documentationArea;
    private JTextField outputPathField;
    private JButton generateButton;
    private JTextArea logArea;
    
    public FileGeneratorGUI() {
        setTitle("NexusAI File Tree Generator");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Panel du haut : Documentation
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JLabel("Documentation (Markdown):"), BorderLayout.NORTH);
        
        documentationArea = new JTextArea(10, 60);
        documentationArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane docScroll = new JScrollPane(documentationArea);
        topPanel.add(docScroll, BorderLayout.CENTER);
        
        // Panel du milieu : Paramètres
        JPanel middlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        middlePanel.add(new JLabel("Chemin de sortie:"));
        
        outputPathField = new JTextField(30);
        outputPathField.setText("./output");
        middlePanel.add(outputPathField);
        
        JButton browseButton = new JButton("Parcourir...");
        browseButton.addActionListener(e -> browseOutputPath());
        middlePanel.add(browseButton);
        
        generateButton = new JButton("Générer Arborescence");
        generateButton.addActionListener(e -> generate());
        middlePanel.add(generateButton);
        
        // Panel du bas : Logs
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(new JLabel("Logs:"), BorderLayout.NORTH);
        
        logArea = new JTextArea(15, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        bottomPanel.add(logScroll, BorderLayout.CENTER);
        
        // Assembler
        add(topPanel, BorderLayout.NORTH);
        add(middlePanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void browseOutputPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void generate() {
        String documentation = documentationArea.getText();
        String outputPath = outputPathField.getText();
        
        if (documentation.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Veuillez saisir du contenu de documentation",
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        generateButton.setEnabled(false);
        logArea.setText("🚀 Génération en cours...\n");
        
        // Exécuter dans un thread séparé
        new Thread(() -> {
            try {
                FileTreeGenerator generator = new FileTreeGenerator(outputPath);
                generator.parseAndGenerateFromString(documentation);
                
                FileTreeGenerator.GenerationReport report = generator.getLastReport();
                
                SwingUtilities.invokeLater(() -> {
                    logArea.append("\n✅ Génération terminée !\n");
                    logArea.append("📦 Fichiers créés: " + report.filesCreated + "\n");
                    logArea.append("⏱️  Durée: " + (report.endTime - report.startTime) + " ms\n");
                    
                    if (!report.errors.isEmpty()) {
                        logArea.append("\n❌ ERREURS:\n");
                        report.errors.forEach(e -> logArea.append("  • " + e + "\n"));
                    }
                    
                    generateButton.setEnabled(true);
                    
                    JOptionPane.showMessageDialog(this,
                        "Génération terminée !\n" + report.filesCreated + " fichiers créés.",
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE);
                });
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append("\n❌ ERREUR: " + e.getMessage() + "\n");
                    generateButton.setEnabled(true);
                    
                    JOptionPane.showMessageDialog(this,
                        "Erreur lors de la génération:\n" + e.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FileGeneratorGUI gui = new FileGeneratorGUI();
            gui.setVisible(true);
        });
    }
}

// ============================================================================
// PLUGIN MAVEN
// ============================================================================

/**
 * pom.xml pour créer un plugin Maven réutilisable
 */
/*
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.nexusai.tools</groupId>
    <artifactId>file-tree-generator</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <name>File Tree Generator</name>
    <description>Génère une arborescence de fichiers depuis documentation</description>
    
    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <build>
        <plugins>
            <!-- Compiler -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
            </plugin>
            
            <!-- Créer un JAR exécutable -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>FileTreeGenerator</mainClass>
                            <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
            
            <!-- Créer un fat JAR avec toutes les dépendances -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>FileTreeGenerator</mainClass>
                                </transformer>
                            </transformers>
                            <finalName>file-generator</finalName>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
*/

// ============================================================================
// SCRIPT BASH D'UTILISATION
// ============================================================================

/**
 * generate-files.sh
 * 
 * Script bash pour faciliter l'utilisation du générateur
 */
/*
#!/bin/bash

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}   NexusAI File Tree Generator${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo

# Vérifier arguments
if [ "$#" -lt 2 ]; then
    echo -e "${RED}Usage: $0 <documentation.md> <output-directory>${NC}"
    echo
    echo "Exemple:"
    echo "  $0 nexusai-payment-module.md ./payment-service"
    exit 1
fi

DOC_FILE="$1"
OUTPUT_DIR="$2"

# Vérifier fichier doc
if [ ! -f "$DOC_FILE" ]; then
    echo -e "${RED}❌ Fichier introuvable: $DOC_FILE${NC}"
    exit 1
fi

# Créer dossier output si nécessaire
mkdir -p "$OUTPUT_DIR"

echo -e "${YELLOW}📄 Documentation: $DOC_FILE${NC}"
echo -e "${YELLOW}📁 Sortie: $OUTPUT_DIR${NC}"
echo

# Compiler si nécessaire
if [ ! -f "file-generator.jar" ]; then
    echo -e "${YELLOW}🔨 Compilation du générateur...${NC}"
    javac FileTreeGenerator.java
    jar cvfe file-generator.jar FileTreeGenerator FileTreeGenerator*.class
    echo
fi

# Exécuter
echo -e "${GREEN}🚀 Génération de l'arborescence...${NC}"
echo
java -jar file-generator.jar "$OUTPUT_DIR" "$DOC_FILE"

EXIT_CODE=$?

echo
if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}✅ Génération terminée avec succès !${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    # Compter fichiers créés
    FILE_COUNT=$(find "$OUTPUT_DIR" -type f | wc -l)
    echo -e "${GREEN}📦 $FILE_COUNT fichiers créés${NC}"
    
    # Afficher arborescence
    echo
    echo -e "${YELLOW}📁 Arborescence créée:${NC}"
    tree -L 3 "$OUTPUT_DIR" 2>/dev/null || ls -R "$OUTPUT_DIR"
else
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}❌ Erreur lors de la génération${NC}"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    exit $EXIT_CODE
fi
*/

// ============================================================================
// README D'UTILISATION
// ============================================================================

/**
 * README.md
 */
/*
# File Tree Generator

Outil pour générer automatiquement une arborescence de fichiers à partir de documentation technique.

## 🚀 Installation

### Compilation

```bash
# Compiler la classe
javac FileTreeGenerator.java

# Créer un JAR exécutable
jar cvfe file-generator.jar FileTreeGenerator FileTreeGenerator*.class
```

### Avec Maven

```bash
mvn clean package
```

## 📖 Utilisation

### Ligne de Commande

```bash
# Depuis un fichier de documentation
java -jar file-generator.jar ./output-directory documentation.md

# Ou directement avec la classe compilée
java FileTreeGenerator ./output-directory documentation.md
```

### Depuis Java

```java
import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        FileTreeGenerator generator = new FileTreeGenerator("./output");
        generator.parseAndGenerate(new File("doc.md"));
    }
}
```

### Interface Graphique

```bash
java FileGeneratorGUI
```

### Script Bash

```bash
chmod +x generate-files.sh
./generate-files.sh documentation.md ./output
```

## 📁 Formats Supportés

Le générateur supporte les types de fichiers suivants :

- ✅ **Java** (.java)
- ✅ **XML** (.xml, pom.xml)
- ✅ **YAML** (.yml, .yaml)
- ✅ **Properties** (.properties)
- ✅ **SQL** (.sql)
- ✅ **Shell** (.sh)
- ✅ **Dockerfile**
- ✅ **Markdown** (.md)

## 🎯 Exemple

### Entrée (documentation.md)

```markdown
# Mon Projet

```java
package com.example;

public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello!");
    }
}
```

```xml
<?xml version="1.0"?>
<project>
    <groupId>com.example</groupId>
    <artifactId>hello</artifactId>
</project>
```
```

### Commande

```bash
java -jar file-generator.jar ./my-project documentation.md
```

### Sortie

```
my-project/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── example/
│                   └── HelloWorld.java
└── pom.xml
```

## 🔧 Options Avancées

### Traitement par Batch

```java
Map<String, String> modules = Map.of(
    "module-1", "docs/module1.md",
    "module-2", "docs/module2.md"
);

for (var entry : modules.entrySet()) {
    FileTreeGenerator gen = new FileTreeGenerator(entry.getKey());
    gen.parseAndGenerate(new File(entry.getValue()));
}
```

### Rapport Détaillé

```java
FileTreeGenerator gen = new FileTreeGenerator("./output");
gen.parseAndGenerateFromString(doc);

GenerationReport report = gen.getLastReport();
System.out.println("Fichiers créés: " + report.filesCreated);
System.out.println("Erreurs: " + report.errors.size());
```

## 🐛 Troubleshooting

### Erreur "Package non trouvé"

Vérifiez que vos blocs Java contiennent bien la déclaration `package`.

### Fichiers non créés

Activez les logs détaillés pour voir quels blocs sont ignorés.

### Chemins incorrects

Le générateur détecte automatiquement les chemins basés sur :
- Les packages Java
- Les commentaires dans le code
- Le contenu des fichiers

## 📞 Support

Pour toute question : support@nexusai.com
*/