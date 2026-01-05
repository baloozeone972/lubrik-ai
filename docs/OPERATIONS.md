# Guide d'Exploitation Production - NexusAI

## 1. Prérequis Infrastructure

### 1.1 Configuration matérielle recommandée

#### Serveur Application (par instance)
| Ressource | Minimum | Recommandé | Production |
|-----------|---------|------------|------------|
| CPU | 4 cores | 8 cores | 16 cores |
| RAM | 8 GB | 16 GB | 32 GB |
| Stockage | 50 GB SSD | 100 GB SSD | 200 GB NVMe |

#### Base de données PostgreSQL
| Ressource | Minimum | Recommandé | Production |
|-----------|---------|------------|------------|
| CPU | 4 cores | 8 cores | 16 cores |
| RAM | 16 GB | 32 GB | 64 GB |
| Stockage | 100 GB SSD | 500 GB SSD | 1 TB NVMe |
| IOPS | 3000 | 10000 | 20000+ |

#### Redis Cache
| Ressource | Minimum | Recommandé |
|-----------|---------|------------|
| RAM | 4 GB | 16 GB |
| CPU | 2 cores | 4 cores |

#### Kafka Cluster (3 nodes minimum)
| Ressource | Par Node |
|-----------|----------|
| CPU | 4 cores |
| RAM | 8 GB |
| Stockage | 500 GB SSD |

#### MinIO (Object Storage)
| Ressource | Minimum | Selon usage |
|-----------|---------|-------------|
| Stockage | 500 GB | Selon besoins |
| Réseau | 1 Gbps | 10 Gbps |

#### Ollama (GPU Server)
| Ressource | Minimum | Recommandé |
|-----------|---------|------------|
| GPU | RTX 3080 | A100 / H100 |
| VRAM | 12 GB | 40+ GB |
| RAM | 32 GB | 64 GB |

### 1.2 Topologie réseau recommandée

```
                    ┌─────────────────┐
                    │   Load Balancer │
                    │   (HAProxy/ALB) │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
        ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐
        │  App #1   │  │  App #2   │  │  App #3   │
        │  :8080    │  │  :8080    │  │  :8080    │
        └─────┬─────┘  └─────┬─────┘  └─────┬─────┘
              │              │              │
              └──────────────┼──────────────┘
                             │
    ┌────────────────────────┼────────────────────────┐
    │                        │                        │
┌───▼───┐  ┌───▼───┐  ┌──────▼──────┐  ┌───▼───┐  ┌───▼───┐
│Postgres│  │ Redis │  │    Kafka    │  │ MinIO │  │Ollama │
│Primary │  │Cluster│  │   Cluster   │  │Cluster│  │ GPU   │
└───┬───┘  └───────┘  └─────────────┘  └───────┘  └───────┘
    │
┌───▼───┐
│Postgres│
│Replica │
└───────┘
```

## 2. Déploiement

### 2.1 Préparation de l'environnement

```bash
# 1. Créer l'utilisateur applicatif
sudo useradd -m -s /bin/bash nexusai
sudo usermod -aG docker nexusai

# 2. Créer les répertoires
sudo mkdir -p /opt/nexusai/{app,config,logs,data}
sudo chown -R nexusai:nexusai /opt/nexusai

# 3. Configurer les limites système
cat >> /etc/security/limits.conf << EOF
nexusai soft nofile 65536
nexusai hard nofile 65536
nexusai soft nproc 32768
nexusai hard nproc 32768
EOF
```

### 2.2 Configuration production

Créer `/opt/nexusai/config/application-prod.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 30000

  data:
    redis:
      host: ${REDIS_HOST}
      port: 6379
      password: ${REDIS_PASSWORD}
      lettuce:
        pool:
          min-idle: 5
          max-idle: 20
          max-active: 50

  kafka:
    bootstrap-servers: ${KAFKA_SERVERS}
    producer:
      acks: all
      retries: 3
    consumer:
      auto-offset-reset: earliest
      enable-auto-commit: false

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true

  flyway:
    enabled: true
    baseline-on-migrate: true

nexusai:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 86400000
      refresh-expiration: 604800000

  storage:
    minio:
      endpoint: ${MINIO_ENDPOINT}
      access-key: ${MINIO_ACCESS_KEY}
      secret-key: ${MINIO_SECRET_KEY}
      bucket-name: nexusai-prod

  ai:
    ollama:
      base-url: ${OLLAMA_URL}
      model: llama3
      timeout: 120000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when_authorized
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    root: WARN
    com.nexusai: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /opt/nexusai/logs/nexusai.log
    max-size: 100MB
    max-history: 30

server:
  port: 8080
  tomcat:
    threads:
      max: 200
      min-spare: 20
    max-connections: 10000
    accept-count: 500
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/plain
```

### 2.3 Script de déploiement

Créer `/opt/nexusai/deploy.sh`:

```bash
#!/bin/bash
set -e

APP_NAME="nexusai-api"
APP_DIR="/opt/nexusai/app"
CONFIG_DIR="/opt/nexusai/config"
LOG_DIR="/opt/nexusai/logs"
JAR_FILE="$APP_DIR/$APP_NAME.jar"
PID_FILE="$APP_DIR/$APP_NAME.pid"

# Charger les variables d'environnement
source /opt/nexusai/config/env.sh

# Arrêter l'ancienne instance
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if ps -p $OLD_PID > /dev/null 2>&1; then
        echo "Stopping old instance (PID: $OLD_PID)..."
        kill -TERM $OLD_PID
        sleep 10
        if ps -p $OLD_PID > /dev/null 2>&1; then
            kill -9 $OLD_PID
        fi
    fi
    rm -f "$PID_FILE"
fi

# Démarrer la nouvelle instance
echo "Starting $APP_NAME..."
java \
    -server \
    -Xms4g \
    -Xmx8g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=$LOG_DIR/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod \
    -Dspring.config.additional-location=file:$CONFIG_DIR/ \
    -jar $JAR_FILE \
    >> $LOG_DIR/startup.log 2>&1 &

NEW_PID=$!
echo $NEW_PID > "$PID_FILE"
echo "Started $APP_NAME with PID: $NEW_PID"

# Attendre le démarrage
echo "Waiting for application to start..."
for i in {1..60}; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null; then
        echo "Application started successfully!"
        exit 0
    fi
    sleep 2
done

echo "Application failed to start!"
exit 1
```

### 2.4 Service Systemd

Créer `/etc/systemd/system/nexusai.service`:

```ini
[Unit]
Description=NexusAI Application
After=network.target postgresql.service redis.service

[Service]
Type=forking
User=nexusai
Group=nexusai
WorkingDirectory=/opt/nexusai/app
Environment="JAVA_HOME=/usr/lib/jvm/java-21-openjdk"
ExecStart=/opt/nexusai/deploy.sh
ExecStop=/bin/kill -TERM $MAINPID
PIDFile=/opt/nexusai/app/nexusai-api.pid
Restart=on-failure
RestartSec=30
TimeoutStartSec=120
TimeoutStopSec=30

# Limites
LimitNOFILE=65536
LimitNPROC=32768

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable nexusai
sudo systemctl start nexusai
```

## 3. Monitoring

### 3.1 Métriques clés à surveiller

#### Application
- `jvm_memory_used_bytes` - Utilisation mémoire JVM
- `jvm_gc_pause_seconds` - Temps de pause GC
- `http_server_requests_seconds` - Latence des requêtes
- `hikaricp_connections_active` - Connexions DB actives
- `kafka_consumer_records_lag` - Lag des consumers Kafka

#### Infrastructure
- CPU utilization > 80%
- Memory usage > 85%
- Disk usage > 80%
- Network errors
- Connection pool exhaustion

### 3.2 Configuration Prometheus

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'nexusai'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app1:8080', 'app2:8080', 'app3:8080']

  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']

  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']
```

### 3.3 Alertes recommandées

```yaml
# alerting_rules.yml
groups:
  - name: nexusai
    rules:
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage detected"

      - alert: HighLatency
        expr: histogram_quantile(0.95, http_server_requests_seconds_bucket) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High API latency detected"

      - alert: DatabaseConnectionExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool nearly exhausted"

      - alert: KafkaLag
        expr: kafka_consumer_records_lag > 10000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Kafka consumer lag detected"
```

## 4. Sécurité

### 4.1 Configuration SSL/TLS

```bash
# Générer un certificat (production: utiliser Let's Encrypt ou CA commercial)
keytool -genkeypair -alias nexusai -keyalg RSA -keysize 2048 \
    -storetype PKCS12 -keystore nexusai.p12 -validity 365 \
    -dname "CN=nexusai.fr,O=NexusAI,L=Paris,C=FR"
```

```yaml
# application-prod.yml
server:
  ssl:
    key-store: /opt/nexusai/config/nexusai.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    enabled: true
  port: 8443
```

### 4.2 Sécurité réseau

```bash
# Firewall (ufw)
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 443/tcp     # HTTPS
sudo ufw allow from 10.0.0.0/8 to any port 8080  # App interne
sudo ufw enable
```

### 4.3 Rotation des secrets

```bash
#!/bin/bash
# rotate_secrets.sh

# Générer nouveau secret JWT
NEW_JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')

# Mettre à jour dans le gestionnaire de secrets (ex: Vault)
vault kv put secret/nexusai/prod jwt_secret="$NEW_JWT_SECRET"

# Redémarrer l'application pour appliquer
systemctl restart nexusai
```

## 5. Backup et Restauration

### 5.1 Backup PostgreSQL

```bash
#!/bin/bash
# backup_postgres.sh

BACKUP_DIR="/opt/nexusai/backups/postgres"
DATE=$(date +%Y%m%d_%H%M%S)
DB_HOST="postgres.internal"
DB_NAME="nexusai_auth"

# Backup
pg_dump -h $DB_HOST -U nexusai -Fc $DB_NAME > $BACKUP_DIR/nexusai_$DATE.dump

# Compression et envoi S3
gzip $BACKUP_DIR/nexusai_$DATE.dump
aws s3 cp $BACKUP_DIR/nexusai_$DATE.dump.gz s3://nexusai-backups/postgres/

# Nettoyage local (garder 7 jours)
find $BACKUP_DIR -name "*.dump.gz" -mtime +7 -delete
```

### 5.2 Backup Redis

```bash
#!/bin/bash
# backup_redis.sh

BACKUP_DIR="/opt/nexusai/backups/redis"
DATE=$(date +%Y%m%d_%H%M%S)

# Trigger BGSAVE
redis-cli -h redis.internal -a $REDIS_PASSWORD BGSAVE

# Attendre la fin
while [ $(redis-cli -h redis.internal -a $REDIS_PASSWORD LASTSAVE) == $(redis-cli -h redis.internal -a $REDIS_PASSWORD LASTSAVE) ]; do
    sleep 1
done

# Copier le dump
scp redis.internal:/var/lib/redis/dump.rdb $BACKUP_DIR/redis_$DATE.rdb
gzip $BACKUP_DIR/redis_$DATE.rdb
aws s3 cp $BACKUP_DIR/redis_$DATE.rdb.gz s3://nexusai-backups/redis/
```

### 5.3 Restauration

```bash
# Restaurer PostgreSQL
gunzip -c nexusai_backup.dump.gz | pg_restore -h $DB_HOST -U nexusai -d nexusai_auth -c

# Restaurer Redis
redis-cli -h redis.internal -a $REDIS_PASSWORD SHUTDOWN SAVE
cp redis_backup.rdb /var/lib/redis/dump.rdb
systemctl start redis
```

## 6. Troubleshooting

### 6.1 Problèmes courants

| Symptôme | Cause probable | Solution |
|----------|----------------|----------|
| OOM Errors | Heap trop petite | Augmenter -Xmx |
| Connexions DB épuisées | Pool trop petit | Augmenter pool size |
| Latence élevée | GC pause | Tuner G1GC |
| Kafka lag | Consumer lent | Scale consumers |
| 502 Bad Gateway | App down | Vérifier logs/health |

### 6.2 Commandes de diagnostic

```bash
# Vérifier la santé
curl http://localhost:8080/actuator/health | jq

# Thread dump
jcmd $(pgrep -f nexusai) Thread.print > thread_dump.txt

# Heap dump
jcmd $(pgrep -f nexusai) GC.heap_dump /tmp/heap.hprof

# Logs en temps réel
tail -f /opt/nexusai/logs/nexusai.log | grep -E "(ERROR|WARN)"

# Métriques JVM
curl http://localhost:8080/actuator/metrics/jvm.memory.used | jq
```

### 6.3 Procédure d'escalade

1. **Niveau 1** - Support technique
   - Vérifier les logs et métriques
   - Redémarrer si nécessaire

2. **Niveau 2** - DevOps
   - Analyse approfondie
   - Scaling / rollback

3. **Niveau 3** - Développeurs
   - Debug du code
   - Hotfix si nécessaire

## 7. Maintenance

### 7.1 Mises à jour

```bash
#!/bin/bash
# update.sh

# 1. Backup avant mise à jour
./backup_postgres.sh
./backup_redis.sh

# 2. Télécharger nouvelle version
wget https://artifacts.nexusai.fr/releases/nexusai-api-${VERSION}.jar -O /tmp/nexusai-api.jar

# 3. Valider le JAR
sha256sum -c /tmp/nexusai-api.jar.sha256

# 4. Déployer (rolling update)
mv /tmp/nexusai-api.jar /opt/nexusai/app/nexusai-api.jar
systemctl restart nexusai

# 5. Vérifier la santé
./health_check.sh
```

### 7.2 Maintenance planifiée

| Tâche | Fréquence | Fenêtre |
|-------|-----------|---------|
| Backup complet | Quotidien | 02:00-04:00 |
| Vacuum PostgreSQL | Hebdomadaire | Dimanche 03:00 |
| Rotation logs | Quotidien | 00:00 |
| Mise à jour sécurité | Mensuel | Premier samedi |
| Revue capacité | Trimestriel | - |

## 8. Contacts

| Rôle | Contact | Disponibilité |
|------|---------|---------------|
| Astreinte Ops | ops@nexusai.fr | 24/7 |
| Lead Dev | dev@nexusai.fr | 9h-18h |
| Security | security@nexusai.fr | 24/7 |
| DBA | dba@nexusai.fr | 9h-18h |
