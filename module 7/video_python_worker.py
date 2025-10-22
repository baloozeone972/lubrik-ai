"""
Worker Python pour la génération de vidéos NexusAI.

Ce worker écoute le topic Kafka 'video.generation.requests' et traite
les demandes de génération vidéo à travers les 7 phases du pipeline:

1. SCRIPT_GENERATION: Génération du scénario avec GPT-4
2. ASSET_GENERATION: Génération des images et audio en parallèle
3. COMPOSITING: Composition des scènes
4. RENDERING: Rendu vidéo
5. ENCODING: Encodage final
6. FINALIZATION: Upload S3 et finalisation

Auteur: NexusAI Team
Version: 1.0
Date: 2025-01
"""

import os
import sys
import json
import time
import uuid
import logging
from typing import Dict, List, Optional
from dataclasses import dataclass
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed

# Imports externes
import boto3
import openai
from kafka import KafkaConsumer, KafkaProducer
from kafka.errors import KafkaError
import requests
import subprocess
from PIL import Image
import numpy as np

# Configuration du logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('/var/log/video-worker.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)


@dataclass
class VideoRequest:
    """Classe de données représentant une requête de génération vidéo."""
    video_id: str
    user_id: str
    companion_id: Optional[str]
    prompt: str
    duration_seconds: int
    quality: str
    resolution: str
    frame_rate: int
    visual_style: Optional[str]
    music_style: Optional[str]
    include_elements: Optional[List[str]]
    timestamp: int


@dataclass
class VideoConfig:
    """Configuration du worker vidéo."""
    kafka_bootstrap_servers: str
    kafka_topic_requests: str
    kafka_topic_events: str
    openai_api_key: str
    elevenlabs_api_key: str
    aws_access_key: str
    aws_secret_key: str
    aws_region: str
    s3_bucket_videos: str
    s3_bucket_assets: str
    temp_dir: str
    worker_id: str

    @classmethod
    def from_env(cls) -> 'VideoConfig':
        """Crée la configuration depuis les variables d'environnement."""
        return cls(
            kafka_bootstrap_servers=os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092'),
            kafka_topic_requests=os.getenv('KAFKA_TOPIC_REQUESTS', 'video.generation.requests'),
            kafka_topic_events=os.getenv('KAFKA_TOPIC_EVENTS', 'video.generation.events'),
            openai_api_key=os.getenv('OPENAI_API_KEY'),
            elevenlabs_api_key=os.getenv('ELEVENLABS_API_KEY'),
            aws_access_key=os.getenv('AWS_ACCESS_KEY_ID'),
            aws_secret_key=os.getenv('AWS_SECRET_ACCESS_KEY'),
            aws_region=os.getenv('AWS_REGION', 'eu-west-1'),
            s3_bucket_videos=os.getenv('S3_BUCKET_VIDEOS', 'nexusai-videos-prod'),
            s3_bucket_assets=os.getenv('S3_BUCKET_ASSETS', 'nexusai-video-assets-prod'),
            temp_dir=os.getenv('TEMP_DIR', '/tmp/video-generation'),
            worker_id=os.getenv('WORKER_ID', str(uuid.uuid4()))
        )


class VideoGenerationWorker:
    """
    Worker principal de génération vidéo.
    
    Responsabilités:
    - Consommation des messages Kafka
    - Orchestration du pipeline de génération
    - Gestion des erreurs et retry
    - Émission des événements de progression
    """

    def __init__(self, config: VideoConfig):
        """Initialise le worker avec la configuration."""
        self.config = config
        self.running = True
        
        # Initialisation des clients
        self._init_kafka()
        self._init_aws()
        self._init_openai()
        
        # Création du répertoire temporaire
        Path(config.temp_dir).mkdir(parents=True, exist_ok=True)
        
        logger.info(f"Worker {config.worker_id} initialisé")

    def _init_kafka(self):
        """Initialise les clients Kafka."""
        self.consumer = KafkaConsumer(
            self.config.kafka_topic_requests,
            bootstrap_servers=self.config.kafka_bootstrap_servers,
            group_id='video-generation-workers',
            auto_offset_reset='earliest',
            enable_auto_commit=False,
            value_deserializer=lambda m: json.loads(m.decode('utf-8'))
        )
        
        self.producer = KafkaProducer(
            bootstrap_servers=self.config.kafka_bootstrap_servers,
            value_serializer=lambda v: json.dumps(v).encode('utf-8')
        )
        
        logger.info("Clients Kafka initialisés")

    def _init_aws(self):
        """Initialise le client S3."""
        self.s3_client = boto3.client(
            's3',
            aws_access_key_id=self.config.aws_access_key,
            aws_secret_access_key=self.config.aws_secret_key,
            region_name=self.config.aws_region
        )
        logger.info("Client S3 initialisé")

    def _init_openai(self):
        """Initialise le client OpenAI."""
        openai.api_key = self.config.openai_api_key
        logger.info("Client OpenAI initialisé")

    def start(self):
        """Démarre la boucle de consommation des messages."""
        logger.info(f"Worker {self.config.worker_id} démarré - En attente de messages...")
        
        try:
            for message in self.consumer:
                if not self.running:
                    break
                
                try:
                    self._process_message(message)
                    self.consumer.commit()
                except Exception as e:
                    logger.error(f"Erreur lors du traitement du message: {e}", exc_info=True)
                    # Ne pas commiter - le message sera retraité
        
        except KeyboardInterrupt:
            logger.info("Arrêt demandé par l'utilisateur")
        finally:
            self.stop()

    def stop(self):
        """Arrête proprement le worker."""
        logger.info("Arrêt du worker...")
        self.running = False
        self.consumer.close()
        self.producer.close()
        logger.info("Worker arrêté")

    def _process_message(self, message):
        """
        Traite un message de génération vidéo.
        
        Args:
            message: Message Kafka contenant la requête
        """
        request_data = message.value
        request = VideoRequest(
            video_id=request_data['videoId'],
            user_id=request_data['userId'],
            companion_id=request_data.get('companionId'),
            prompt=request_data['prompt'],
            duration_seconds=request_data['durationSeconds'],
            quality=request_data['quality'],
            resolution=request_data['resolution'],
            frame_rate=request_data['frameRate'],
            visual_style=request_data.get('visualStyle'),
            music_style=request_data.get('musicStyle'),
            include_elements=request_data.get('includeElements'),
            timestamp=request_data['timestamp']
        )
        
        logger.info(f"Traitement de la vidéo {request.video_id}")
        start_time = time.time()
        
        try:
            # Phase 1: Génération du scénario
            self._emit_phase_event(request.video_id, 'SCRIPT_GENERATION', 0)
            scenario = self._generate_scenario(request)
            self._emit_phase_event(request.video_id, 'SCRIPT_GENERATION', 100)
            
            # Phase 2: Génération des assets
            self._emit_phase_event(request.video_id, 'ASSET_GENERATION', 0)
            assets = self._generate_assets(request, scenario)
            self._emit_phase_event(request.video_id, 'ASSET_GENERATION', 100)
            
            # Phase 3: Composition
            self._emit_phase_event(request.video_id, 'COMPOSITING', 0)
            scenes = self._composite_scenes(request, scenario, assets)
            self._emit_phase_event(request.video_id, 'COMPOSITING', 100)
            
            # Phase 4: Rendu
            self._emit_phase_event(request.video_id, 'RENDERING', 0)
            raw_video_path = self._render_video(request, scenes)
            self._emit_phase_event(request.video_id, 'RENDERING', 100)
            
            # Phase 5: Encodage
            self._emit_phase_event(request.video_id, 'ENCODING', 0)
            encoded_video_path = self._encode_video(request, raw_video_path)
            self._emit_phase_event(request.video_id, 'ENCODING', 100)
            
            # Phase 6: Finalisation
            self._emit_phase_event(request.video_id, 'FINALIZATION', 0)
            video_url, thumbnail_urls, file_size = self._finalize(
                request, encoded_video_path
            )
            self._emit_phase_event(request.video_id, 'FINALIZATION', 100)
            
            # Calcul du temps total
            generation_time_minutes = int((time.time() - start_time) / 60)
            
            # Émission de l'événement de complétion
            self._emit_completion_event(
                request.video_id,
                video_url,
                thumbnail_urls,
                file_size,
                generation_time_minutes
            )
            
            logger.info(
                f"Vidéo {request.video_id} générée avec succès en "
                f"{generation_time_minutes} minutes"
            )
        
        except Exception as e:
            logger.error(f"Échec de la génération de la vidéo {request.video_id}", exc_info=True)
            self._emit_failure_event(request.video_id, str(e))
            raise

    def _generate_scenario(self, request: VideoRequest) -> Dict:
        """
        Génère le scénario de la vidéo avec GPT-4.
        
        Args:
            request: Requête de génération
            
        Returns:
            Scénario structuré au format JSON
        """
        logger.info(f"Génération du scénario pour {request.video_id}")
        
        # Construction du prompt système
        system_prompt = """Tu es un expert en création de scénarios vidéo.
        Génère un scénario détaillé au format JSON avec la structure suivante:
        {
            "title": "Titre de la vidéo",
            "scenes": [
                {
                    "scene_number": 1,
                    "duration_seconds": 10,
                    "description": "Description de la scène",
                    "visual_elements": ["element1", "element2"],
                    "dialogue": "Texte du dialogue",
                    "camera_angle": "plan",
                    "lighting": "ambiance lumière"
                }
            ]
        }
        """
        
        # Construction du prompt utilisateur
        user_prompt = f"""Crée un scénario vidéo de {request.duration_seconds} secondes
        basé sur le prompt suivant: "{request.prompt}"
        
        Style visuel: {request.visual_style or 'Réaliste'}
        Éléments à inclure: {', '.join(request.include_elements or [])}
        """
        
        # Appel à l'API OpenAI
        response = openai.ChatCompletion.create(
            model="gpt-4",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt}
            ],
            temperature=0.7,
            max_tokens=2000
        )
        
        scenario_text = response.choices[0].message.content
        scenario = json.loads(scenario_text)
        
        logger.info(f"Scénario généré: {len(scenario['scenes'])} scènes")
        return scenario

    def _generate_assets(self, request: VideoRequest, scenario: Dict) -> Dict:
        """
        Génère tous les assets nécessaires (images, audio) en parallèle.
        
        Args:
            request: Requête de génération
            scenario: Scénario structuré
            
        Returns:
            Dictionnaire des assets générés
        """
        logger.info(f"Génération des assets pour {request.video_id}")
        assets = {
            'backgrounds': [],
            'characters': [],
            'audio_voice': [],
            'audio_music': None
        }
        
        # Génération en parallèle avec ThreadPoolExecutor
        with ThreadPoolExecutor(max_workers=4) as executor:
            futures = []
            
            # Génération des backgrounds
            for scene in scenario['scenes']:
                future = executor.submit(
                    self._generate_background_image,
                    request.video_id,
                    scene
                )
                futures.append(('background', future))
            
            # Génération des dialogues audio
            for scene in scenario['scenes']:
                if scene.get('dialogue'):
                    future = executor.submit(
                        self._generate_voice_audio,
                        request.video_id,
                        scene['dialogue'],
                        request.companion_id
                    )
                    futures.append(('voice', future))
            
            # Génération de la musique de fond
            if request.music_style:
                future = executor.submit(
                    self._generate_background_music,
                    request.video_id,
                    request.duration_seconds,
                    request.music_style
                )
                futures.append(('music', future))
            
            # Collecte des résultats
            for asset_type, future in futures:
                try:
                    result = future.result(timeout=300)  # 5 minutes max
                    if asset_type == 'background':
                        assets['backgrounds'].append(result)
                    elif asset_type == 'voice':
                        assets['audio_voice'].append(result)
                    elif asset_type == 'music':
                        assets['audio_music'] = result
                except Exception as e:
                    logger.error(f"Erreur génération asset {asset_type}: {e}")
        
        logger.info(
            f"Assets générés: {len(assets['backgrounds'])} images, "
            f"{len(assets['audio_voice'])} audio voix"
        )
        return assets

    def _generate_background_image(self, video_id: str, scene: Dict) -> str:
        """
        Génère une image de fond avec Stable Diffusion.
        
        Args:
            video_id: ID de la vidéo
            scene: Données de la scène
            
        Returns:
            Chemin du fichier image généré
        """
        # TODO: Intégrer Stable Diffusion
        # Pour l'instant, génération d'une image de test
        logger.debug(f"Génération image pour scène {scene['scene_number']}")
        
        img_path = os.path.join(
            self.config.temp_dir,
            f"{video_id}_scene_{scene['scene_number']}.png"
        )
        
        # Création d'une image de test (à remplacer par Stable Diffusion)
        img = Image.new('RGB', (1920, 1080), color=(73, 109, 137))
        img.save(img_path)
        
        return img_path

    def _generate_voice_audio(
            self, 
            video_id: str, 
            text: str, 
            companion_id: Optional[str]
    ) -> str:
        """
        Génère l'audio de la voix avec ElevenLabs.
        
        Args:
            video_id: ID de la vidéo
            text: Texte à synthétiser
            companion_id: ID du compagnon (pour récupérer la voix)
            
        Returns:
            Chemin du fichier audio généré
        """
        logger.debug(f"Génération audio: {text[:50]}...")
        
        # TODO: Intégrer ElevenLabs API
        audio_path = os.path.join(
            self.config.temp_dir,
            f"{video_id}_audio_{uuid.uuid4()}.mp3"
        )
        
        # Placeholder - à remplacer par vrai TTS
        return audio_path

    def _generate_background_music(
            self, 
            video_id: str, 
            duration: int, 
            style: str
    ) -> str:
        """
        Génère la musique de fond.
        
        Args:
            video_id: ID de la vidéo
            duration: Durée en secondes
            style: Style musical
            
        Returns:
            Chemin du fichier audio
        """
        logger.debug(f"Génération musique de fond ({style})")
        
        # TODO: Intégrer Mubert AI ou bibliothèque musicale
        music_path = os.path.join(
            self.config.temp_dir,
            f"{video_id}_music.mp3"
        )
        
        return music_path

    def _composite_scenes(
            self, 
            request: VideoRequest, 
            scenario: Dict, 
            assets: Dict
    ) -> List[str]:
        """
        Composite les scènes en combinant images et audio.
        
        Args:
            request: Requête de génération
            scenario: Scénario
            assets: Assets générés
            
        Returns:
            Liste des chemins des scènes composées
        """
        logger.info(f"Composition des scènes pour {request.video_id}")
        scene_paths = []
        
        for i, scene in enumerate(scenario['scenes']):
            scene_path = self._composite_single_scene(
                request.video_id,
                scene,
                assets['backgrounds'][i],
                assets['audio_voice'][i] if i < len(assets['audio_voice']) else None
            )
            scene_paths.append(scene_path)
        
        return scene_paths

    def _composite_single_scene(
            self,
            video_id: str,
            scene: Dict,
            background_path: str,
            audio_path: Optional[str]
    ) -> str:
        """Composite une scène individuelle."""
        output_path = os.path.join(
            self.config.temp_dir,
            f"{video_id}_scene_{scene['scene_number']}_composed.mp4"
        )
        
        # TODO: Utiliser OpenCV/PIL pour composer la scène
        
        return output_path

    def _render_video(self, request: VideoRequest, scene_paths: List[str]) -> str:
        """
        Rend la vidéo finale en assemblant toutes les scènes.
        
        Args:
            request: Requête de génération
            scene_paths: Chemins des scènes composées
            
        Returns:
            Chemin de la vidéo rendue
        """
        logger.info(f"Rendu vidéo pour {request.video_id}")
        
        output_path = os.path.join(
            self.config.temp_dir,
            f"{request.video_id}_rendered.mp4"
        )
        
        # Création du fichier de concatenation pour FFmpeg
        concat_file = os.path.join(
            self.config.temp_dir,
            f"{request.video_id}_concat.txt"
        )
        
        with open(concat_file, 'w') as f:
            for scene_path in scene_paths:
                f.write(f"file '{scene_path}'\n")
        
        # Commande FFmpeg pour assembler les scènes
        cmd = [
            'ffmpeg',
            '-f', 'concat',
            '-safe', '0',
            '-i', concat_file,
            '-c', 'copy',
            output_path
        ]
        
        subprocess.run(cmd, check=True, capture_output=True)
        
        logger.info(f"Vidéo rendue: {output_path}")
        return output_path

    def _encode_video(self, request: VideoRequest, raw_video_path: str) -> str:
        """
        Encode la vidéo au format final avec les paramètres de qualité.
        
        Args:
            request: Requête de génération
            raw_video_path: Chemin de la vidéo brute
            
        Returns:
            Chemin de la vidéo encodée
        """
        logger.info(f"Encodage vidéo pour {request.video_id}")
        
        output_path = os.path.join(
            self.config.temp_dir,
            f"{request.video_id}_final.mp4"
        )
        
        # Paramètres d'encodage selon la qualité
        if request.quality == 'ULTRA':
            video_codec = 'libx265'
            crf = '18'
            preset = 'slow'
        elif request.quality == 'HD':
            video_codec = 'libx264'
            crf = '20'
            preset = 'medium'
        else:  # STANDARD
            video_codec = 'libx264'
            crf = '23'
            preset = 'fast'
        
        # Commande FFmpeg pour l'encodage
        cmd = [
            'ffmpeg',
            '-i', raw_video_path,
            '-c:v', video_codec,
            '-crf', crf,
            '-preset', preset,
            '-r', str(request.frame_rate),
            '-s', self._get_resolution_size(request.resolution),
            '-c:a', 'aac',
            '-b:a', '192k',
            output_path
        ]
        
        subprocess.run(cmd, check=True, capture_output=True)
        
        logger.info(f"Vidéo encodée: {output_path}")
        return output_path

    def _finalize(
            self, 
            request: VideoRequest, 
            video_path: str
    ) -> tuple[str, List[str], float]:
        """
        Finalise la génération: upload S3, génération thumbnails.
        
        Args:
            request: Requête de génération
            video_path: Chemin de la vidéo finale
            
        Returns:
            Tuple (video_url, thumbnail_urls, file_size_mb)
        """
        logger.info(f"Finalisation pour {request.video_id}")
        
        # Upload de la vidéo vers S3
        video_url = self._upload_to_s3(
            video_path,
            self.config.s3_bucket_videos,
            f"videos/{request.video_id}.mp4"
        )
        
        # Génération des thumbnails
        thumbnail_urls = self._generate_thumbnails(request.video_id, video_path)
        
        # Calcul de la taille du fichier
        file_size_mb = os.path.getsize(video_path) / (1024 * 1024)
        
        # Nettoyage des fichiers temporaires
        self._cleanup_temp_files(request.video_id)
        
        return video_url, thumbnail_urls, file_size_mb

    def _generate_thumbnails(self, video_id: str, video_path: str) -> List[str]:
        """Génère 3 thumbnails de la vidéo."""
        logger.debug(f"Génération des thumbnails pour {video_id}")
        thumbnail_urls = []
        
        for i in range(3):
            thumbnail_path = os.path.join(
                self.config.temp_dir,
                f"{video_id}_thumb_{i}.jpg"
            )
            
            # Extraction d'une frame avec FFmpeg
            timestamp = f"00:00:{i * 10:02d}"  # 0s, 10s, 20s
            cmd = [
                'ffmpeg',
                '-i', video_path,
                '-ss', timestamp,
                '-vframes', '1',
                '-q:v', '2',
                thumbnail_path
            ]
            
            subprocess.run(cmd, check=True, capture_output=True)
            
            # Upload vers S3
            url = self._upload_to_s3(
                thumbnail_path,
                self.config.s3_bucket_videos,
                f"thumbnails/{video_id}/thumb_{i}.jpg"
            )
            thumbnail_urls.append(url)
        
        return thumbnail_urls

    def _upload_to_s3(self, file_path: str, bucket: str, key: str) -> str:
        """
        Upload un fichier vers S3.
        
        Args:
            file_path: Chemin du fichier local
            bucket: Nom du bucket S3
            key: Clé S3
            
        Returns:
            URL S3 du fichier
        """
        logger.debug(f"Upload vers S3: {key}")
        
        self.s3_client.upload_file(file_path, bucket, key)
        
        url = f"https://{bucket}.s3.{self.config.aws_region}.amazonaws.com/{key}"
        return url

    def _cleanup_temp_files(self, video_id: str):
        """Supprime les fichiers temporaires."""
        logger.debug(f"Nettoyage des fichiers temporaires pour {video_id}")
        
        import glob
        temp_files = glob.glob(os.path.join(self.config.temp_dir, f"{video_id}*"))
        
        for file in temp_files:
            try:
                os.remove(file)
            except Exception as e:
                logger.warning(f"Erreur suppression fichier {file}: {e}")

    def _get_resolution_size(self, resolution: str) -> str:
        """Retourne la taille en pixels selon la résolution."""
        sizes = {
            '720p': '1280x720',
            '1080p': '1920x1080',
            '4K': '3840x2160'
        }
        return sizes.get(resolution, '1920x1080')

    def _emit_phase_event(self, video_id: str, phase: str, percentage: int):
        """Émet un événement de changement de phase."""
        event = {
            'eventType': 'PHASE_STARTED' if percentage == 0 else 'PROGRESS_UPDATE',
            'videoId': video_id,
            'phase': phase,
            'percentage': percentage,
            'timestamp': int(time.time() * 1000)
        }
        
        self.producer.send(self.config.kafka_topic_events, value=event)
        logger.debug(f"Événement émis: {phase} - {percentage}%")

    def _emit_completion_event(
            self,
            video_id: str,
            storage_url: str,
            thumbnail_urls: List[str],
            file_size_mb: float,
            generation_time_minutes: int
    ):
        """Émet un événement de complétion."""
        event = {
            'eventType': 'GENERATION_COMPLETED',
            'videoId': video_id,
            'storageUrl': storage_url,
            'thumbnailUrls': thumbnail_urls,
            'fileSizeMb': file_size_mb,
            'generationTimeMinutes': generation_time_minutes,
            'timestamp': int(time.time() * 1000)
        }
        
        self.producer.send(self.config.kafka_topic_events, value=event)
        logger.info(f"Événement de complétion émis pour {video_id}")

    def _emit_failure_event(self, video_id: str, error_message: str):
        """Émet un événement d'échec."""
        event = {
            'eventType': 'GENERATION_FAILED',
            'videoId': video_id,
            'errorMessage': error_message,
            'timestamp': int(time.time() * 1000)
        }
        
        self.producer.send(self.config.kafka_topic_events, value=event)
        logger.error(f"Événement d'échec émis pour {video_id}")


def main():
    """Point d'entrée principal du worker."""
    logger.info("Démarrage du worker de génération vidéo")
    
    # Chargement de la configuration
    config = VideoConfig.from_env()
    
    # Création et démarrage du worker
    worker = VideoGenerationWorker(config)
    
    try:
        worker.start()
    except Exception as e:
        logger.critical(f"Erreur fatale: {e}", exc_info=True)
        sys.exit(1)


if __name__ == '__main__':
    main()
