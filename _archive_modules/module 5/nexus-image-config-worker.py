"""
═══════════════════════════════════════════════════════════════════════════
MODULE 5 : CONFIGURATION & WORKER PYTHON
═══════════════════════════════════════════════════════════════════════════
"""

# ═══════════════════════════════════════════════════════════════════════════
# 5. APPLICATION.YML - CONFIGURATION SPRING BOOT
# ═══════════════════════════════════════════════════════════════════════════

"""
# application.yml

spring:
  application:
    name: nexus-image-generation
  
  datasource:
    url: jdbc:postgresql://localhost:5432/nexusai
    username: nexusai
    password: nexusai123
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: true
  
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.type.mapping: imageGenerationRequestedEvent:com.nexusai.image.domain.event.ImageGenerationRequestedEvent
    
    consumer:
      group-id: image-generation-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.nexusai.image.domain.event
        spring.json.type.mapping: imageGenerationCompletedEvent:com.nexusai.image.domain.event.ImageGenerationCompletedEvent

# Configuration AWS S3
aws:
  s3:
    bucket-name: nexusai-images
    region: eu-west-1
    access-key: ${AWS_ACCESS_KEY}
    secret-key: ${AWS_SECRET_KEY}
    endpoint: ${AWS_S3_ENDPOINT:}  # Pour MinIO en local

# URLs des autres services
services:
  payment:
    url: http://localhost:8081
  moderation:
    url: http://localhost:8089

# Configuration Actuator (Monitoring)
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
  endpoint:
    health:
      show-details: always

# Configuration OpenAPI
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method

# Configuration logging
logging:
  level:
    root: INFO
    com.nexusai: DEBUG
    org.springframework.kafka: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Configuration serveur
server:
  port: 8085
  compression:
    enabled: true
  http2:
    enabled: true
"""

# ═══════════════════════════════════════════════════════════════════════════
# 6. CONFIGURATION JAVA
# ═══════════════════════════════════════════════════════════════════════════

"""
package com.nexusai.image.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Configuration Spring Boot principale
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 2025-01-01
 */
@Configuration
public class AppConfig {

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    /**
     * Bean RestTemplate pour les appels HTTP vers autres services
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Bean S3Client pour le stockage des images
     */
    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            ));

        // Configuration pour MinIO en local
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint))
                   .forcePathStyle(true);
        }

        return builder.build();
    }
}
"""

# ═══════════════════════════════════════════════════════════════════════════
# 7. WORKER PYTHON - GÉNÉRATION D'IMAGES
# ═══════════════════════════════════════════════════════════════════════════

# requirements.txt
"""
torch==2.1.0
diffusers==0.25.0
transformers==4.35.0
accelerate==0.25.0
kafka-python==2.0.2
Pillow==10.1.0
boto3==1.29.7
requests==2.31.0
python-dotenv==1.0.0
psycopg2-binary==2.9.9
"""

# worker.py
"""
Worker Python pour la génération d'images via Stable Diffusion

Ce worker:
1. Consomme les messages Kafka
2. Génère les images avec Stable Diffusion
3. Upload vers S3
4. Met à jour la base de données
5. Notifie via événement Kafka

@author NexusAI Team
@version 1.0
@since 2025-01-01
"""

import os
import json
import time
import logging
from io import BytesIO
from datetime import datetime
from typing import Dict, Any

import torch
from diffusers import StableDiffusionPipeline, DPMSolverMultistepScheduler
from PIL import Image
import boto3
from kafka import KafkaConsumer, KafkaProducer
import psycopg2
from psycopg2.extras import DictCursor
from dotenv import load_dotenv

# Configuration logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Chargement variables d'environnement
load_dotenv()


class ImageGenerationWorker:
    """
    Worker de génération d'images
    
    Attributes:
        pipe: Pipeline Stable Diffusion
        s3_client: Client AWS S3
        db_conn: Connexion PostgreSQL
        kafka_producer: Producer Kafka pour les événements
    """
    
    def __init__(self):
        """Initialise le worker et tous ses composants"""
        logger.info("Initialisation du worker de génération d'images")
        
        # Configuration
        self.model_id = "runwayml/stable-diffusion-v1-5"
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.s3_bucket = os.getenv("AWS_S3_BUCKET", "nexusai-images")
        
        # Initialisation des composants
        self._init_stable_diffusion()
        self._init_s3_client()
        self._init_database()
        self._init_kafka()
        
        logger.info(f"Worker initialisé sur device: {self.device}")

    def _init_stable_diffusion(self):
        """Initialise le pipeline Stable Diffusion"""
        logger.info(f"Chargement du modèle {self.model_id}...")
        
        self.pipe = StableDiffusionPipeline.from_pretrained(
            self.model_id,
            torch_dtype=torch.float16 if self.device == "cuda" else torch.float32,
            safety_checker=None,  # Désactivé car modération côté API
            requires_safety_checker=False
        ).to(self.device)
        
        # Optimisations
        self.pipe.scheduler = DPMSolverMultistepScheduler.from_config(
            self.pipe.scheduler.config
        )
        
        if self.device == "cuda":
            self.pipe.enable_attention_slicing()
            self.pipe.enable_vae_slicing()
        
        logger.info("Modèle Stable Diffusion chargé avec succès")

    def _init_s3_client(self):
        """Initialise le client S3"""
        self.s3_client = boto3.client(
            's3',
            aws_access_key_id=os.getenv("AWS_ACCESS_KEY"),
            aws_secret_access_key=os.getenv("AWS_SECRET_KEY"),
            endpoint_url=os.getenv("AWS_S3_ENDPOINT"),
            region_name=os.getenv("AWS_S3_REGION", "eu-west-1")
        )
        logger.info("Client S3 initialisé")

    def _init_database(self):
        """Initialise la connexion PostgreSQL"""
        self.db_conn = psycopg2.connect(
            host=os.getenv("DB_HOST", "localhost"),
            port=os.getenv("DB_PORT", "5432"),
            database=os.getenv("DB_NAME", "nexusai"),
            user=os.getenv("DB_USER", "nexusai"),
            password=os.getenv("DB_PASSWORD", "nexusai123")
        )
        self.db_conn.autocommit = True
        logger.info("Connexion PostgreSQL établie")

    def _init_kafka(self):
        """Initialise le consumer et producer Kafka"""
        kafka_servers = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
        
        self.kafka_consumer = KafkaConsumer(
            'image.generation.requests',
            bootstrap_servers=kafka_servers,
            value_deserializer=lambda m: json.loads(m.decode('utf-8')),
            group_id='image-generation-workers',
            auto_offset_reset='earliest',
            enable_auto_commit=True
        )
        
        self.kafka_producer = KafkaProducer(
            bootstrap_servers=kafka_servers,
            value_serializer=lambda m: json.dumps(m).encode('utf-8')
        )
        
        logger.info("Kafka consumer/producer initialisés")

    def generate_image(self, request: Dict[str, Any]) -> Image.Image:
        """
        Génère une image avec Stable Diffusion
        
        Args:
            request: Requête de génération contenant prompt, résolution, etc.
            
        Returns:
            Image PIL générée
        """
        prompt = request['prompt']
        negative_prompt = request.get('negativePrompt', '')
        resolution = request.get('resolution', '1024x1024')
        seed = request.get('seed')
        
        # Parse résolution
        width, height = map(int, resolution.split('x'))
        
        # Paramètres de génération
        generator = None
        if seed is not None:
            generator = torch.Generator(device=self.device).manual_seed(seed)
        
        logger.info(f"Génération image: {prompt[:50]}... [{resolution}]")
        
        # Génération
        start_time = time.time()
        
        output = self.pipe(
            prompt=prompt,
            negative_prompt=negative_prompt,
            width=width,
            height=height,
            num_inference_steps=50,
            guidance_scale=7.5,
            generator=generator
        )
        
        generation_time = int(time.time() - start_time)
        logger.info(f"Image générée en {generation_time} secondes")
        
        return output.images[0], generation_time

    def create_thumbnail(self, image: Image.Image, size: tuple = (256, 256)) -> Image.Image:
        """
        Crée une thumbnail de l'image
        
        Args:
            image: Image PIL originale
            size: Taille de la thumbnail
            
        Returns:
            Thumbnail PIL
        """
        thumbnail = image.copy()
        thumbnail.thumbnail(size, Image.Resampling.LANCZOS)
        return thumbnail

    def upload_to_s3(self, image: Image.Image, image_id: str, is_thumbnail: bool = False) -> str:
        """
        Upload une image vers S3
        
        Args:
            image: Image PIL à uploader
            image_id: ID de l'image
            is_thumbnail: True si c'est une thumbnail
            
        Returns:
            URL publique de l'image
        """
        # Définir le chemin
        prefix = "thumbnails" if is_thumbnail else "images"
        suffix = "_thumb" if is_thumbnail else ""
        key = f"{prefix}/{image_id[:2]}/{image_id}{suffix}.png"
        
        # Convertir l'image en bytes
        buffer = BytesIO()
        image.save(buffer, format='PNG')
        buffer.seek(0)
        
        # Upload vers S3
        self.s3_client.upload_fileobj(
            buffer,
            self.s3_bucket,
            key,
            ExtraArgs={
                'ContentType': 'image/png',
                'ACL': 'public-read'
            }
        )
        
        # Générer URL publique
        url = f"https://{self.s3_bucket}.s3.amazonaws.com/{key}"
        logger.info(f"Image uploadée: {key}")
        
        return url

    def update_database(self, image_id: str, status: str, storage_url: str = None,
                       thumbnail_url: str = None, generation_time: int = None):
        """
        Met à jour le statut de l'image dans la base de données
        
        Args:
            image_id: ID de l'image
            status: Nouveau statut (PROCESSING, COMPLETED, FAILED)
            storage_url: URL de l'image (si COMPLETED)
            thumbnail_url: URL de la thumbnail (si COMPLETED)
            generation_time: Temps de génération en secondes
        """
        cursor = self.db_conn.cursor()
        
        if status == 'COMPLETED':
            query = """
                UPDATE generated_images 
                SET status = %s, 
                    storage_url = %s, 
                    thumbnail_url = %s,
                    generation_time_seconds = %s,
                    completed_at = %s
                WHERE id = %s
            """
            cursor.execute(query, (
                status, 
                storage_url, 
                thumbnail_url, 
                generation_time,
                datetime.now(),
                image_id
            ))
        else:
            query = "UPDATE generated_images SET status = %s WHERE id = %s"
            cursor.execute(query, (status, image_id))
        
        cursor.close()
        logger.info(f"Base de données mise à jour: {image_id} -> {status}")

    def publish_completion_event(self, image_id: str, user_id: str, status: str):
        """
        Publie un événement de complétion dans Kafka
        
        Args:
            image_id: ID de l'image
            user_id: ID de l'utilisateur
            status: Statut final (COMPLETED ou FAILED)
        """
        event = {
            'imageId': image_id,
            'userId': user_id,
            'status': status,
            'timestamp': datetime.now().isoformat()
        }
        
        self.kafka_producer.send(
            'image.generation.completed',
            key=image_id,
            value=event
        )
        
        logger.info(f"Événement de complétion publié: {image_id}")

    def process_request(self, request: Dict[str, Any]):
        """
        Traite une requête de génération complète
        
        Args:
            request: Requête Kafka contenant tous les paramètres
        """
        image_id = request['imageId']
        user_id = request['userId']
        
        logger.info(f"Traitement de la requête {image_id}")
        
        try:
            # 1. Mettre à jour le statut: PROCESSING
            self.update_database(image_id, 'PROCESSING')
            
            # 2. Générer l'image
            image, generation_time = self.generate_image(request)
            
            # 3. Créer la thumbnail
            thumbnail = self.create_thumbnail(image)
            
            # 4. Upload vers S3
            storage_url = self.upload_to_s3(image, image_id, is_thumbnail=False)
            thumbnail_url = self.upload_to_s3(thumbnail, image_id, is_thumbnail=True)
            
            # 5. Mettre à jour la base de données: COMPLETED
            self.update_database(
                image_id, 
                'COMPLETED', 
                storage_url, 
                thumbnail_url,
                generation_time
            )
            
            # 6. Publier événement de complétion
            self.publish_completion_event(image_id, user_id, 'COMPLETED')
            
            logger.info(f"Requête {image_id} traitée avec succès")
            
        except Exception as e:
            logger.error(f"Erreur lors du traitement de {image_id}: {str(e)}", exc_info=True)
            
            # Mettre à jour le statut: FAILED
            self.update_database(image_id, 'FAILED')
            self.publish_completion_event(image_id, user_id, 'FAILED')

    def run(self):
        """
        Boucle principale du worker
        Consomme les messages Kafka en continu
        """
        logger.info("Worker démarré, en attente de requêtes...")
        
        try:
            for message in self.kafka_consumer:
                request = message.value
                self.process_request(request)
                
        except KeyboardInterrupt:
            logger.info("Arrêt du worker demandé")
        except Exception as e:
            logger.error(f"Erreur fatale: {str(e)}", exc_info=True)
        finally:
            self.cleanup()

    def cleanup(self):
        """Nettoie les ressources avant l'arrêt"""
        logger.info("Nettoyage des ressources...")
        
        if hasattr(self, 'kafka_consumer'):
            self.kafka_consumer.close()
        
        if hasattr(self, 'kafka_producer'):
            self.kafka_producer.close()
        
        if hasattr(self, 'db_conn'):
            self.db_conn.close()
        
        logger.info("Worker arrêté proprement")


if __name__ == "__main__":
    worker = ImageGenerationWorker()
    worker.run()

# ═══════════════════════════════════════════════════════════════════════════
# 8. DOCKERFILE WORKER
# ═══════════════════════════════════════════════════════════════════════════

"""
FROM python:3.11-slim

WORKDIR /app

# Installation dépendances système
RUN apt-get update && apt-get install -y \
    git \
    && rm -rf /var/lib/apt/lists/*

# Installation dépendances Python
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copie du code
COPY worker.py .

# Variables d'environnement par défaut
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV DB_HOST=postgres
ENV DB_PORT=5432
ENV DB_NAME=nexusai
ENV AWS_S3_BUCKET=nexusai-images

# Commande de démarrage
CMD ["python", "worker.py"]
"""

# ═══════════════════════════════════════════════════════════════════════════
# 9. DOCKER-COMPOSE.YML
# ═══════════════════════════════════════════════════════════════════════════

"""
version: '3.8'

services:
  # API Java
  image-api:
    build:
      context: ./nexus-image-api
      dockerfile: Dockerfile
    ports:
      - "8085:8085"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/nexusai
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      AWS_S3_ENDPOINT: http://minio:9000
    depends_on:
      - postgres
      - kafka
      - minio
    networks:
      - nexusai-network

  # Worker Python (peut être scalé)
  image-worker:
    build:
      context: ./nexus-image-worker
      dockerfile: Dockerfile
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      DB_HOST: postgres
      DB_PORT: 5432
      AWS_S3_ENDPOINT: http://minio:9000
      AWS_ACCESS_KEY: minioadmin
      AWS_SECRET_KEY: minioadmin
    depends_on:
      - postgres
      - kafka
      - minio
    deploy:
      replicas: 2  # 2 workers en parallèle
    networks:
      - nexusai-network

  # PostgreSQL
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: nexusai
      POSTGRES_USER: nexusai
      POSTGRES_PASSWORD: nexusai123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - nexusai-network

  # Kafka
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    depends_on:
      - zookeeper
    networks:
      - nexusai-network

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    networks:
      - nexusai-network

  # MinIO (S3-compatible)
  minio:
    image: minio/minio:latest
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    command: server /data --console-address ":9001"
    volumes:
      - minio_data:/data
    networks:
      - nexusai-network

volumes:
  postgres_data:
  minio_data:

networks:
  nexusai-network:
    driver: bridge
"""

logger.info("Configuration et worker chargés")
