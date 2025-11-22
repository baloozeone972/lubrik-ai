# 🚀 Module 5 : Guide de Démarrage Rapide

## ⚡ Quick Start en 5 Minutes

### Étape 1 : Compiler le Parser (30 secondes)

```bash
# Créer le dossier
mkdir -p nexusai-tools/src/main/java/com/nexusai/tools

# Copier ModuleFileParser.java depuis l'artifact
# (Copier le contenu de l'artifact "Module 5 - File Parser & Generator")

# Compiler
cd nexusai-tools
javac src/main/java/com/nexusai/tools/ModuleFileParser.java

# Créer un alias pratique
alias parse='java -cp src/main/java com.nexusai.tools.ModuleFileParser'
```

### Étape 2 : Préparer les Artifacts (1 minute)

```bash
# Créer le dossier artifacts
mkdir -p artifacts

# Copier les 5 artifacts créés précédemment dans ce dossier :
# 1. nexus-image-gen-module.md       (Structure, Entités, DTOs, Infrastructure)
# 2. nexus-image-services.md         (Services, Controllers)
# 3. nexus-image-config-worker.md    (Config, Worker Python)
# 4. nexus-image-tests-sql.md        (Tests, SQL)
# 5. nexus-image-readme.md           (README, Documentation)
```

### Étape 3 : Générer l'Arborescence (30 secondes)

```bash
# Parser tous les artifacts
parse ./nexus-image-generation artifacts/*.md

# Vérifier la structure
tree nexus-image-generation -L 3
```

### Étape 4 : Compiler le Projet (2 minutes)

```bash
cd nexus-image-generation

# Compiler tous les modules
mvn clean install -DskipTests

# Résultat attendu :
# [INFO] BUILD SUCCESS
# [INFO] ------------------------------------------------------------------------
# [INFO] Total time:  01:45 min
```

### Étape 5 : Lancer l'Infrastructure (1 minute)

```bash
# Démarrer PostgreSQL, Kafka, MinIO
docker-compose up -d

# Vérifier que tout est démarré
docker-compose ps

# Attendre que tout soit ready (~30 secondes)
```

🎉 **Voilà ! Votre Module 5 est prêt à fonctionner !**

---

## 📋 Checklist Complète de Setup

### ✅ Pré-requis

```bash
# Vérifier Java
java -version
# Doit afficher: openjdk version "21.x.x"

# Vérifier Maven
mvn -version
# Doit afficher: Apache Maven 3.9.x

# Vérifier Docker
docker --version
docker-compose --version

# Vérifier Python (pour le worker)
python --version
# Doit afficher: Python 3.11.x
```

### ✅ Installation Complète Étape par Étape

#### 1️⃣ Cloner et Préparer

```bash
# Créer la structure de base
mkdir -p nexusai-project
cd nexusai-project

# Créer les dossiers nécessaires
mkdir -p {nexusai-tools,artifacts,nexus-image-generation}
```

#### 2️⃣ Compiler le Parser

```bash
cd nexusai-tools

# Créer la structure Maven
cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.nexusai</groupId>
    <artifactId>module-file-parser</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>com.nexusai.tools.ModuleFileParser</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF

# Créer le dossier source
mkdir -p src/main/java/com/nexusai/tools

# Copier ModuleFileParser.java (depuis l'artifact)
# vim src/main/java/com/nexusai/tools/ModuleFileParser.java

# Compiler
mvn clean package

# Le JAR est maintenant dans target/module-file-parser-1.0.0.jar
cd ..
```

#### 3️⃣ Récupérer les Artifacts

**Option A : Copier depuis les artifacts Claude**

```bash
# Les 5 artifacts ont été créés précédemment :
# 1. nexus-image-gen-module
# 2. nexus-image-services
# 3. nexus-image-config-worker
# 4. nexus-image-tests-sql
# 5. nexus-image-readme

# Copier leur contenu dans artifacts/*.md
```

**Option B : Créer manuellement**

```bash
cd artifacts

# Créer les fichiers
touch {structure,services,config,tests,docs}.md

# Remplir chaque fichier avec le contenu correspondant
```

#### 4️⃣ Générer l'Arborescence

```bash
# Retour à la racine
cd ..

# Parser tous les artifacts
java -jar nexusai-tools/target/module-file-parser-1.0.0.jar \
    ./nexus-image-generation \
    artifacts/*.md

# Consulter le rapport
cat nexus-image-generation/PARSING_REPORT.md
```

#### 5️⃣ Vérifier la Structure Générée

```bash
cd nexus-image-generation

# Vérifier l'arborescence
tree -L 2

# Sortie attendue :
# .
# ├── PARSING_REPORT.md
# ├── README.md
# ├── docker-compose.yml
# ├── pom.xml
# ├── nexus-image-api/
# │   ├── pom.xml
# │   └── src/
# ├── nexus-image-core/
# │   ├── pom.xml
# │   └── src/
# ├── nexus-image-domain/
# │   ├── pom.xml
# │   └── src/
# ├── nexus-image-infrastructure/
# │   ├── pom.xml
# │   └── src/
# ├── nexus-image-worker/
# │   ├── Dockerfile
# │   ├── requirements.txt
# │   └── worker.py
# └── scripts/
#     └── schema.sql
```

#### 6️⃣ Créer les POM.xml Manquants

Les sous-modules nécessitent leurs propres POM :

```bash
# Pour chaque sous-module
for module in nexus-image-{domain,infrastructure,core,api}; do
    cat > $module/pom.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.nexusai</groupId>
        <artifactId>nexus-image-generation</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>$module</artifactId>
    
    <dependencies>
        <!-- Dépendances spécifiques au module -->
    </dependencies>
</project>
EOF
done
```

#### 7️⃣ Compiler le Projet

```bash
# Compilation complète
mvn clean install -DskipTests

# Si succès :
# [INFO] BUILD SUCCESS
```

Si erreurs de compilation, vérifier :
- Les imports Java
- Les dépendances Maven
- Les chemins de packages

#### 8️⃣ Configurer l'Environnement

```bash
# Créer le fichier .env
cat > .env << 'EOF'
# Database
POSTGRES_DB=nexusai
POSTGRES_USER=nexusai
POSTGRES_PASSWORD=nexusai123

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# S3/MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin
AWS_S3_BUCKET=nexusai-images

# API Keys (à remplacer)
OPENAI_API_KEY=your-key-here
STRIPE_API_KEY=your-key-here
EOF
```

#### 9️⃣ Initialiser la Base de Données

```bash
# Démarrer PostgreSQL
docker-compose up -d postgres

# Attendre que PostgreSQL soit prêt
sleep 10

# Créer les tables
docker-compose exec postgres psql -U nexusai -d nexusai -f /scripts/schema.sql

# Vérifier
docker-compose exec postgres psql -U nexusai -d nexusai -c "\dt"
```

#### 🔟 Démarrer Tous les Services

```bash
# Démarrer l'infrastructure complète
docker-compose up -d

# Vérifier les logs
docker-compose logs -f
```

---

## 🧪 Tests de Validation

### Test 1 : API Health Check

```bash
# Attendre que l'API démarre (~30 secondes)
sleep 30

# Tester le health endpoint
curl http://localhost:8085/actuator/health

# Résultat attendu :
# {"status":"UP"}
```

### Test 2 : Swagger UI

```bash
# Ouvrir dans le navigateur
open http://localhost:8085/swagger-ui.html

# Ou avec curl
curl http://localhost:8085/v3/api-docs
```

### Test 3 : Worker Python

```bash
# Vérifier que le worker écoute Kafka
docker-compose logs image-worker | grep "Worker démarré"

# Devrait afficher :
# Worker démarré, en attente de requêtes...
```

### Test 4 : Base de Données

```bash
# Se connecter à PostgreSQL
docker-compose exec postgres psql -U nexusai -d nexusai

# Lister les tables
\dt

# Devrait afficher :
#              List of relations
#  Schema |       Name        | Type  |  Owner
# --------+-------------------+-------+---------
#  public | album_images      | table | nexusai
#  public | generated_images  | table | nexusai
#  public | image_albums      | table | nexusai
```

### Test 5 : MinIO (S3)

```bash
# Ouvrir la console MinIO
open http://localhost:9001

# Login: minioadmin / minioadmin

# Créer le bucket "nexusai-images"
```

### Test 6 : Génération d'une Image (Bout en Bout)

```bash
# Créer un token JWT de test (à adapter selon votre module User)
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Générer une image
curl -X POST http://localhost:8085/api/v1/images/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "prompt": "A beautiful sunset over mountains",
    "style": "realistic",
    "resolution": "1024x1024"
  }'

# Devrait retourner :
# {
#   "id": "uuid",
#   "status": "QUEUED",
#   "tokens_cost": 20
# }
```

---

## 🐛 Dépannage

### Problème : mvn clean install échoue

**Erreur:** `Cannot find symbol`

**Solution:**
```bash
# Vérifier que tous les fichiers Java ont été générés
find nexus-image-* -name "*.java"

# Reparser si nécessaire
java -jar nexusai-tools/target/module-file-parser-1.0.0.jar \
    ./nexus-image-generation \
    artifacts/*.md --force
```

### Problème : Docker Compose ne démarre pas

**Erreur:** `Port already in use`

**Solution:**
```bash
# Identifier le processus utilisant le port
lsof -i :5432  # PostgreSQL
lsof -i :9092  # Kafka
lsof -i :8085  # API

# Arrêter le processus ou changer le port dans docker-compose.yml
```

### Problème : Worker Python ne démarre pas

**Erreur:** `ModuleNotFoundError: No module named 'diffusers'`

**Solution:**
```bash
# Installer les dépendances dans le container
docker-compose exec image-worker pip install -r requirements.txt

# Ou reconstruire l'image
docker-compose build image-worker
docker-compose up -d image-worker
```

### Problème : Out of Memory (Worker)

**Erreur:** `CUDA out of memory`

**Solution:**
```python
# Dans worker.py, activer les optimisations
self.pipe.enable_attention_slicing()
self.pipe.enable_vae_slicing()
self.pipe.enable_sequential_cpu_offload()
```

---

## 📊 Monitoring

### Prometheus

```bash
# Métriques exposées
curl http://localhost:8085/actuator/prometheus

# Prometheus UI
open http://localhost:9090
```

### Grafana

```bash
# Grafana UI
open http://localhost:3000

# Login: admin / admin

# Importer le dashboard : monitoring/grafana-dashboard.json
```

---

## 🎯 Prochaines Étapes

Maintenant que le Module 5 est fonctionnel :

1. **Intégrer avec le Module User** pour l'authentification
2. **Intégrer avec le Module Payment** pour les tokens
3. **Intégrer avec le Module Moderation** pour filtrer les prompts
4. **Ajouter les fonctionnalités avancées** (albums, recherche, etc.)
5. **Déployer en staging** avec Kubernetes

---

## 📚 Ressources

- **Documentation complète**: `nexus-image-generation/README.md`
- **API Reference**: `http://localhost:8085/swagger-ui.html`
- **Rapport de parsing**: `nexus-image-generation/PARSING_REPORT.md`
- **Scripts SQL**: `nexus-image-generation/scripts/`

---

## ✅ Checklist Finale

Avant de passer en production :

- [ ] Tous les tests unitaires passent (>80% coverage)
- [ ] Tests d'intégration OK
- [ ] Test E2E de génération d'image OK
- [ ] Monitoring configuré (Prometheus + Grafana)
- [ ] Secrets externalisés (pas de clés en dur)
- [ ] Backup de la base de données configuré
- [ ] CI/CD pipeline configuré
- [ ] Documentation à jour
- [ ] Équipe formée sur le module

---

**🎉 Félicitations ! Votre Module 5 est maintenant opérationnel !**

Pour toute question, consultez la documentation ou contactez l'équipe NexusAI.
