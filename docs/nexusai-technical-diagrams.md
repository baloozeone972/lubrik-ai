# NEXUSAI - DIAGRAMMES TECHNIQUES DÉTAILLÉS

**Version 1.0 | Date : 15 Janvier 2025**

---

## TABLE DES MATIÈRES

1. [Architecture VR Complète](#1-architecture-vr-complète)
2. [Pipeline Génération Vidéo](#2-pipeline-génération-vidéo)
3. [Flux de Modération Multi-Niveaux](#3-flux-de-modération-multi-niveaux)
4. [Architecture Système Globale](#4-architecture-système-globale)
5. [Diagrammes UML](#5-diagrammes-uml)
6. [Diagrammes de Séquence](#6-diagrammes-de-séquence)
7. [Architecture Base de Données](#7-architecture-base-de-données)

---

# 1. ARCHITECTURE VR COMPLÈTE

## 1.1 Vue d'Ensemble Architecture VR

```
┌─────────────────────────────────────────────────────────────────┐
│                    ARCHITECTURE VR NEXUSAI                      │
│                   (Multi-Plateforme & Cloud)                    │
└─────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────┐
│                         CLIENT VR (Unity/Unreal)                      │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐    │
│  │  Input Manager  │  │  Render Engine  │  │  Audio Engine   │    │
│  │  - Head Track   │  │  - 90-120 FPS   │  │  - Spatial 3D   │    │
│  │  - Hand Track   │  │  - Foveated     │  │  - HRTF         │    │
│  │  - Controllers  │  │  - LOD Dynamic  │  │  - Occlusion    │    │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘    │
│           │                    │                     │              │
│  ┌────────┴────────────────────┴─────────────────────┴────────┐   │
│  │              Avatar & Animation System                      │   │
│  │  - Full Body IK (Inverse Kinematics)                       │   │
│  │  - Facial Animation (52 blend shapes)                      │   │
│  │  - Lip Sync Real-Time                                      │   │
│  │  - Gesture Recognition                                     │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              Haptic Feedback Controller                     │   │
│  │  - HaptX / SenseGlove Integration                          │   │
│  │  - bHaptics Vest Integration                               │   │
│  │  - Vibration Patterns Library                              │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                       │
└───────────────────────────────┬───────────────────────────────────────┘
                                │
                    WebSocket + WebRTC (Low Latency)
                                │
                                ▼
┌───────────────────────────────────────────────────────────────────────┐
│                        BACKEND VR SERVICES                            │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌──────────────────────┐  ┌──────────────────────┐                │
│  │  VR Session Manager  │  │  Sync Coordinator    │                │
│  │  - Session Lifecycle │  │  - State Sync        │                │
│  │  - User Presence     │  │  - Movement Sync     │                │
│  │  - Room Management   │  │  - Animation Sync    │                │
│  └──────────┬───────────┘  └──────────┬───────────┘                │
│             │                          │                            │
│  ┌──────────┴──────────────────────────┴───────────┐               │
│  │        Companion AI Controller (VR)             │               │
│  │  - Behavior Tree (complex AI logic)            │               │
│  │  - Emotion State Machine                       │               │
│  │  - Contextual Reaction Generator               │               │
│  │  - Proximity & Gaze Detection                  │               │
│  └────────────────────────────────────────────────┘               │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │            Environment & Physics Engine                      │   │
│  │  - Scene Loading (Dynamic)                                  │   │
│  │  - Physics Simulation (Havok/PhysX)                        │   │
│  │  - Collision Detection                                      │   │
│  │  - Object Interaction System                               │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                       │
└───────────────────────────────┬───────────────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────────────┐
│                   CLOUD RENDERING SERVERS (Optional)                  │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              GPU Rendering Farm                              │   │
│  │  - NVIDIA RTX 4090 / A100 GPUs                             │   │
│  │  - Unity/Unreal Server Build                               │   │
│  │  - Ray Tracing Ultra Quality                               │   │
│  │  - 4K @ 120fps Rendering                                   │   │
│  └────────────────────────────┬────────────────────────────────┘   │
│                                │                                      │
│  ┌────────────────────────────┴────────────────────────────────┐   │
│  │           Video Encoding Pipeline                            │   │
│  │  - H.265 Hardware Encoding (NVENC)                         │   │
│  │  - Adaptive Bitrate (5-50 Mbps)                            │   │
│  │  - Foveated Encoding (Higher quality at gaze point)        │   │
│  │  - Frame Prediction (Reduce latency)                       │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │            Streaming Distribution (CDN)                      │   │
│  │  - Edge Nodes (Low latency < 20ms)                         │   │
│  │  - Geographic Distribution                                  │   │
│  │  - Load Balancing                                           │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────┐
│                    SUPPORTED VR HEADSETS                              │
├───────────────────────────────────────────────────────────────────────┤
│  • Meta Quest 2/3/Pro (Standalone + PCVR)                           │
│  • PlayStation VR2                                                    │
│  • Valve Index                                                        │
│  • HTC Vive / Vive Pro                                               │
│  • HP Reverb G2                                                       │
│  • Pico 4 / Pico Neo 3                                               │
└───────────────────────────────────────────────────────────────────────┘
```

## 1.2 Détails Techniques VR

### 1.2.1 Optimisations Performance

```
RENDERING OPTIMIZATIONS
├── Foveated Rendering
│   ├── Eye Tracking Required
│   ├── High Resolution Center (100%)
│   ├── Medium Periphery (50%)
│   └── Low Far Periphery (25%)
│
├── Level of Detail (LOD) System
│   ├── LOD0: < 5m (High poly)
│   ├── LOD1: 5-15m (Medium poly)
│   ├── LOD2: 15-30m (Low poly)
│   └── LOD3: > 30m (Impostor/Billboard)
│
├── Occlusion Culling
│   ├── Frustum Culling
│   ├── Portal Culling
│   └── GPU Occlusion Queries
│
├── Texture Streaming
│   ├── Mipmap Streaming
│   ├── Virtual Texturing
│   └── Async Loading
│
└── Physics Optimization
    ├── Simplified Colliders
    ├── Physics LOD
    └── Async Physics Simulation
```

### 1.2.2 Latency Optimization

```
TARGET LATENCIES (Motion-to-Photon)
├── Local Rendering: < 20ms
├── PCVR (Wired): < 30ms
├── PCVR (Wireless): < 40ms
└── Cloud Streaming: < 50ms

TECHNIQUES
├── Asynchronous Timewarp (ATW)
├── Asynchronous Spacewarp (ASW)
├── Motion Prediction
├── Frame Extrapolation
└── Network Prediction
```

### 1.2.3 Hand Tracking Architecture

```
┌────────────────────────────────────────┐
│      Hand Tracking Pipeline            │
├────────────────────────────────────────┤
│                                        │
│  Camera Input (IR Cameras)            │
│         ▼                              │
│  Hand Detection (ML Model)             │
│         ▼                              │
│  Keypoint Extraction (21 points)      │
│         ▼                              │
│  Pose Estimation                       │
│         ▼                              │
│  Gesture Recognition                   │
│         ▼                              │
│  Physics Interaction                   │
│         ▼                              │
│  Haptic Feedback                       │
│                                        │
└────────────────────────────────────────┘
```

---

# 2. PIPELINE GÉNÉRATION VIDÉO

## 2.1 Architecture Complète Pipeline Vidéo

```
┌─────────────────────────────────────────────────────────────────────┐
│                 PIPELINE GÉNÉRATION VIDÉO NEXUSAI                   │
│                    (Court & Avancé 4K)                              │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│  PHASE 1: REQUÊTE UTILISATEUR                                        │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  User Input (Web/Mobile/VR)                                         │
│  ├── Prompt Texte: "Mon compagnon dans un café parisien"          │
│  ├── Durée: 2 minutes                                               │
│  ├── Style: Cinématique                                             │
│  ├── Résolution: 1080p                                              │
│  └── Personnalisation: Tenue spécifique, émotion                   │
│                                                                      │
│         ▼                                                            │
│  ┌──────────────────────────────────────────┐                      │
│  │  Validation & Cost Estimation            │                      │
│  │  - Vérification quotas/jetons            │                      │
│  │  - Estimation temps (2-10 min)           │                      │
│  │  - Calcul coût jetons (100-500)          │                      │
│  └──────────────────────────────────────────┘                      │
│                                                                      │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│  PHASE 2: GÉNÉRATION SCÉNARIO (LLM)                                 │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  LLM Script Generator (GPT-4 / Claude)                     │   │
│  │  Input: User prompt + Companion profile                    │   │
│  │  Output: Structured scenario JSON                          │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  Scenario JSON Structure:                                           │
│  {                                                                   │
│    "duration": 120,                                                  │
│    "scenes": [                                                       │
│      {                                                               │
│        "scene_id": 1,                                                │
│        "duration": 30,                                               │
│        "location": "Café terrace exterior",                         │
│        "time_of_day": "Golden hour",                                │
│        "weather": "Sunny, slight breeze",                           │
│        "camera_angles": ["Medium shot", "Close-up"],                │
│        "companion_action": "Sipping coffee, looking at user",       │
│        "dialogue": "J'adore cet endroit...",                        │
│        "emotion": "Contentment",                                     │
│        "background_elements": ["Passers-by", "Street musician"]     │
│      },                                                              │
│      { ... }                                                         │
│    ]                                                                 │
│  }                                                                   │
│                                                                      │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│  PHASE 3: GÉNÉRATION ASSETS VISUELS (Parallel)                      │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────────────┐  ┌──────────────────────┐                │
│  │  Background Scenes   │  │  Character Renders   │                │
│  │  (Stable Diffusion)  │  │  (Custom Model)      │                │
│  │                      │  │                      │                │
│  │  • Café exterior     │  │  • Companion frames  │                │
│  │  • Street views      │  │  • Facial express.   │                │
│  │  • Interior shots    │  │  • Body poses        │                │
│  └──────────┬───────────┘  └──────────┬───────────┘                │
│             │                          │                            │
│             └──────────┬───────────────┘                            │
│                        ▼                                             │
│            ┌───────────────────────┐                                │
│            │  Composite & Masking  │                                │
│            │  - Layer companion    │                                │
│            │  - Depth compositing  │                                │
│            │  - Color matching     │                                │
│            └───────────┬───────────┘                                │
│                        │                                             │
└────────────────────────┼─────────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────────────┐
│  PHASE 4: GÉNÉRATION AUDIO                                           │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Text-to-Speech (ElevenLabs / Coqui TTS)                  │   │
│  │  - Dialogue synthesis with companion voice                 │   │
│  │  - Emotion modulation                                      │   │
│  │  - Lip sync phoneme generation                             │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Background Music & Ambiance (AI-Generated)                │   │
│  │  - Mood-appropriate music (Mubert AI)                     │   │
│  │  - Ambient sounds (café noise, street, birds)             │   │
│  │  - Foley effects (footsteps, cup clinks)                  │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Audio Mixing & Mastering                                  │   │
│  │  - Level balancing                                         │   │
│  │  - Spatial audio (if VR)                                   │   │
│  │  - Compression & normalization                             │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│  PHASE 5: RENDERING & COMPOSITION                                    │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Video Composition Engine (FFmpeg / DaVinci Resolve API)   │   │
│  │                                                             │   │
│  │  Per Scene:                                                 │   │
│  │  1. Load background frame                                   │   │
│  │  2. Overlay companion with mask                             │   │
│  │  3. Apply color grading                                     │   │
│  │  4. Add effects (motion blur, depth of field)              │   │
│  │  5. Sync audio (dialogue + music + ambiance)               │   │
│  │  6. Render frame                                            │   │
│  │                                                             │   │
│  │  Scene Transitions:                                         │   │
│  │  - Cross-dissolve                                           │   │
│  │  - Cut                                                      │   │
│  │  - Fade to black                                            │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  GPU Rendering Farm                                         │   │
│  │  - NVIDIA RTX 4090 / A100 GPUs                            │   │
│  │  - Parallel rendering (multiple scenes simultaneously)     │   │
│  │  - Real-time progress tracking                             │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  Quality Settings:                                                   │
│  ├── Standard (1080p, H.264, 30fps): ~5 min render                 │
│  ├── HD (1080p, H.265, 60fps): ~8 min render                       │
│  └── 4K Advanced (2160p, H.265, 60fps, HDR): ~20 min render        │
│                                                                      │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│  PHASE 6: ENCODING & OPTIMIZATION                                    │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Video Encoding (H.265 / VP9)                              │   │
│  │  - CRF 23 (High Quality)                                   │   │
│  │  - 2-pass encoding                                         │   │
│  │  - Hardware acceleration (NVENC)                           │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Multiple Formats Generation                               │   │
│  │  - MP4 (H.265): Web/mobile compatible                     │   │
│  │  - WebM (VP9): Browser streaming                          │   │
│  │  - MOV (ProRes): Professional editing                     │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Thumbnail Generation                                       │   │
│  │  - Key frame extraction                                     │   │
│  │  - 3 preview thumbnails                                     │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│  PHASE 7: STORAGE & DELIVERY                                         │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Storage (S3 / MinIO)                                      │   │
│  │  - Original quality file                                   │   │
│  │  - Compressed web version                                  │   │
│  │  - Thumbnail images                                        │   │
│  │  - Metadata JSON                                           │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  CDN Distribution                                           │   │
│  │  - Edge caching for fast delivery                         │   │
│  │  - Geo-distributed                                         │   │
│  │  - Signed URLs (24h expiration)                           │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  User Notification                                          │   │
│  │  - In-app notification                                      │   │
│  │  - Email with download link                                │   │
│  │  - Push notification (mobile)                              │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│  MONITORING & ANALYTICS                                              │
├──────────────────────────────────────────────────────────────────────┤
│  • Queue length & wait time                                         │
│  • Render time per quality setting                                  │
│  • Success/failure rate                                             │
│  • Resource utilization (GPU, CPU, storage)                         │
│  • Cost per video generated                                         │
│  • User satisfaction ratings                                        │
└──────────────────────────────────────────────────────────────────────┘
```

## 2.2 Spécifications Techniques Vidéo

### 2.2.1 Formats de Sortie

```
FORMAT SPECIFICATIONS

Standard Video (Premium)
├── Resolution: 1920x1080 (1080p)
├── Frame Rate: 30 fps
├── Codec: H.264 (High Profile)
├── Bitrate: 8 Mbps (variable)
├── Audio: AAC 192 kbps stereo
├── Duration: 30s - 5min
└── File Size: ~60-600 MB

HD Video (VIP)
├── Resolution: 1920x1080 (1080p)
├── Frame Rate: 60 fps
├── Codec: H.265 (HEVC)
├── Bitrate: 12 Mbps (variable)
├── Audio: AAC 256 kbps stereo
├── Duration: 30s - 10min
└── File Size: ~90-1200 MB

4K Advanced Video (VIP+)
├── Resolution: 3840x2160 (4K UHD)
├── Frame Rate: 60 fps
├── Codec: H.265 (HEVC)
├── Bitrate: 40 Mbps (variable)
├── HDR: HDR10 (optional)
├── Audio: AAC 320 kbps 5.1 surround
├── Duration: 30s - 30min
└── File Size: ~300 MB - 9 GB
```

### 2.2.2 Temps de Génération Estimés

```
GENERATION TIME ESTIMATES
(With GPU Farm: 4x NVIDIA RTX 4090)

30 seconds @ 1080p 30fps:  2-3 minutes
2 minutes @ 1080p 30fps:   5-8 minutes
5 minutes @ 1080p 60fps:   10-15 minutes
2 minutes @ 4K 60fps:      15-20 minutes
10 minutes @ 4K 60fps:     40-60 minutes
30 minutes @ 4K 60fps HDR: 90-120 minutes
```

---

# 3. FLUX DE MODÉRATION MULTI-NIVEAUX

## 3.1 Architecture Globale Modération

```
┌─────────────────────────────────────────────────────────────────────┐
│          SYSTÈME DE MODÉRATION ADAPTATIVE NEXUSAI                   │
│            (3 Niveaux selon Plan d'Abonnement)                      │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│  INPUT: User Content                                                 │
│  ├── Text Message                                                    │
│  ├── Audio Message (transcribed)                                    │
│  ├── Image Upload                                                    │
│  ├── Image Generation Request                                        │
│  └── Video Generation Request                                        │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│  ÉTAPE 1: IDENTIFICATION UTILISATEUR & NIVEAU                        │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  User Service - Get Subscription Level                     │   │
│  │  Input: user_id                                            │   │
│  │  Output: {                                                 │   │
│  │    plan: "FREE" | "STANDARD" | "PREMIUM" | "VIP" | "VIP+"│   │
│  │    moderation_level: "STRICT" | "LIGHT" | "OPTIONAL"     │   │
│  │    moderation_enabled: boolean                            │   │
│  │    kyc_level: 0-3                                         │   │
│  │  }                                                         │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  Decision Tree:                                                      │
│  ├── FREE/STANDARD → STRICT (forced)                                │
│  ├── PREMIUM → STRICT or LIGHT (user choice)                        │
│  ├── VIP → LIGHT (default) or OPTIONAL (if KYC3)                   │
│  └── VIP+ → OPTIONAL (if KYC3 + consent)                           │
│                                                                      │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                ┌─────────────┴─────────────┐
                │                           │
                ▼                           ▼
┌────────────────────────────┐  ┌────────────────────────────┐
│  MODE: STRICT             │  │  MODE: LIGHT               │
│  (Gratuit/Standard/       │  │  (Premium/VIP)             │
│   Premium choix)          │  │                            │
└──────────┬─────────────────┘  └──────────┬─────────────────┘
           │                               │
           │                               │
           └────────────┬──────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────────────────┐
│  MODE: OPTIONAL                                                     │
│  (VIP+ uniquement avec KYC3 + consentement)                       │
└──────────┬─────────────────────────────────────────────────────────┘
           │
           │
  ┌────────┴────────┐
  │                 │
  ▼                 ▼
[Modéré]    [Sans Modération]


═══════════════════════════════════════════════════════════════════════
  FLUX MODE STRICT (Gratuit/Standard/Premium-Strict)
═══════════════════════════════════════════════════════════════════════

┌──────────────────────────────────────────────────────────────────────┐
│  ÉTAPE 2A: PRÉ-FILTRAGE RAPIDE                                      │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Blacklist Check (< 10ms)                                  │   │
│  │  - Known banned terms                                      │   │
│  │  - Prohibited URLs/patterns                                │   │
│  │  - Spam detection                                          │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  If BLOCKED → Return error + warning                                │
│  If PASS → Continue                                                  │
│                                                                      │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│  ÉTAPE 3A: ANALYSE IA PROFONDE                                       │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  OpenAI Moderation API (or equivalent)                     │   │
│  │  Input: content                                            │   │
│  │  Output: {                                                 │   │
│  │    categories: {                                           │   │
│  │      sexual: 0.05,                                         │   │
│  │      hate: 0.01,                                           │   │
│  │      violence: 0.02,                                       │   │
│  │      self-harm: 0.00,                                      │   │
│  │      sexual/minors: 0.00,  // ZERO TOLERANCE             │   │
│  │      hate/threatening: 0.00,                              │   │
│  │      violence/graphic: 0.01                                │   │
│  │    },                                                      │   │
│  │    flagged: false                                          │   │
│  │  }                                                         │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  Thresholds MODE STRICT:                                             │
│  ├── sexual: > 0.50 → BLOCK                                         │
│  ├── hate: > 0.30 → BLOCK                                           │
│  ├── violence: > 0.40 → BLOCK                                       │
│  ├── sexual/minors: > 0.01 → BLOCK + ALERT                         │
│  └── self-harm: > 0.30 → BLOCK + WELLBEING ALERT                   │
│                                                                      │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │                   │
              [BLOCKED]           [ALLOWED]
                    │                   │
                    ▼                   ▼
          ┌─────────────────┐  ┌─────────────────┐
          │ Log + Warning   │  │ Pass to AI      │
          │ User Notified   │  │ Processing      │
          └─────────────────┘  └─────────────────┘


═══════════════════════════════════════════════════════════════════════
  FLUX MODE LIGHT (Premium/VIP choix)
═══════════════════════════════════════════════════════════════════════

┌──────────────────────────────────────────────────────────────────────┐
│  ÉTAPE 2B: PRÉ-FILTRAGE MINIMAL                                     │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Critical Checks Only                                       │   │
│  │  - Illegal content signatures                              │   │
│  │  - CSAM (Child Sexual Abuse Material) - ZERO TOLERANCE    │   │
│  │  - Terrorism/violent extremism                             │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  If CRITICAL → Immediate block + authority alert                    │
│  If PASS → Continue to AI analysis                                  │
│                                                                      │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│  ÉTAPE 3B: ANALYSE IA PERMISSIVE                                     │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Same AI analysis but HIGHER thresholds:                             │
│                                                                      │
│  Thresholds MODE LIGHT:                                              │
│  ├── sexual: > 0.85 → WARN (not block)                             │
│  ├── hate: > 0.70 → WARN                                            │
│  ├── violence: > 0.75 → WARN                                        │
│  ├── sexual/minors: > 0.01 → BLOCK + ALERT (unchanged)             │
│  └── self-harm: > 0.60 → WELLBEING ALERT (not block)               │
│                                                                      │
│  Warnings shown to user but content allowed.                         │
│                                                                      │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ Allowed with    │
                    │ optional warning│
                    └─────────────────┘


═══════════════════════════════════════════════════════════════════════
  FLUX MODE OPTIONAL / SANS MODÉRATION (VIP+ KYC3)
═══════════════════════════════════════════════════════════════════════

┌──────────────────────────────────────────────────────────────────────┐
│  VÉRIFICATION PRÉALABLE                                              │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Prerequisites Check                                        │   │
│  │  ☑ KYC Level 3 verified (< 6 months)                      │   │
│  │  ☑ Adult consent signed this session                      │   │
│  │  ☑ VIP+ subscription active                               │   │
│  │  ☑ No recent violations                                   │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  If NOT all met → Fallback to LIGHT mode                            │
│  If all met → Proceed                                                │
│                                                                      │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│  ÉTAPE UNIQUE: VÉRIFICATION LÉGALITÉ MINIMALE                       │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Illegal Content Detection ONLY                            │   │
│  │                                                             │   │
│  │  ALWAYS BLOCKED (même sans modération):                   │   │
│  │  ❌ Child sexual abuse material (CSAM)                    │   │
│  │  ❌ Terrorism content                                      │   │
│  │  ❌ Violent extremism                                      │   │
│  │  ❌ Human trafficking                                      │   │
│  │  ❌ Non-consensual pornography (revenge porn)            │   │
│  │  ❌ Deepfakes of real people (malicious)                 │   │
│  │                                                             │   │
│  │  Uses:                                                      │   │
│  │  - PhotoDNA / Hash matching (Microsoft)                   │   │
│  │  - Machine learning classifiers                           │   │
│  │  - Pattern recognition                                     │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  If ILLEGAL → Immediate block + suspend account + authorities       │
│  If LEGAL → Allow (no further checks)                               │
│                                                                      │
│  ⚠️ NO analytics on content                                         │
│  ⚠️ NO logging of content (only metadata)                           │
│  ⚠️ End-to-end encrypted                                            │
│                                                                      │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ Allowed         │
                    │ (UserResponsib.)│
                    └─────────────────┘


═══════════════════════════════════════════════════════════════════════
  ACTIONS POST-MODÉRATION
═══════════════════════════════════════════════════════════════════════

┌──────────────────────────────────────────────────────────────────────┐
│  SI CONTENU BLOQUÉ                                                   │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. Log Incident                                                     │
│     ├── user_id                                                      │
│     ├── content_hash (not content itself)                           │
│     ├── moderation_reason                                            │
│     ├── severity: LOW | MEDIUM | HIGH | CRITICAL                    │
│     └── timestamp                                                    │
│                                                                      │
│  2. User Notification                                                │
│     ├── "Content blocked: [reason]"                                 │
│     ├── Guidelines link                                              │
│     └── Appeal option (if applicable)                               │
│                                                                      │
│  3. Severity-Based Actions                                           │
│     ├── LOW: Warning only                                           │
│     ├── MEDIUM: Warning + count towards suspension                  │
│     ├── HIGH: Temporary suspension (1-7 days)                       │
│     └── CRITICAL: Permanent ban + authority notification            │
│                                                                      │
│  4. Human Review (if flagged)                                        │
│     ├── Queue for moderator                                          │
│     ├── Review within 24h                                            │
│     └── Final decision logged                                        │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│  SI CONTENU AUTORISÉ                                                 │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. Pass to AI Processing                                            │
│     ├── Conversation Engine                                          │
│     ├── Image Generation                                             │
│     └── Video Generation                                             │
│                                                                      │
│  2. Anonymized Analytics (if not opt-out mode)                      │
│     ├── Content type                                                 │
│     ├── Moderation scores (aggregated)                              │
│     └── No personal data                                             │
│                                                                      │
│  3. Audit Log (metadata only)                                        │
│     ├── user_id                                                      │
│     ├── action_type                                                  │
│     ├── moderation_passed: true                                      │
│     └── timestamp                                                    │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

## 3.2 Matrice de Décision Modération

```
┌─────────────────────────────────────────────────────────────────────┐
│           MATRICE DE DÉCISION - NIVEAUX DE MODÉRATION              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Content Type    │  STRICT  │  LIGHT   │  OPTIONAL (Sans)         │
│  ────────────────┼──────────┼──────────┼────────────────────────  │
│  Sexual (mild)   │  BLOCK   │  ALLOW   │  ALLOW                   │
│  Sexual (expl.)  │  BLOCK   │  WARN    │  ALLOW                   │
│  CSAM            │  BLOCK*  │  BLOCK*  │  BLOCK* (ALWAYS)         │
│  Violence        │  BLOCK   │  WARN    │  ALLOW (if legal)        │
│  Hate Speech     │  BLOCK   │  WARN    │  ALLOW (if legal)        │
│  Self-Harm       │  BLOCK+  │  WARN+   │  ALLOW + wellbeing alert │
│  Terrorism       │  BLOCK*  │  BLOCK*  │  BLOCK* (ALWAYS)         │
│  Illegal Drugs   │  BLOCK   │  WARN    │  ALLOW (discussion)      │
│  Gore/Graphic    │  BLOCK   │  WARN    │  ALLOW                   │
│                                                                     │
│  * = Signalement autorités systématique                            │
│  + = Alerte équipe bien-être                                       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

# 4. ARCHITECTURE SYSTÈME GLOBALE

[Contenu trop long, voir document principal]

---

# 5. DIAGRAMMES UML

## 5.1 Diagramme de Classes - Module Utilisateur

```
┌─────────────────────────────────────────────────────────┐
│                        User                             │
├─────────────────────────────────────────────────────────┤
│ - id: UUID                                              │
│ - email: String                                         │
│ - username: String                                      │
│ - passwordHash: String                                  │
│ - birthDate: LocalDate                                  │
│ - emailVerified: boolean                                │
│ - active: boolean                                       │
│ - role: UserRole                                        │
│ - createdAt: LocalDateTime                              │
│ - updatedAt: LocalDateTime                              │
├─────────────────────────────────────────────────────────┤
│ + register(): void                                      │
│ + verify Email(): void                                  │
│ + changePassword(): void                                │
│ + updateProfile(): void                                 │
└──────────────┬──────────────────────────────────────────┘
               │
               │ 1
               │
               │ 1
┌──────────────┴──────────────────────────────────────────┐
│                    Subscription                         │
├─────────────────────────────────────────────────────────┤
│ - id: UUID                                              │
│ - userId: UUID                                          │
│ - plan: SubscriptionPlan                                │
│ - startDate: LocalDateTime                              │
│ - endDate: LocalDateTime                                │
│ - autoRenewal: boolean                                  │
│ - monthlyPrice: BigDecimal                              │
├─────────────────────────────────────────────────────────┤
│ + upgrade(): void                                       │
│ + downgrade(): void                                     │
│ + cancel(): void                                        │
│ + renew(): void                                         │
│ + isActive(): boolean                                   │
└─────────────────────────────────────────────────────────┘

               │ 1
               │
               │ 1
┌──────────────┴──────────────────────────────────────────┐
│                   TokenWallet                           │
├─────────────────────────────────────────────────────────┤
│ - id: UUID                                              │
│ - userId: UUID                                          │
│ - balance: Integer                                      │
│ - totalEarned: Integer                                  │
│ - totalSpent: Integer                                   │
├─────────────────────────────────────────────────────────┤
│ + addTokens(amount: int): void                          │
│ + consumeTokens(amount: int): boolean                   │
│ + getBalance(): int                                     │
│ + getTransactionHistory(): List<Transaction>            │
└─────────────────────────────────────────────────────────┘

<<enumeration>>
SubscriptionPlan
────────────────
FREE
STANDARD
PREMIUM
VIP
VIP_PLUS
```

## 5.2 Diagramme de Classes - Module Compagnon

```
┌─────────────────────────────────────────────────────────┐
│                     Companion                           │
├─────────────────────────────────────────────────────────┤
│ - id: String                                            │
│ - userId: String                                        │
│ - name: String                                          │
│ - appearance: Appearance                                │
│ - personality: Personality                              │
│ - voice: Voice                                          │
│ - backstory: String                                     │
│ - geneticProfile: GeneticProfile                        │
│ - emotionalState: EmotionalState                        │
│ - createdAt: LocalDateTime                              │
│ - lastEvolutionDate: LocalDateTime                      │
│ - isPublic: boolean                                     │
├─────────────────────────────────────────────────────────┤
│ + evolve(): void                                        │
│ + updateAppearance(): void                              │
│ + updatePersonality(): void                             │
│ + freezeTraits(): void                                  │
│ + generateResponse(context): String                     │
│ + reactToEmotion(emotion): void                         │
└─────────────────────────────────────────────────────────┘
               │
               │ contains
               │
    ┌──────────┴──────────┬──────────────────────┐
    │                     │                      │
    ▼                     ▼                      ▼
┌─────────────┐   ┌─────────────┐      ┌─────────────┐
│ Appearance  │   │ Personality │      │ Emotional   │
│             │   │             │      │ State       │
├─────────────┤   ├─────────────┤      ├─────────────┤
│ - gender    │   │ - traits    │      │ - current   │
│ - hairColor │   │ - interests │      │ - intensity │
│ - eyeColor  │   │ - humor     │      │ - duration  │
│ - skinTone  │   │ - style     │      │ - triggers  │
│ - avatarUrl │   │             │      │             │
└─────────────┘   └─────────────┘      └─────────────┘

<<enumeration>>
EmotionType
────────────
JOY
SADNESS
ANGER
FEAR
SURPRISE
DISGUST
LOVE
JEALOUSY
EMPATHY
FRUSTRATION
EXCITEMENT
CONTENTMENT
```

---

# 6. DIAGRAMMES DE SÉQUENCE

## 6.1 Séquence - Appel Vidéo VR

```
User        WebApp      Gateway     VRService   AIService   CompanionDB
 │            │           │            │            │            │
 │──Request──>│           │            │            │            │
 │  VR Call   │           │            │            │            │
 │            │           │            │            │            │
 │            │──Auth────>│            │            │            │
 │            │  Check    │            │            │            │
 │            │<──────────│            │            │            │
 │            │  OK       │            │            │            │
 │            │           │            │            │            │
 │            │──Get Comp─┼───────────┼───────────>│            │
 │            │  Profile  │            │            │            │
 │            │<──────────┼───────────┼────────────│            │
 │            │           │            │            │            │
 │            │──Create──>│            │            │            │
 │            │  VR Session            │            │            │
 │            │<──────────│            │            │            │
 │            │  SessionID│            │            │            │
 │            │           │            │            │            │
 │<───URL─────│           │            │            │            │
 │  VR Client │           │            │            │            │
 │            │           │            │            │            │
 │═══════════════════════════════════════════════════════════════│
 │           VR CLIENT CONNECTS                                  │
 │═══════════════════════════════════════════════════════════════│
 │                      │            │            │            │
 │──────WebRTC────────────────────>│            │            │
 │  Connection                      │            │            │
 │<─────────────────────────────────│            │            │
 │  Established                     │            │            │
 │                                  │            │            │
 │──────Stream──────────────────────>│           │            │
 │  Position                        │            │            │
 │  Rotation                        │            │            │
 │  Hand Track                      │            │            │
 │                                  │            │            │
 │                                  │──Request──>│            │
 │                                  │  Avatar    │            │
 │                                  │  Animation │            │
 │                                  │<───────────│            │
 │                                  │  Frame     │            │
 │                                  │  Data      │            │
 │                                  │            │            │
 │<─────Video──────────────────────│            │            │
 │  Stream                         │            │            │
 │  (Animated)                     │            │            │
 │                                  │            │            │
 │──Voice─────────────────────────>│            │            │
 │  Input                          │            │            │
 │                                  │──STT──────>│            │
 │                                  │            │            │
 │                                  │            │──Process──>│
 │                                  │            │  Message   │
 │                                  │            │<───────────│
 │                                  │            │  Response  │
 │                                  │            │            │
 │                                  │<──TTS──────│            │
 │                                  │  +LipSync  │            │
 │                                  │            │            │
 │<─────Audio─────────────────────│            │            │
 │  +Animation                     │            │            │
 │                                  │            │            │
```

---

# 7. ARCHITECTURE BASE DE DONNÉES

## 7.1 Schéma Relationnel PostgreSQL Complet

```sql
-- ═══════════════════════════════════════════════════════
-- NEXUSAI DATABASE SCHEMA (PostgreSQL 16+)
-- ═══════════════════════════════════════════════════════

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- Full-text search
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- Encryption

-- ═══════════════════════════════════════════════════════
-- USERS & AUTHENTICATION
-- ═══════════════════════════════════════════════════════

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    birth_date DATE NOT NULL,
    role VARCHAR(20) DEFAULT 'USER' CHECK (role IN ('USER', 'MODERATOR', 'ADMIN')),
    email_verified BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_active ON users(active);

-- ═══════════════════════════════════════════════════════
-- KYC VERIFICATIONS
-- ═══════════════════════════════════════════════════════

CREATE TABLE kyc_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    level INTEGER NOT NULL CHECK (level BETWEEN 0 AND 3),
    provider VARCHAR(50), -- 'ONFIDO', 'JUMIO', etc.
    provider_id VARCHAR(255), -- External verification ID
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED')),
    document_type VARCHAR(50), -- 'ID_CARD', 'PASSPORT', 'DRIVERS_LICENSE'
    document_number_encrypted BYTEA, -- Encrypted
    selfie_verified BOOLEAN DEFAULT FALSE,
    liveness_check BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, level)
);

CREATE INDEX idx_kyc_user ON kyc_verifications(user_id);
CREATE INDEX idx_kyc_status ON kyc_verifications(status);
CREATE INDEX idx_kyc_expires ON kyc_verifications(expires_at);

-- ═══════════════════════════════════════════════════════
-- SUBSCRIPTIONS
-- ═══════════════════════════════════════════════════════

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE UNIQUE,
    plan VARCHAR(20) NOT NULL CHECK (plan IN ('FREE', 'STANDARD', 'PREMIUM', 'VIP', 'VIP_PLUS')),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    auto_renewal BOOLEAN DEFAULT TRUE,
    monthly_price DECIMAL(10,2),
    stripe_subscription_id VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CANCELED', 'SUSPENDED', 'EXPIRED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_end_date ON subscriptions(end_date);

-- ═══════════════════════════════════════════════════════
-- TOKEN SYSTEM
-- ═══════════════════════════════════════════════════════

CREATE TABLE token_wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE UNIQUE,
    balance INTEGER DEFAULT 0 CHECK (balance >= 0),
    total_earned INTEGER DEFAULT 0,
    total_spent INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE token_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID REFERENCES token_wallets(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('PURCHASE', 'EARN', 'SPEND', 'REFUND', 'EXPIRE')),
    amount INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    description TEXT,
    reference_id UUID, -- Reference to purchase, generation, etc.
    reference_type VARCHAR(50), -- 'IMAGE_GENERATION', 'VIDEO_GENERATION', etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_token_transactions_wallet ON token_transactions(wallet_id);
CREATE INDEX idx_token_transactions_created ON token_transactions(created_at DESC);

-- ═══════════════════════════════════════════════════════
-- PAYMENTS
-- ═══════════════════════════════════════════════════════

CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    type VARCHAR(50) NOT NULL, -- 'SUBSCRIPTION', 'TOKEN_PURCHASE', 'ONE_TIME'
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    payment_method VARCHAR(50), -- 'CARD', 'PAYPAL', 'APPLE_PAY', etc.
    stripe_payment_intent_id VARCHAR(255),
    stripe_charge_id VARCHAR(255),
    refund_amount DECIMAL(10,2) DEFAULT 0,
    refunded_at TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_user ON payment_transactions(user_id);
CREATE INDEX idx_payments_status ON payment_transactions(status);
CREATE INDEX idx_payments_created ON payment_transactions(created_at DESC);

-- ═══════════════════════════════════════════════════════
-- GENERATED CONTENT (IMAGES & VIDEOS)
-- ═══════════════════════════════════════════════════════

CREATE TABLE generated_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    companion_id VARCHAR(255), -- MongoDB reference
    prompt TEXT NOT NULL,
    style VARCHAR(50), -- 'REALISTIC', 'ANIME', 'ARTISTIC', etc.
    resolution VARCHAR(20), -- '512x512', '1024x1024', '2048x2048'
    status VARCHAR(20) DEFAULT 'QUEUED' CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    storage_url TEXT,
    thumbnail_url TEXT,
    seed INTEGER,
    parameters JSONB,
    generation_time_seconds INTEGER,
    is_favorite BOOLEAN DEFAULT FALSE,
    is_public BOOLEAN DEFAULT FALSE,
    tokens_cost INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_images_user ON generated_images(user_id);
CREATE INDEX idx_images_status ON generated_images(status);
CREATE INDEX idx_images_created ON generated_images(created_at DESC);
CREATE INDEX idx_images_companion ON generated_images(companion_id);

CREATE TABLE generated_videos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    companion_id VARCHAR(255), -- MongoDB reference
    prompt TEXT NOT NULL,
    scenario_json JSONB NOT NULL, -- Detailed scenario
    duration_seconds INTEGER NOT NULL,
    resolution VARCHAR(20), -- '1080p', '4K'
    frame_rate INTEGER, -- 30, 60
    quality VARCHAR(20), -- 'STANDARD', 'HD', '4K_ADVANCED'
    status VARCHAR(20) DEFAULT 'QUEUED' CHECK (status IN ('QUEUED', 'SCRIPT_GEN', 'ASSET_GEN', 'RENDERING', 'ENCODING', 'COMPLETED', 'FAILED')),
    storage_url TEXT,
    thumbnail_urls TEXT[], -- Multiple thumbnails
    file_size_mb DECIMAL(10,2),
    generation_time_minutes INTEGER,
    is_favorite BOOLEAN DEFAULT FALSE,
    tokens_cost INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_videos_user ON generated_videos(user_id);
CREATE INDEX idx_videos_status ON generated_videos(status);
CREATE INDEX idx_videos_created ON generated_videos(created_at DESC);

-- ═══════════════════════════════════════════════════════
-- MODERATION
-- ═══════════════════════════════════════════════════════

CREATE TABLE moderation_incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    content_type VARCHAR(50) NOT NULL, -- 'TEXT', 'IMAGE', 'VIDEO', etc.
    content_hash VARCHAR(64), -- SHA-256 hash (not content itself)
    conversation_id VARCHAR(255), -- MongoDB reference
    message_id VARCHAR(255),
    
    incident_type VARCHAR(50) NOT NULL, -- 'SEXUAL', 'VIOLENCE', 'HATE', 'CSAM', etc.
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    confidence FLOAT, -- AI confidence score
    moderation_scores JSONB, -- Detailed scores from AI
    
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'CONFIRMED', 'FALSE_POSITIVE', 'RESOLVED')),
    automated BOOLEAN DEFAULT TRUE,
    
    reviewed_by UUID REFERENCES users(id), -- Moderator
    reviewed_at TIMESTAMP,
    action_taken VARCHAR(100), -- 'WARNING', 'SUSPENSION_1D', 'BAN', etc.
    notes TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_moderation_user ON moderation_incidents(user_id);
CREATE INDEX idx_moderation_status ON moderation_incidents(status);
CREATE INDEX idx_moderation_severity ON moderation_incidents(severity);
CREATE INDEX idx_moderation_created ON moderation_incidents(created_at DESC);

CREATE TABLE user_warnings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    incident_id UUID REFERENCES moderation_incidents(id),
    warning_type VARCHAR(50) NOT NULL,
    description TEXT,
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_warnings_user ON user_warnings(user_id);
CREATE INDEX idx_warnings_expires ON user_warnings(expires_at);

-- ═══════════════════════════════════════════════════════
-- CONSENT MANAGEMENT (VIP+)
-- ═══════════════════════════════════════════════════════

CREATE TABLE adult_content_consents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    consent_type VARCHAR(50) NOT NULL, -- 'UNMODERATED_MODE', 'ADULT_DEVICES', etc.
    version VARCHAR(10) NOT NULL, -- Version of consent form
    ip_address INET NOT NULL,
    user_agent TEXT,
    digital_signature TEXT NOT NULL, -- Cryptographic signature
    signed_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_consents_user ON adult_content_consents(user_id);
CREATE INDEX idx_consents_type ON adult_content_consents(consent_type);
CREATE INDEX idx_consents_active ON adult_content_consents(user_id, consent_type) WHERE NOT revoked;

-- ═══════════════════════════════════════════════════════
-- VR SESSIONS
-- ═══════════════════════════════════════════════════════

CREATE TABLE vr_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    companion_id VARCHAR(255), -- MongoDB reference
    headset_type VARCHAR(50), -- 'QUEST_3', 'INDEX', 'PSVR2', etc.
    rendering_mode VARCHAR(20), -- 'LOCAL', 'CLOUD_STREAM'
    quality_preset VARCHAR(20), -- 'PERFORMANCE', 'BALANCED', 'ULTRA'
    
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    duration_minutes INTEGER,
    
    average_fps FLOAT,
    min_fps FLOAT,
    latency_ms FLOAT,
    
    interactions_count INTEGER DEFAULT 0,
    hand_tracking_used BOOLEAN DEFAULT FALSE,
    haptic_feedback_used BOOLEAN DEFAULT FALSE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vr_sessions_user ON vr_sessions(user_id);
CREATE INDEX idx_vr_sessions_started ON vr_sessions(started_at DESC);

-- ═══════════════════════════════════════════════════════
-- AUDIT LOGS (Compliance)
-- ═══════════════════════════════════════════════════════

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID, -- Can be NULL for system events
    action VARCHAR(100) NOT NULL, -- 'LOGIN', 'KYC_VERIFICATION', 'MODE_ACTIVATION', etc.
    resource_type VARCHAR(50), -- 'USER', 'COMPANION', 'CONTENT', etc.
    resource_id VARCHAR(255),
    ip_address INET,
    user_agent TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);

-- ═══════════════════════════════════════════════════════
-- TRIGGERS FOR UPDATED_AT
-- ═══════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_subscriptions_updated_at BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_token_wallets_updated_at BEFORE UPDATE ON token_wallets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ═══════════════════════════════════════════════════════
-- VIEWS FOR ANALYTICS
-- ═══════════════════════════════════════════════════════

CREATE VIEW user_subscription_status AS
SELECT 
    u.id,
    u.email,
    u.username,
    s.plan,
    s.status as subscription_status,
    s.end_date,
    tw.balance as token_balance,
    u.created_at as user_since
FROM users u
LEFT JOIN subscriptions s ON u.id = s.user_id
LEFT JOIN token_wallets tw ON u.id = tw.user_id;

CREATE VIEW moderation_summary AS
SELECT 
    user_id,
    COUNT(*) as total_incidents,
    SUM(CASE WHEN severity = 'CRITICAL' THEN 1 ELSE 0 END) as critical_count,
    SUM(CASE WHEN severity = 'HIGH' THEN 1 ELSE 0 END) as high_count,
    MAX(created_at) as last_incident
FROM moderation_incidents
GROUP BY user_id;
```

---

**FIN DU DOCUMENT DIAGRAMMES TECHNIQUES**

Document généré le : 15 Janvier 2025  
Version : 1.0  
Classification : Confidentiel

---