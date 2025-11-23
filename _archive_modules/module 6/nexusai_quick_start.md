# ⚡ NEXUSAI AUDIO MODULE - DÉMARRAGE ULTRA-RAPIDE

**Temps estimé : 5 minutes** ⏱️

---

## 🎯 MÉTHODE 1 : Avec le générateur Java (RECOMMANDÉ)

### Étape 1 : Prérequis (30 secondes)

```bash
# Vérifier Java 21+
java -version

# Vérifier Maven
mvn -version

# Vérifier Docker (optionnel)
docker --version
```

### Étape 2 : Préparer les fichiers (30 secondes)

```bash
# Créer un dossier de travail
mkdir ~/nexusai-setup && cd ~/nexusai-setup

# Vous devez avoir ces 3 fichiers :
# 1. ProjectGenerator.java
# 2. nexusai-audio-complete.md
# 3. setup-nexusai-audio.sh

# Les fichiers sont disponibles dans les artifacts Claude précédents
```

### Étape 3 : Générer le projet (2 minutes)

```bash
# Méthode automatique (le plus simple)
chmod +x setup-nexusai-audio.sh

export OPENAI_API_KEY=sk-votre-clé
export ELEVENLABS_API_KEY=votre-clé

./setup-nexusai-audio.sh \
    --doc nexusai-audio-complete.md \
    --output ~/projects/nexus-audio

# ✅ C'EST TOUT ! Le script fait tout automatiquement :
# - Compile le générateur
# - Génère le projet complet (127 fichiers)
# - Démarre Docker Compose
# - Compile Maven
# - Affiche le résumé
```

### Étape 4 : Lancer l'application (30 secondes)

```bash
cd ~/projects/nexus-audio
mvn spring-boot:run -pl nexus-audio-api

# Attendre "Started AudioApplication"
```

### Étape 5 : Tester (30 secondes)

```bash
# Dans un autre terminal
curl http://localhost:8083/actuator/health
# Résultat : {"status":"UP"}

# Ouvrir Swagger UI
open http://localhost:8083/swagger-ui.html
```

**🎉 TERMINÉ ! Votre module audio est opérationnel !**

---

## 🐍 MÉTHODE 2 : Avec le générateur Python

### Étapes (3 minutes)

```bash
# 1. Copier le générateur Python
# (Disponible dans l'artifact précédent)
cp project_generator.py ~/nexusai-setup/

# 2. Générer le projet
python3 project_generator.py \
    nexusai-audio-complete.md \
    ~/projects/nexus-audio

# 3. Démarrer les services
cd ~/projects/nexus-audio
docker-compose up -d

# 4. Compiler
mvn clean install

# 5. Lancer
mvn spring-boot:run -pl nexus-audio-api
```

---

## 🛠️ MÉTHODE 3 : Manuelle (si problèmes)

### Option A : Sans Docker (le plus simple)

```bash
# 1. Créer le générateur
mkdir -p nexusai-generator/src/main/java/com/nexusai/tools
# Copier ProjectGenerator.java dans ce dossier

# 2. Créer le POM
cat > nexusai-generator/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.nexusai</groupId>
    <artifactId>nexusai-generator</artifactId>
    <version>1.0.0</version>
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>
</project>
EOF

# 3. Compiler
cd nexusai-generator
mvn clean compile

# 4. Générer le projet
java -cp target/classes com.nexusai.tools.ProjectGenerator \
    ../nexusai-audio-complete.md \
    ~/projects/nexus-audio

# 5. Aller dans le projet
cd ~/projects/nexus-audio

# 6. IMPORTANT : Installer PostgreSQL, Kafka et MinIO localement
# OU utiliser Docker Compose (voir Option B)
```

### Option B : Avec Docker (recommandé)

```bash
# Après l'étape 4 ci-dessus :

# 5. Démarrer les services
cd ~/projects/nexus-audio
docker-compose up -d

# Attendre 30 secondes que les services démarrent

# 6. Compiler le projet
mvn clean install -DskipTests

# 7. Lancer l'application
mvn spring-boot:run -pl nexus-audio-api
```

---

## 🔑 CONFIGURATION DES API KEYS

### Obtenir les clés

1. **OpenAI** (pour Whisper STT)
   - Aller sur https://platform.openai.com/api-keys
   - Créer une nouvelle clé
   - Coût : ~$0.006 / minute d'audio

2. **ElevenLabs** (pour TTS)
   - Aller sur https://elevenlabs.io/
   - S'inscrire (plan gratuit : 10K caractères/mois)
   - Copier la clé API depuis le dashboard

### Configurer

```bash
# Méthode 1 : Variables d'environnement (temporaire)
export OPENAI_API_KEY=sk-...
export ELEVENLABS_API_KEY=...

# Méthode 2 : Fichier .env (permanent)
cd ~/projects/nexus-audio
cat > .env << EOF
OPENAI_API_KEY=sk-...
ELEVENLABS_API_KEY=...
EOF

# Charger les variables
set -a; source .env; set +a
```

---

## 🧪 TESTER L'API

### Test 1 : Health Check

```bash
curl http://localhost:8083/actuator/health
```

**Résultat attendu :**
```json
{
  "status": "UP"
}
```

### Test 2 : Upload d'un message vocal

```bash
# Créer un fichier audio de test (ou utiliser un vrai fichier MP3)
echo "test" > test.mp3

# Upload
curl -X POST http://localhost:8083/api/v1/audio/voice-messages \
  -F "audioFile=@test.mp3" \
  -F "conversationId=conv-test-123" \
  -F "userId=550e8400-e29b-41d4-a716-446655440000" \
  -F "senderType=USER"
```

**Résultat attendu :**
```json
{
  "id": "...",
  "conversationId": "conv-test-123",
  "transcription": "...",
  "emotionDetected": "NEUTRAL",
  ...
}
```

### Test 3 : Swagger UI

```bash
# Ouvrir dans le navigateur
http://localhost:8083/swagger-ui.html

# Vous verrez :
# - Voice Messages API
# - Voice Calls API
# - Voice Profiles API
```

---

## 📊 VÉRIFIER QUE TOUT FONCTIONNE

### Checklist ✅

```bash
# 1. Services Docker
docker-compose ps
# Tous les services doivent être "Up"

# 2. Base de données
docker exec -it nexusai-audio-postgres psql -U nexusai -d nexusai -c "\dt"
# Doit afficher les tables : voice_messages, voice_calls, voice_profiles

# 3. MinIO
open http://localhost:9001
# Login: nexusai / nexusai123

# 4. Application
curl http://localhost:8083/actuator/health
# {"status":"UP"}

# 5. Logs
docker-compose logs -f nexusai-audio-service
# Doit afficher "Started AudioApplication"
```

---

## 🐛 DÉPANNAGE EXPRESS

### Problème : "Port 8083 already in use"

```bash
# Trouver le processus
lsof -ti:8083

# Tuer le processus
kill -9 $(lsof -ti:8083)

# Ou changer le port dans application.yml
```

### Problème : "Cannot connect to database"

```bash
# Vérifier PostgreSQL
docker-compose ps postgres
# Doit être "Up"

# Redémarrer PostgreSQL
docker-compose restart postgres

# Vérifier les logs
docker-compose logs postgres
```

### Problème : "MinIO connection refused"

```bash
# Redémarrer MinIO
docker-compose restart minio

# Vérifier le bucket
docker exec -it nexusai-audio-minio mc ls local/
```

### Problème : "Compilation Maven échoue"

```bash
# Nettoyer et recompiler
mvn clean install -DskipTests -U

# Si erreur de dépendances
rm -rf ~/.m2/repository/com/nexusai
mvn clean install -DskipTests
```

---

## 🚀 COMMANDES UTILES

### Démarrer / Arrêter

```bash
# Démarrer tout
docker-compose up -d && mvn spring-boot:run -pl nexus-audio-api

# Arrêter tout
docker-compose down
```

### Logs

```bash
# Logs de l'application
mvn spring-boot:run -pl nexus-audio-api | tee app.log

# Logs Docker
docker-compose logs -f

# Logs d'un service spécifique
docker-compose logs -f postgres
```

### Base de données

```bash
# Accéder à PostgreSQL
docker exec -it nexusai-audio-postgres psql -U nexusai -d nexusai

# Requête SQL
docker exec -it nexusai-audio-postgres psql -U nexusai -d nexusai \
  -c "SELECT * FROM voice_messages;"
```

### Nettoyer

```bash
# Nettoyer tout (ATTENTION : supprime les données)
docker-compose down -v
mvn clean
rm -rf ~/projects/nexus-audio
```

---

## 📚 RESSOURCES

### URLs importantes

- **API** : http://localhost:8083
- **Swagger UI** : http://localhost:8083/swagger-ui.html
- **Health** : http://localhost:8083/actuator/health
- **MinIO Console** : http://localhost:9001

### Documentation

- **README** : `~/projects/nexus-audio/README.md`
- **Architecture** : Dans les artifacts Claude
- **Plan de dev** : Dans les artifacts Claude

### Support

- **GitHub Issues** : (à créer)
- **Email** : dev@nexusai.com
- **Slack** : #nexusai-audio

---

## 🎓 PROCHAINES ÉTAPES

### Développement

1. **Lire le plan de développement** (5 semaines, 6 devs)
2. **Choisir un module** (api, core, stt, tts, etc.)
3. **Créer une branche Git**
   ```bash
   git checkout -b feature/mon-module
   ```
4. **Développer et tester**
5. **Faire une Pull Request**

### Tests

```bash
# Tests unitaires
mvn test

# Tests d'intégration
mvn verify

# Tests E2E
mvn test -Dtest=E2EVoiceMessageTest
```

### Déploiement

```bash
# Build Docker
docker build -t nexusai/audio-service:1.0.0 .

# Deploy Kubernetes
kubectl apply -f k8s/
```

---

## 🎉 FÉLICITATIONS !

Vous avez maintenant un module audio complet et fonctionnel !

**Ce que vous avez :**
- ✅ 8 modules Maven indépendants
- ✅ 127 fichiers générés automatiquement
- ✅ API REST complète avec Swagger
- ✅ Intégration OpenAI Whisper (STT)
- ✅ Intégration ElevenLabs (TTS)
- ✅ Stockage S3/MinIO
- ✅ Base de données PostgreSQL
- ✅ Kafka pour l'événementiel
- ✅ Docker Compose pour le dev
- ✅ Tests unitaires et d'intégration
- ✅ Documentation exhaustive

**Temps total : 5 minutes** ⚡

---

*Guide créé par l'équipe NexusAI - Version 1.0.0 - 20 Octobre 2025*
