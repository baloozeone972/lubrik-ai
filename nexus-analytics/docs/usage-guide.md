# Guide d'Utilisation - Organisation Automatique des Fichiers

## 📋 Vue d'ensemble

Ce guide explique comment utiliser l'utilitaire `FileOrganizerUtility` pour organiser automatiquement tous les fichiers du Module 10 dans la structure Maven correcte.

---

## 🎯 Objectif

L'utilitaire permet de :
- ✅ Créer automatiquement l'arborescence Maven complète
- ✅ Placer chaque fichier Java dans le bon package
- ✅ Organiser les fichiers de configuration (YAML, XML, SQL)
- ✅ Structurer les fichiers Docker et Kubernetes
- ✅ Générer des statistiques sur les fichiers créés

---

## 📁 Structure générée

```
nexusai-analytics/
├── pom.xml                                    # POM parent
├── README.md
├── Makefile
├── REMAINING-TASKS.md
├── docker-compose.yml
├── Dockerfile
├── .dockerignore
│
├── analytics-core/                            # Module Core
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexusai/analytics/core/
│       │   ├── model/                         # Modèles de données
│       │   │   ├── UserEvent.java
│       │   │   ├── SystemMetric.java
│       │   │   ├── AggregatedMetric.java
│       │   │   ├── Report.java
│       │   │   └── Alert.java
│       │   ├── service/                       # Services métier
│       │   │   ├── EventService.java
│       │   │   ├── MetricService.java
│       │   │   └── AggregationService.java
│       │   ├── repository/                    # Repositories ClickHouse
│       │   │   ├── EventRepository.java
│       │   │   ├── MetricRepository.java
│       │   │   └── AggregatedMetricRepository.java
│       │   └── config/                        # Configuration
│       │       ├── ClickHouseConfig.java
│       │       ├── KafkaConfig.java
│       │       └── RedisConfig.java
│       ├── main/resources/
│       │   └── application.yml
│       └── test/java/com/nexusai/analytics/core/
│           ├── EventServiceTest.java
│           ├── MetricServiceTest.java
│           └── AggregationServiceTest.java
│
├── analytics-api/                             # Module API
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexusai/analytics/api/
│       │   ├── controller/                    # Controllers REST
│       │   │   ├── EventController.java
│       │   │   ├── MetricController.java
│       │   │   ├── DashboardController.java
│       │   │   └── HealthController.java
│       │   ├── dto/                           # DTOs
│       │   │   ├── EventRequest.java
│       │   │   ├── EventResponse.java
│       │   │   ├── MetricRequest.java
│       │   │   └── MetricResponse.java
│       │   └── security/                      # Sécurité
│       │       └── JwtAuthenticationFilter.java
│       ├── main/resources/
│       │   └── application.yml
│       └── test/java/com/nexusai/analytics/api/
│           └── EventControllerTest.java
│
├── analytics-collector/                       # Module Collector
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexusai/analytics/collector/
│       │   ├── listener/                      # Kafka Listeners
│       │   │   ├── EventCollectorListener.java
│       │   │   ├── MetricCollectorListener.java
│       │   │   ├── EventBuffer.java
│       │   │   └── MetricBuffer.java
│       │   └── config/
│       │       └── KafkaListenerConfig.java
│       └── main/resources/
│           └── application.yml
│
├── analytics-reporting/                       # Module Reporting
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexusai/analytics/reporting/
│       │   ├── generator/
│       │   │   ├── ReportService.java
│       │   │   └── ReportGenerator.java
│       │   ├── scheduler/
│       │   │   └── ScheduledReportGenerator.java
│       │   └── exporter/
│       │       ├── ReportExporter.java
│       │       └── S3StorageService.java
│       └── main/resources/
│           └── application.yml
│
├── analytics-monitoring/                      # Module Monitoring
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexusai/analytics/monitoring/
│       │   ├── metrics/
│       │   │   └── AnalyticsMetricsService.java
│       │   ├── health/
│       │   │   ├── ClickHouseHealthIndicator.java
│       │   │   ├── KafkaHealthIndicator.java
│       │   │   └── BufferHealthIndicator.java
│       │   └── alerting/
│       │       ├── AlertService.java
│       │       └── NotificationService.java
│       └── main/resources/
│           └── application.yml
│
├── sql/                                       # Scripts SQL
│   ├── init-clickhouse.sql
│   ├── views.sql
│   └── queries.sql
│
├── k8s/                                       # Kubernetes
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── configmap.yaml
│   ├── secrets.yaml
│   ├── hpa.yaml
│   └── servicemonitor.yaml
│
├── monitoring/                                # Monitoring
│   ├── prometheus/
│   │   ├── prometheus.yml
│   │   └── alerts.yml
│   ├── grafana/
│   │   ├── datasources.yml
│   │   ├── dashboards.yml
│   │   └── dashboards/
│   │       ├── overview.json
│   │       ├── performance.json
│   │       └── errors.json
│   └── alertmanager/
│       └── alertmanager.yml
│
└── docs/                                      # Documentation
    ├── ARCHITECTURE.md
    ├── API.md
    ├── DEPLOYMENT.md
    └── CONTRIBUTING.md
```

---

## 🚀 Utilisation

### Option 1 : Script Shell (Recommandé)

Le plus simple est d'utiliser le script shell fourni :

```bash
# Rendre le script exécutable
chmod +x organize-files.sh

# Exécuter (avec répertoire de sortie par défaut)
./organize-files.sh

# OU avec un répertoire personnalisé
./organize-files.sh /path/to/output/directory
```

Le script va :
1. ✅ Vérifier que Java est installé
2. ✅ Compiler l'utilitaire Java
3. ✅ Créer la structure complète
4. ✅ Générer les statistiques
5. ✅ Créer des fichiers utilitaires (Makefile, REMAINING-TASKS.md)

### Option 2 : Utilitaire Java directement

Si vous préférez utiliser l'utilitaire Java directement :

```bash
# Compiler
javac FileOrganizerUtility.java

# Exécuter
java FileOrganizerUtility ./source-files ./nexusai-analytics
```

**Paramètres** :
- `./source-files` : Répertoire contenant les fichiers source (pas utilisé actuellement)
- `./nexusai-analytics` : Répertoire de sortie pour le projet Maven

### Option 3 : Dans un projet Java existant

Vous pouvez aussi intégrer l'utilitaire dans un projet Java :

```java
import com.nexusai.tools.FileOrganizerUtility;

public class Main {
    public static void main(String[] args) throws Exception {
        FileOrganizerUtility organizer = new FileOrganizerUtility(
            "./source",
            "./output"
        );
        organizer.organize();
    }
}
```

---

## 📊 Sortie attendue

Lors de l'exécution, vous verrez :

```
═══════════════════════════════════════════════════════════
FILE ORGANIZER UTILITY - Module 10 Analytics
═══════════════════════════════════════════════════════════

Source: ./source-files
Output: ./nexusai-analytics

📁 Création de la structure de base...
   ✓ Structure créée

☕ Organisation des fichiers Java...
   ✓ 45 fichiers Java organisés

📄 Organisation des fichiers XML...
   ✓ 6 fichiers XML organisés

📋 Organisation des fichiers YAML...
   ✓ 14 fichiers YAML organisés

🗄️  Organisation des fichiers SQL...
   ✓ 3 fichiers SQL organisés

🐳 Organisation des fichiers Docker...
   ✓ 2 fichiers Docker organisés

☸️  Organisation des fichiers Kubernetes...
   ✓ Fichiers K8s déjà organisés

📊 Organisation des fichiers de monitoring...
   ✓ Fichiers monitoring déjà organisés

📚 Organisation de la documentation...
   ✓ 5 fichiers doc organisés

═══════════════════════════════════════════════════════════
STATISTIQUES
═══════════════════════════════════════════════════════════

  Java files                :  45 fichiers
  XML files                 :   6 fichiers
  YAML files                :  14 fichiers
  SQL files                 :   3 fichiers
  Docker files              :   2 fichiers
  Documentation files       :   5 fichiers
  ----------------------------------------
  TOTAL                     :  75 fichiers

✅ Organisation terminée avec succès !
```

---

## 📝 Après l'organisation

Une fois l'organisation terminée, suivez ces étapes :

### 1. Aller dans le répertoire

```bash
cd nexusai-analytics
```

### 2. Compiler le projet

```bash
mvn clean install
```

Ou avec le Makefile :

```bash
make build
```

### 3. Démarrer les services

```bash
docker-compose up -d
```

Ou avec le Makefile :

```bash
make docker-up
```

### 4. Vérifier que tout fonctionne

```bash
# Vérifier les services Docker
docker-compose ps

# Vérifier la santé de l'API
curl http://localhost:8080/actuator/health

# Voir les logs
docker-compose logs -f analytics-api
```

Ou avec le Makefile :

```bash
make status
make health
make docker-logs
```

### 5. Initialiser ClickHouse

```bash
make init-clickhouse
```

### 6. Consulter le Swagger UI

Ouvrir dans le navigateur :
```
http://localhost:8080/swagger-ui.html
```

---

## 🔧 Personnalisation

### Ajouter de nouveaux fichiers Java

Pour ajouter un nouveau fichier Java à l'organisation, modifiez la méthode `organizeJavaFiles()` :

```java
// Dans FileOrganizerUtility.java
javaFiles.put("MaNouvelleclasse", new ModuleInfo("analytics-core", "service"));
```

### Ajouter de nouveaux modules

Pour ajouter un nouveau module Maven :

1. Ajoutez le répertoire dans `createBaseStructure()` :
```java
"analytics-nouveau-module/src/main/java/com/nexusai/analytics/nouveaumodule",
```

2. Ajoutez le module dans le POM parent

### Modifier la structure

Vous pouvez facilement modifier la structure en changeant le tableau `directories[]` dans `createBaseStructure()`.

---

## ⚠️ Limitations actuelles

L'utilitaire actuel :
- ✅ Crée la structure complète
- ✅ Génère des squelettes de fichiers
- ⚠️ Ne parse pas les fichiers existants (version future)
- ⚠️ Génère du contenu minimal (à compléter)

### Ce qui est généré

Chaque fichier Java généré contient :
```java
package com.nexusai.analytics.core.model;

/**
 * UserEvent
 * 
 * TODO: Implémenter cette classe
 * 
 * @author NexusAI Team
 */
public class UserEvent {
    // TODO: Implémenter
}
```

**Vous devez ensuite** :
1. Copier le code des artifacts créés
2. Remplacer le contenu des fichiers générés
3. Compléter les TODOs

---

## 🐛 Dépannage

### Erreur : "Java n'est pas installé"

```bash
# Installer Java 21 (Ubuntu/Debian)
sudo apt install openjdk-21-jdk

# Ou télécharger depuis
https://adoptium.net/
```

### Erreur : "Permission denied"

```bash
# Rendre le script exécutable
chmod +x organize-files.sh
```

### Erreur de compilation

```bash
# Vérifier la version de Java
java -version

# Compiler avec verbose
javac -verbose FileOrganizerUtility.java
```

### Le répertoire de sortie existe déjà

L'utilitaire va créer les fichiers même si le répertoire existe. Pour repartir de zéro :

```bash
rm -rf ./nexusai-analytics
./organize-files.sh
```

---

## 📚 Ressources

- **Code source** : Tous les artifacts créés dans Claude
- **Documentation** : Voir `docs/` après génération
- **Aide** : Consultez `REMAINING-TASKS.md` pour savoir quoi faire ensuite

---

## 🎯 Prochaines étapes

Après avoir organisé les fichiers :

1. ✅ **Vérifier** que la structure est correcte
2. ✅ **Copier** le code des artifacts dans les fichiers
3. ✅ **Compiler** le projet (`mvn clean install`)
4. ✅ **Lancer** les tests (`mvn test`)
5. ✅ **Démarrer** les services (`docker-compose up`)
6. ✅ **Consulter** `REMAINING-TASKS.md` pour les tâches restantes

---

## 💡 Conseils

- **Utilisez le Makefile** : Il contient toutes les commandes utiles
- **Travaillez par module** : Commencez par `analytics-core`, puis `analytics-api`, etc.
- **Lancez les tests souvent** : `make test` après chaque modification
- **Consultez les logs** : `make docker-logs` en cas de problème
- **Utilisez le coverage** : `mvn jacoco:report` pour voir le coverage des tests

---

## ✅ Checklist de démarrage

- [ ] Java 21+ installé
- [ ] Maven 3.9+ installé
- [ ] Docker & Docker Compose installés
- [ ] Script d'organisation exécuté avec succès
- [ ] Structure créée et vérifiée
- [ ] Code copié des artifacts
- [ ] Projet compile sans erreur
- [ ] Tests passent (au moins les tests de base)
- [ ] Services Docker démarrés
- [ ] API accessible sur http://localhost:8080

**Vous êtes prêt à développer ! 🚀**
