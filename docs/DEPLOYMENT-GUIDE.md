# 🚀 Guide de Déploiement Production - NexusAI

## Prérequis

### Serveur
- **OS**: Ubuntu 22.04 LTS ou supérieur
- **CPU**: 4 cores minimum (8 recommandé)
- **RAM**: 8 GB minimum (16 GB recommandé)
- **Disk**: 50 GB minimum (SSD recommandé)
- **Network**: Connexion stable, IP publique

### Logiciels
- Docker 24.0+
- Docker Compose 2.0+
- Git
- Certbot (pour SSL)

### Services externes
- OpenAI API key
- Anthropic API key
- Stripe account (Live keys)
- Nom de domaine configuré

---

## Étape 1 : Préparation du serveur

```bash
# Connexion SSH
ssh user@your-server-ip

# Mise à jour système
sudo apt update && sudo apt upgrade -y

# Installation Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Installation Docker Compose
sudo apt install docker-compose-plugin -y

# Créer utilisateur nexusai
sudo useradd -m -s /bin/bash nexusai
sudo usermod -aG docker nexusai
sudo su - nexusai
```

---

## Étape 2 : Installation du projet

```bash
# Se placer dans /opt
cd /opt
sudo mkdir nexusai
sudo chown nexusai:nexusai nexusai
cd nexusai

# Copier les fichiers (via scp ou git)
scp nexusai-complete-package.tar.gz nexusai@your-server:/opt/nexusai/
tar -xzf nexusai-complete-package.tar.gz
```

---

## Étape 3 : Configuration

```bash
cd /opt/nexusai/NEXUSAI-COMPLETE-PACKAGE

# Copier .env
cp .env.example .env

# Éditer avec vos valeurs
nano .env
```

### Variables essentielles :

```env
# Database
POSTGRES_PASSWORD=votre_password_securise_ici

# Redis
REDIS_PASSWORD=votre_redis_password

# JWT (générer avec: openssl rand -base64 64)
JWT_SECRET=votre_jwt_secret_tres_long

# AI Services
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...

# Storage
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=votre_minio_password

# Stripe
STRIPE_API_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_PRICE_STANDARD=price_...
STRIPE_PRICE_PREMIUM=price_...
STRIPE_PRICE_VIP=price_...

# Frontend
FRONTEND_URL=https://nexusai.app
FRONTEND_API_URL=https://api.nexusai.app

# Monitoring
GRAFANA_ADMIN_PASSWORD=votre_grafana_password
```

---

## Étape 4 : Configuration SSL (Let's Encrypt)

```bash
# Installer Certbot
sudo apt install certbot python3-certbot-nginx -y

# Obtenir certificat
sudo certbot certonly --nginx -d nexusai.app -d www.nexusai.app

# Copier les certificats
sudo mkdir -p ssl
sudo cp /etc/letsencrypt/live/nexusai.app/fullchain.pem ssl/
sudo cp /etc/letsencrypt/live/nexusai.app/privkey.pem ssl/
sudo chown -R nexusai:nexusai ssl/

# Auto-renouvellement
sudo certbot renew --dry-run
```

---

## Étape 5 : Démarrage des services

```bash
cd /opt/nexusai/NEXUSAI-COMPLETE-PACKAGE

# Démarrer tous les services
docker-compose -f docker-compose.prod.yml up -d

# Vérifier les services
docker-compose -f docker-compose.prod.yml ps

# Voir les logs
docker-compose -f docker-compose.prod.yml logs -f
```

### Services démarrés :
- ✅ PostgreSQL (port 5432)
- ✅ Redis (port 6379)
- ✅ MinIO (ports 9000, 9001)
- ✅ Kafka + Zookeeper
- ✅ NexusAI API (port 8080)
- ✅ NexusAI Frontend (port 3000)
- ✅ Nginx (ports 80, 443)
- ✅ Prometheus (port 9090)
- ✅ Grafana (port 3001)
- ✅ Loki + Promtail

---

## Étape 6 : Vérification santé

```bash
# API Health
curl http://localhost:8080/api/actuator/health
# Devrait retourner: {"status":"UP"}

# Frontend
curl http://localhost:3000
# Devrait retourner le HTML

# Nginx
curl https://nexusai.app
# Devrait rediriger vers HTTPS

# Prometheus
curl http://localhost:9090/-/healthy
# Devrait retourner: Prometheus is Healthy.
```

---

## Étape 7 : Configuration Monitoring

### Grafana

```bash
# Accéder à Grafana
open http://your-server-ip:3001

# Login
User: admin
Pass: (depuis .env GRAFANA_ADMIN_PASSWORD)
```

### Importer dashboards :

1. **JVM Micrometer** (ID: 4701)
   - Settings → Dashboards → Import → 4701

2. **Node Exporter** (ID: 1860)
   - Settings → Dashboards → Import → 1860

3. **PostgreSQL** (ID: 893)
   - Settings → Dashboards → Import → 893

### Ajouter Loki datasource :
- URL: http://loki:3100
- Save & Test

---

## Étape 8 : Configuration Stripe Webhooks

```bash
# URL du webhook à configurer dans Stripe Dashboard
https://nexusai.app/api/v1/payments/webhook

# Events à écouter :
- checkout.session.completed
- customer.subscription.created
- customer.subscription.updated
- customer.subscription.deleted
- invoice.payment_succeeded
- invoice.payment_failed
```

---

## Étape 9 : Tests de production

```bash
# Test AI Engine
curl -X POST https://nexusai.app/api/v1/conversations/{id}/messages \
  -H "Authorization: Bearer YOUR_JWT" \
  -H "Content-Type: application/json" \
  -d '{"content":"Hello!"}'

# Test Media Upload
curl -X POST https://nexusai.app/api/v1/media/upload \
  -H "Authorization: Bearer YOUR_JWT" \
  -F "file=@test.jpg" \
  -F "category=IMAGE"

# Test Payment
curl https://nexusai.app/api/v1/payments/plans
```

---

## Étape 10 : Sauvegarde

### Sauvegarder PostgreSQL

```bash
# Backup manuel
docker exec nexusai-postgres pg_dump -U nexusai nexusai > backup-$(date +%Y%m%d).sql

# Restaurer
cat backup-20260105.sql | docker exec -i nexusai-postgres psql -U nexusai nexusai
```

### Sauvegarder MinIO

```bash
# Backup données MinIO
sudo tar -czf minio-backup-$(date +%Y%m%d).tar.gz \
  /var/lib/docker/volumes/nexusai_minio-data
```

### Backup automatique (cron)

```bash
# Éditer crontab
crontab -e

# Ajouter backup quotidien à 2h du matin
0 2 * * * /opt/nexusai/scripts/backup.sh
```

---

## Commandes utiles

### Gestion services

```bash
# Voir les logs
docker-compose -f docker-compose.prod.yml logs -f nexusai-api

# Restart un service
docker-compose -f docker-compose.prod.yml restart nexusai-api

# Arrêter tout
docker-compose -f docker-compose.prod.yml down

# Démarrer tout
docker-compose -f docker-compose.prod.yml up -d

# Rebuild un service
docker-compose -f docker-compose.prod.yml up -d --build nexusai-api
```

### Monitoring

```bash
# Voir métriques
docker stats

# Voir espace disque
df -h

# Voir logs système
journalctl -u docker -f
```

---

## Troubleshooting

### API ne démarre pas

```bash
# Vérifier logs
docker-compose -f docker-compose.prod.yml logs nexusai-api

# Vérifier variables d'environnement
docker-compose -f docker-compose.prod.yml config

# Restart
docker-compose -f docker-compose.prod.yml restart nexusai-api
```

### Database connection error

```bash
# Vérifier PostgreSQL
docker-compose -f docker-compose.prod.yml logs postgres

# Tester connexion
docker exec -it nexusai-postgres psql -U nexusai -d nexusai
```

### Mémoire insuffisante

```bash
# Ajuster limites dans docker-compose.prod.yml
services:
  nexusai-api:
    deploy:
      resources:
        limits:
          memory: 4G  # Augmenter si nécessaire
```

---

## Mise à jour

```bash
# Pull nouvelle version
git pull origin main

# Rebuild
docker-compose -f docker-compose.prod.yml build

# Redémarrer avec zero-downtime
docker-compose -f docker-compose.prod.yml up -d --no-deps nexusai-api
```

---

## Sécurité Post-Installation

### Firewall

```bash
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
```

### Fail2ban

```bash
sudo apt install fail2ban
sudo systemctl enable fail2ban
```

### Monitoring SSL

```bash
# Vérifier expiration
sudo certbot certificates

# Renouveler
sudo certbot renew
```

---

## Support

**En cas de problème** :
1. Vérifier les logs : `docker-compose logs -f`
2. Vérifier health : `curl /actuator/health`
3. Vérifier monitoring : Grafana dashboards
4. GitHub Issues pour bugs

**Déploiement réussi !** 🎉

Votre plateforme NexusAI est maintenant en production sur https://nexusai.app
