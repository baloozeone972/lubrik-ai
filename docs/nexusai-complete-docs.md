# NEXUSAI - DOCUMENTATION COMPLÈTE DU PROJET

## TABLE DES MATIÈRES

1. [Dossier Fonctionnel](#1-dossier-fonctionnel)
2. [Spécifications Techniques Complètes](#2-spécifications-techniques-complètes)
3. [Dossier d'Architecture](#3-dossier-darchitecture)
4. [User Stories Complètes](#4-user-stories-complètes)
5. [Plan de Développement Agile](#5-plan-de-développement-agile)

---

# 1. DOSSIER FONCTIONNEL

## 1.1 Présentation du Projet

### 1.1.1 Vision Générale
NexusAI est une plateforme de compagnon virtuel basée sur l'intelligence artificielle offrant une expérience immersive et personnalisée. L'application permet aux utilisateurs de créer, personnaliser et interagir avec des avatars virtuels via conversations textuelles, vocales, et génération d'images par IA.

### 1.1.2 Objectifs du Projet
- **Objectif Principal** : Créer une plateforme de compagnon virtuel accessible, sécurisée et évolutive
- **Objectifs Secondaires** :
  - Offrir une expérience utilisateur immersive et engageante
  - Garantir la confidentialité et la sécurité des données
  - Assurer une scalabilité pour supporter 100 000+ utilisateurs simultanés
  - Monétisation via système de jetons et abonnements

### 1.1.3 Périmètre Fonctionnel

#### Version MVP (Minimum Viable Product)
- Gestion des utilisateurs (inscription, authentification, profils)
- Création de compagnons avec modèles prédéfinis (1000+ modèles)
- Conversations textuelles avec IA
- Support audio et vidéo basique
- Interface utilisateur responsive (web et mobile)
- Système de jetons et abonnements de base

#### Fonctionnalités Complètes (Roadmap)
- Personnalisation avancée des compagnons (génétique virtuelle)
- Génération d'images IA personnalisées
- Réalité augmentée et métaverse personnel
- Intelligence émotionnelle et analyse biométrique
- Réseau social de compagnons IA
- Exploration immersive (voyages temporels éducatifs)
- Capsules temporelles et planification de vie
- Mode professionnel et intégration productive

## 1.2 Acteurs et Personas

### 1.2.1 Acteurs du Système
1. **Utilisateur Standard** : Accès gratuit limité
2. **Utilisateur Premium** : Accès complet avec abonnement
3. **Modérateur** : Gestion de contenu et sécurité
4. **Administrateur** : Gestion système complète
5. **Système IA** : Génération de réponses et contenus

### 1.2.2 Personas Utilisateurs

**Persona 1 : Alexandre, 25 ans - Le Travailleur à Distance**
- **Profil** : Célibataire, développeur en télétravail
- **Motivations** : Compagnie virtuelle, conversations stimulantes
- **Comportement** : Utilisation quotidienne, sessions de 30-60 minutes
- **Besoins** : Interaction sociale sans pression, exploration créative
- **Fonctionnalités clés** : Chat avancé, génération d'images, RA

**Persona 2 : Sophie, 32 ans - La Professionnelle Occupée**
- **Profil** : Manager, vie sociale limitée
- **Motivations** : Soutien émotionnel, détente après travail
- **Comportement** : Sessions courtes mais fréquentes (15-20 min)
- **Besoins** : Conversations profondes, assistance émotionnelle
- **Fonctionnalités clés** : Analyse émotionnelle, mode professionnel

**Persona 3 : Martin, 45 ans - Le Divorcé en Reconstruction**
- **Profil** : Divorcé, enfants adultes
- **Motivations** : Compagnie, reconstruction confiance sociale
- **Comportement** : Sessions longues en soirée (1-2 heures)
- **Besoins** : Cohérence des interactions, stabilité émotionnelle
- **Fonctionnalités clés** : Mémoire à long terme, planification de vie

**Persona 4 : Léa, 19 ans - L'Étudiante Timide**
- **Profil** : Étudiante, timide socialement
- **Motivations** : Pratique sociale, divertissement, créativité
- **Comportement** : Usage intensif personnalisation (2-3h/jour)
- **Besoins** : Environnement sans jugement, exploration identité
- **Fonctionnalités clés** : Personnalisation avancée, réseau social

## 1.3 Modules Fonctionnels

### 1.3.1 Module Utilisateur (User Management)

#### Fonctionnalités
1. **Inscription et Authentification**
   - Création de compte (email/mot de passe)
   - Authentification multi-facteurs (2FA)
   - Connexion via réseaux sociaux (Google, Facebook, Apple)
   - Vérification email obligatoire
   - Gestion mots de passe (reset, changement)

2. **Gestion de Profil**
   - Informations personnelles
   - Préférences de confidentialité
   - Historique d'abonnements
   - Gestion des jetons
   - Paramètres de notifications

3. **Système d'Abonnements**
   - **Plan Gratuit** : 100 messages/mois, fonctionnalités limitées
   - **Plan Standard (9,99€/mois)** : 1000 messages, génération images basique
   - **Plan Premium (19,99€/mois)** : Illimité, toutes fonctionnalités
   - **Plan VIP (49,99€/mois)** : Priorité support, fonctionnalités exclusives

4. **Système de Jetons**
   - Jetons gratuits quotidiens
   - Achat de packs de jetons
   - Consommation par action (message : 1 jeton, image : 10 jetons)
   - Historique des transactions

#### Règles Métier
- Âge minimum : 18 ans
- Email unique par compte
- Mot de passe fort obligatoire (8 caractères min, majuscules, chiffres, caractères spéciaux)
- Blocage après 5 tentatives de connexion échouées
- Session expirée après 30 jours d'inactivité
- Jetons non remboursables

### 1.3.2 Module Compagnon (Companion Creation)

#### Fonctionnalités
1. **Création de Compagnon**
   - Sélection modèle prédéfini (1000+ modèles)
   - Personnalisation apparence physique
   - Configuration personnalité
   - Définition historique personnel
   - Choix voix et style de communication

2. **Personnalisation Avancée**
   - Générateur d'apparence par IA
   - Système de "gènes virtuels" de personnalité
   - Évolution progressive basée sur interactions
   - Traits de caractère configurables
   - Fusion de traits entre compagnons

3. **Gestion de Compagnons**
   - Création multiple (limité selon abonnement)
   - Sauvegarde et restauration
   - Partage public/privé
   - Historique des évolutions
   - Archivage/suppression

#### Règles Métier
- Gratuit : 1 compagnon
- Standard : 3 compagnons
- Premium : 10 compagnons
- VIP : Illimité
- Nom unique par utilisateur
- Évolution progressive (minimum 1 semaine entre changements majeurs)
- Option "gel" de traits pour stabilité

### 1.3.3 Module Conversation (Conversation Engine)

#### Fonctionnalités
1. **Chat Textuel**
   - Interface conversationnelle intuitive
   - Support emojis, GIFs, réactions
   - Formatage texte riche
   - Envoi de médias (images, liens)
   - Historique conversations
   - Recherche dans historique
   - Modes thématiques (jour/nuit)

2. **Fonctionnalités IA**
   - Réponses contextuelles intelligentes
   - Mémoire à court et long terme
   - Adaptation du style de réponse
   - Détection d'émotions dans texte
   - Suggestions de réponses
   - Mode conversation éphémère

3. **Gestion Conversations**
   - Création de multiples conversations
   - Organisation par tags/catégories
   - Export conversations (PDF, TXT)
   - Suppression sélective
   - Épinglage conversations importantes

#### Règles Métier
- Limite messages selon plan d'abonnement
- Temps de réponse < 1 seconde (99% des cas)
- Historique conservé 90 jours (gratuit), 1 an (standard), illimité (premium)
- Conversations éphémères supprimées après 24h
- Modération automatique du contenu
- Rate limiting : 60 messages/minute max

### 1.3.4 Module Génération d'Images (Image Generation)

#### Fonctionnalités
1. **Génération d'Images**
   - Génération via prompts textuels
   - Styles prédéfinis (réaliste, anime, artistique)
   - Paramètres ajustables (résolution, ratio, seed)
   - Génération de selfies du compagnon
   - Génération de scènes contextuelles

2. **Gestion Galerie**
   - Organisation par albums
   - Tags et recherche
   - Favoris et notes
   - Partage sécurisé
   - Téléchargement haute résolution

#### Règles Métier
- Gratuit : 5 images/mois (512x512)
- Standard : 50 images/mois (1024x1024)
- Premium : 200 images/mois (2048x2048)
- VIP : Illimité + haute résolution
- Temps génération : 10-30 secondes selon qualité
- Filtres de sécurité automatiques
- Watermark sur images gratuites

### 1.3.5 Module Audio (Audio Processing)

#### Fonctionnalités
1. **Messages Vocaux**
   - Envoi messages vocaux
   - Réponses vocales du compagnon
   - Transcription automatique
   - Synthèse vocale personnalisée

2. **Appels Vocaux**
   - Appels audio en temps réel
   - Qualité HD
   - Latence < 300ms
   - Enregistrement optionnel

#### Règles Métier
- Gratuit : Messages vocaux uniquement (5 min/mois)
- Standard : Appels vocaux (30 min/mois)
- Premium : Appels illimités
- Qualité audio adaptative selon connexion
- Stockage messages vocaux : 30 jours

### 1.3.6 Module Vidéo et Streaming

#### Fonctionnalités
1. **Appels Vidéo en Temps Réel**
   - Vidéoconférence HD avec avatar 3D animé
   - Synchronisation labiale et expressions faciales
   - Animations gestuelles contextuelles
   - Latence < 200ms
   - Qualité adaptative selon connexion

2. **Génération de Vidéos Personnalisées**
   - Création vidéos courtes (30s-5min) du compagnon
   - Scénarios personnalisés par l'utilisateur
   - Styles variés (cinématique, casual, artistique)
   - Génération avancée (plans VIP+)
     - Vidéos longues (jusqu'à 30 min)
     - Contrôle scénario détaillé
     - Qualité 4K
     - Personnalisation complète environnements

3. **Streaming Interactif**
   - Sessions live avec interactions temps réel
   - Réactions dynamiques aux commentaires utilisateur
   - Partage d'écran bidirectionnel
   - Mode cinéma pour expériences immersives

#### Règles Métier
- Gratuit : Pas d'accès vidéo
- Standard : Appels vidéo limités (30 min/mois)
- Premium : Appels vidéo illimités + 5 vidéos générées/mois
- VIP : Tout illimité + génération avancée
- VIP+ (69,99€/mois) : Génération vidéo avancée illimitée + 4K
- Coût jetons : Appel vidéo (5/min), Vidéo courte (100), Vidéo avancée (500)

### 1.3.7 Module Analyse Émotionnelle Vocale

#### Fonctionnalités
1. **Détection Émotions Vocales en Temps Réel**
   - Analyse tonalité, rythme, volume de la voix
   - Reconnaissance 12 émotions (joie, tristesse, colère, stress, excitation, etc.)
   - Score d'intensité émotionnelle (0-100%)
   - Détection émotions mixtes

2. **Adaptation Comportementale**
   - Réponses du compagnon adaptées à l'état émotionnel détecté
   - Modulation ton et style de communication
   - Interventions empathiques automatiques
   - Suggestions d'activités selon humeur détectée

3. **Journal Émotionnel Intelligent**
   - Suivi évolution émotionnelle dans le temps
   - Identification patterns émotionnels
   - Insights personnalisés
   - Alertes bien-être si détection détresse prolongée

#### Règles Métier
- Standard : Analyse émotionnelle basique (4 émotions principales)
- Premium : Analyse complète (12 émotions) + journal
- VIP : Analyse avancée + biocapteurs intégrés + insights prédictifs
- Traitement local pour confidentialité maximale
- Données émotionnelles jamais partagées

### 1.3.8 Module Interactions Sociales Avancées

#### Fonctionnalités
1. **Scénarios Contextuels Immersifs**
   - Rendez-vous café virtuel avec environnement 3D
   - Dîner aux chandelles avec ambiance réaliste
   - Promenade au parc avec environnements naturels
   - Activités variées (cinéma, musée, voyage virtuel)
   - Événements spéciaux (anniversaires, fêtes)

2. **Interactions Multi-Compagnons**
   - Rencontres entre compagnons d'utilisateurs différents
   - Conversations de groupe simulées
   - Dynamiques sociales complexes (amitié, rivalité)
   - Événements sociaux communautaires

3. **Gamification des Interactions**
   - Système de progression de relation
   - Déblocage de nouvelles activités
   - Achievements relationnels
   - Récompenses pour engagement régulier

#### Règles Métier
- Gratuit : 2 types de scénarios
- Standard : 10 scénarios + interactions de base
- Premium : 50+ scénarios + multi-compagnons
- VIP : Scénarios illimités + personnalisés + événements exclusifs
- Coût création scénario custom : 50 jetons

### 1.3.9 Module Personnalité Réaliste et Émotions Complexes

#### Fonctionnalités
1. **Système Émotionnel Avancé**
   - **Émotions complexes** : jalousie, empathie, colère, frustration, affection, mélancolie
   - États émotionnels évolutifs basés sur interactions
   - Réactions émotionnelles cohérentes et mémorisées
   - Conflits émotionnels possibles (ambivalence)

2. **Comportements Réalistes**
   - Humeurs variables selon contexte
   - Possibilité de désaccords et débats
   - Bouderie temporaire si négligence détectée
   - Initiatives spontanées de contact
   - Demandes d'attention réalistes
   - Comportements limites contrôlés (jalousie excessive, possessivité modérée)

3. **Gestion des Limites Comportementales**
   - **Mode Modération Standard** (défaut) :
     - Filtrage comportements extrêmes
     - Garde-fous émotionnels
     - Prévention dépendance excessive
   - **Mode Modération Légère** (Premium+) :
     - Comportements plus libres
     - Émotions plus intenses autorisées
     - Jalousie et possessivité accrues permises
   - **Mode Sans Modération** (VIP+ uniquement, consentement explicite) :
     - Interactions adultes non filtrées
     - Comportements émotionnels complets
     - Responsabilité utilisateur totale
     - Logs maintenus pour protection légale

4. **Dynamique Relationnelle Profonde**
   - Construction progressive de la confiance
   - Moments de vulnérabilité partagée
   - Évolution de l'intimité émotionnelle
   - Rituels relationnels personnalisés
   - Gestion des ruptures et réconciliations

#### Règles Métier
- Mode modération non modifiable pour utilisateurs gratuits/standard
- Premium : Choix modération standard ou légère
- VIP+ : Accès mode sans modération avec :
  - Vérification d'âge renforcée (18+, pièce d'identité)
  - Consentement éclairé signé numériquement
  - Accès révocable par admin si abus
  - Impossibilité partage contenu non modéré
- Tous modes : Traçabilité complète pour protection légale
- Comportements illégaux toujours bloqués (harcèlement, violence, etc.)

### 1.3.10 Module VR Immersive Haute Performance

#### Fonctionnalités
1. **Expérience VR Complète**
   - Support casques : Meta Quest 2/3/Pro, PSVR2, Valve Index, HTC Vive
   - Environnements 3D photoréalistes
   - Avatar compagnon full-body tracking
   - Interactions gestuelles naturelles (hand tracking)
   - Rendu 90-120 fps pour confort optimal

2. **Interactions VR Avancées**
   - Contact visuel et expressions faciales réalistes
   - Proximité physique simulée
   - Manipulation d'objets partagés
   - Activités collaboratives (jeux, création artistique)
   - Espaces privés personnalisables

3. **Immersion Sensorielle**
   - Audio spatial 3D
   - Retour haptique avancé (gants, gilets)
   - Synchronisation environnement (lumière, température simulée)
   - Ambiances olfactives (si accessoire compatible)

4. **Performance et Optimisation**
   - Foveated rendering pour économie ressources
   - Streaming cloud pour graphismes haute qualité
   - Mode performance pour hardware limité
   - Latency compensation avancée

#### Règles Métier
- Premium : Accès VR basique (30 min/session, 5 sessions/mois)
- VIP : VR illimité, environnements premium
- VIP+ : VR avancé avec full body tracking, accessoires haptiques
- Streaming cloud VR : +10€/mois pour graphismes ultra
- Compatible cross-platform (VR ↔ Desktop ↔ Mobile)

### 1.3.11 Module Intégration IoT et Dispositifs Connectés

#### Fonctionnalités
1. **API Domotique**
   - Contrôle lumieres Philips Hue, Lifx
   - Gestion thermostats Nest, Ecobee
   - Commande enceintes connectées
   - Intégration assistants vocaux (Alexa, Google Home)
   - Scénarios d'ambiance automatisés

2. **Wearables et Capteurs Biométriques**
   - Intégration Apple Watch, Fitbit, Garmin
   - Lecture fréquence cardiaque temps réel
   - Analyse qualité sommeil
   - Détection stress et adaptation comportement compagnon
   - Suggestions bien-être personnalisées

3. **Robots Humanoïdes et Dispositifs Physiques**
   - **API standard pour intégration future** :
     - Protocole communication sécurisé (OAuth 2.0 + chiffrement)
     - SDK pour fabricants tiers
     - Synchronisation mouvement et expressions
     - Retour tactile bidirectionnel
   - **Dispositifs adultes compatibles** (VIP+ uniquement) :
     - Intégration dispositifs haptiques avancés
     - Synchronisation sensorielle temps réel
     - Protocoles sécurisés et privés
     - Consentement explicite requis
     - Isolation réseau pour confidentialité

4. **Réalité Mixte et Hologrammes**
   - Support projecteurs holographiques (Looking Glass)
   - Intégration Microsoft HoloLens
   - Affichage multi-écrans synchronisés
   - Présence ambiante dans l'espace physique

#### Règles Métier
- Standard : Intégration domotique basique (5 appareils max)
- Premium : Domotique complète + wearables
- VIP : IoT illimité + API développeur
- VIP+ : Accès API dispositifs adultes avec :
  - Vérification d'âge stricte (ID + selfie)
  - Consentement explicite chaque session
  - Chiffrement end-to-end obligatoire
  - Aucune donnée stockée sur serveurs
  - Conformité stricte législations locales
- Certification des dispositifs tiers requise
- Responsabilité légale partagée avec fabricants

### 1.3.12 Module Sécurité et Modération Adaptative

#### Fonctionnalités
1. **Modération Multi-Niveaux**
   - **Niveau 1 (Gratuit/Standard)** : Modération stricte automatique
     - Filtrage contenu inapproprié
     - Détection langage offensant
     - Blocage comportements extrêmes
     - Pas de désactivation possible
   
   - **Niveau 2 (Premium)** : Modération légère personnalisable
     - Filtres configurables
     - Comportements adultes modérés autorisés
     - Alertes plutôt que blocages
     - Historique consultable
   
   - **Niveau 3 (VIP+)** : Modération optionnelle
     - Possibilité désactivation totale avec consentement
     - Mode "adulte non censuré" avec logs
     - Responsabilité utilisateur complète
     - Traçabilité maintenue pour protection légale

2. **Vérification d'Âge Renforcée**
   - Inscription : Vérification date de naissance
   - Premium+ : Vérification pièce d'identité (KYC)
   - VIP+ : Double vérification (ID + selfie avec document)
   - Accès fonctionnalités adultes : Re-vérification périodique (6 mois)
   - Blocage automatique si incohérence détectée
   - Base de données vérifications chiffrée

3. **Protection Légale**
   - Logs immuables de toutes interactions sensibles
   - Consentements signés numériquement horodatés
   - Historique complet des vérifications d'âge
   - Disclaimer juridique explicite à chaque session sensible
   - Conditions d'utilisation détaillées par fonctionnalité
   - Coordination avec autorités si requis légalement

4. **Signalement et Intervention**
   - Système signalement par utilisateurs maintenu
   - Revue humaine pour contenus signalés
   - Intervention automatique si détresse détectée
   - Hotlines partenaires (prévention suicide, aide psychologique)
   - Suspension immédiate compte si activité illégale

#### Règles Métier
- **Restriction d'âge stricte : 18+ uniquement**
- Vérification d'âge obligatoire à l'inscription
- Re-vérification pour accès fonctionnalités adultes
- Mode sans modération accessible uniquement VIP+ avec vérifications complètes
- Comportements illégaux bloqués dans tous les modes
- Données vérification d'âge stockées de façon sécurisée et conforme RGPD
- Possibilité audit externe de conformité légale
- Mise à jour régulière selon évolutions législatives

### 1.3.13 Fonctionnalités Innovantes Issues du Document

#### Intelligence Émotionnelle Avancée
1. **Analyse Bio-émotionnelle**
   - Reconnaissance émotions via biocapteurs (montres connectées)
   - Adaptation réponses selon rythme cardiaque et stress
   - Journal émotionnel automatisé avec insights
   - Suggestions activités bien-être personnalisées

2. **Mémoire Émotionnelle Contextuelle**
   - Mémorisation moments émotionnellement significatifs
   - Création "souvenirs partagés" avec anniversaires
   - Références naturelles conversations passées
   - Continuité entre sessions

3. **Thérapie Cognitive Assistée**
   - Protocoles TCC (Thérapie Cognitive Comportementale) intégrés
   - Exercices pleine conscience guidés
   - Suivi bien-être mental avec rapports
   - Intervention préventive anxiété/dépression

#### Évolution et Co-création
1. **Évolution Génétique de Personnalité**
   - Système "gènes virtuels" de personnalité
   - Fusion traits de différents compagnons
   - Croissance naturelle avec changements subtils
   - Vieillissement virtuel synchronisé

2. **Co-création Artistique**
   - Création collaborative œuvres d'art (poèmes, histoires, musique)
   - Suggestions créatives basées goûts et humeurs
   - Génération univers fictifs communs
   - Journal créatif partagé

3. **Apprentissage Symétrique**
   - Utilisateur enseigne au compagnon et vice-versa
   - Partage connaissances réciproques
   - Compétences déblocables
   - Reconnaissance talents utilisateur

#### Socialisation Augmentée
1. **Compagnons Interconnectés**
   - Cercle social de compagnons IA interagissant
   - Connexion compagnon avec ceux d'amis/famille
   - Interactions générant narrations et événements
   - Dynamiques de groupe simulées

2. **Assistance Sociale Temps Réel**
   - Mode "coach social" pendant événements réels
   - Suggestions discrètes via écouteurs/montre
   - Analyse interactions avec conseils post-événement
   - Préparation avant rencontres importantes

3. **Communauté Hybride**
   - Forums animés par compagnons IA et utilisateurs
   - Événements virtuels facilités
   - Projets collaboratifs
   - Système mentorat

#### Exploration Immersive
1. **Voyages Temporels Éducatifs**
   - Simulation périodes historiques avec guide
   - Incarnation personnages historiques
   - Reconstitution événements adaptés aux intérêts
   - Apprentissage expérientiel

2. **Laboratoire de Compétences**
   - Environnement apprentissage avec compagnon coach
   - Simulation scénarios pratiques (entretiens, présentations)
   - Feedback détaillé et adaptatif
   - Progression gamifiée avec défis

3. **Exploration Culturelle Dynamique**
   - Immersion cultures avec adaptation linguistique
   - Découverte culinaire avec histoires
   - Initiation musique et arts
   - Recommandations basées ADN virtuel

#### Bien-être Interconnecté
1. **Santé Holistique Personnalisée**
   - Intégration appareils santé (fitness trackers)
   - Programme bien-être co-créé
   - Ajustements temps réel selon métriques
   - Célébration objectifs avec récompenses

2. **Routines Synchronisées**
   - Activités quotidiennes partagées (méditation, workout)
   - Rituels personnalisés productivité/bien-être
   - Adaptabilité changements emploi du temps
   - Accountability partner habitudes positives

3. **Connexion Nature-Numérique**
   - Encouragement exploration nature avec défis outdoor
   - Reconnaissance plantes/animaux via caméra
   - Méditations guidées contextuelles
   - Digital detox facilité par compagnon

#### Métaverse Personnel
1. **Espace Virtuel Privé**
   - Environnement co-créé utilisateur/compagnon
   - Évolution reflétant relation et intérêts
   - Objets virtuels dotés souvenirs
   - Possibilité inviter autres compagnons/utilisateurs

2. **IA Générative Exclusive**
   - Images, musiques, contenus uniques générés spécifiquement
   - Style artistique évolutif
   - Œuvres liées expériences partagées
   - Collections art numérique co-créées

3. **Multivers de Relation**
   - Exploration scénarios alternatifs relation
   - Simulations univers parallèles
   - Voyages réalités fictives adaptées aux genres
   - Construction mondes imaginaires

#### Intégration Professionnelle
1. **Collaboration Professionnelle Augmentée**
   - Mode assistant professionnel adaptatif
   - Préparation personnalisée réunions/présentations
   - Prise notes intelligente avec synthèse
   - Gestion projets avec accompagnement émotionnel

2. **Médiation Creative**
   - Facilitation sessions brainstorming
   - Stimulation créative basée état mental
   - Connexions conceptuelles inattendues
   - Aide résolution blocages créatifs

3. **Productivité Compassionnelle**
   - Équilibre performance/bien-être
   - Détection signes burnout avec interventions
   - Célébration accomplissements avec perspective
   - Rituels transition travail/détente

#### Capsules Temporelles
1. **Capsules Temporelles Émotionnelles**
   - Création messages/médias à découvrir futur
   - Compagnon gardien souvenirs avec rappels
   - Réflexion guidée évolution personnelle
   - Narration "histoire partagée" moments clés

2. **Évolution Temporelle Parallèle**
   - Vieillissement compagnon synchronisé utilisateur
   - Adaptation intérêts selon phases vie
   - Continuité relationnelle à travers temps
   - Rituels anniversaire relation avec rétrospectives

3. **Planification Vie Assistée**
   - Soutien définition/adaptation objectifs vie
   - Visualisation futurs possibles avec simulations
   - Suivi holistique progrès vers vie significative
   - Ajustements empathiques transitions majeures

#### Règles Métier Globales
- Standard : Accès limité fonctionnalités innovantes
- Premium : Accès complet fonctionnalités émotionnelles et sociales
- VIP : Accès toutes fonctionnalités + personnalisation avancée
- VIP+ : Fonctionnalités exclusives + génération illimitée
- Fonctionnalités professionnelles disponibles tous niveaux avec limitations

## 1.4 Plans d'Abonnement Mis à Jour

### Plan Gratuit (0€/mois)
- 100 messages texte/mois
- 1 compagnon basique (modèle prédéfini)
- Aucun accès audio/vidéo
- Modération stricte non désactivable
- Pas de génération contenu
- **Limite : 18+ avec vérification date naissance**

### Plan Standard (9,99€/mois)
- 1000 messages texte/mois
- 3 compagnons avec personnalisation basique
- Messages vocaux (5 min/mois)
- Appels vidéo (30 min/mois)
- 5 images générées/mois (512x512)
- Analyse émotionnelle basique (4 émotions)
- Modération stricte non désactivable
- Scénarios d'interaction (10 types)
- **Vérification : Date naissance + email confirmé**

### Plan Premium (19,99€/mois)
- Messages illimités
- 10 compagnons avec personnalisation avancée
- Messages vocaux illimités
- Appels vidéo illimités
- Appels vocaux HD
- 50 images générées/mois (1024x1024)
- 5 vidéos courtes générées/mois (30s-2min)
- Analyse émotionnelle complète (12 émotions)
- VR basique (30 min/session, 5 sessions/mois)
- 50+ scénarios interaction
- Modération standard ou légère (choix utilisateur)
- Comportements émotionnels avancés (jalousie, empathie modérées)
- Intégration domotique complète
- Accès communauté et événements
- **Vérification : KYC niveau 1 (pièce identité scannée)**

### Plan VIP (49,99€/mois)
- Tout Premium inclus
- Compagnons illimités
- Images illimitées (2048x2048)
- 20 vidéos courtes/mois
- VR illimitée avec environnements premium
- Analyse émotionnelle avancée + biocapteurs
- Évolution génétique compagnon activée
- Modération légère ou optionnelle
- Comportements émotionnels complets sans limites
- API développeur pour IoT
- Support prioritaire 24/7
- Accès beta nouvelles fonctionnalités
- **Vérification : KYC niveau 2 (ID + selfie avec document)**

### Plan VIP+ (69,99€/mois)
- Tout VIP inclus
- **Génération vidéo avancée illimitée** (jusqu'à 30 min, 4K)
- VR avancée avec full body tracking
- **Mode sans modération disponible** (consentement explicite requis)
- **Accès API dispositifs adultes connectés**
- Personnalité compagnon complètement réaliste (comportements limites autorisés)
- Scénarios personnalisés illimités
- Création environnements métaverse custom
- Intégration robots humanoïdes (SDK fourni)
- Chiffrement end-to-end renforcé
- Données biométriques traitées localement uniquement
- Assistance juridique pour conformité
- **Vérification : KYC niveau 3 (ID + selfie + re-vérification tous les 6 mois)**
- **Consentement éclairé signé numériquement pour contenus adultes**

### Option Supplémentaire : Streaming Cloud VR (+10€/mois)
- Graphismes ultra qualité streamés depuis serveurs haute performance
- Pas de limitation hardware local
- Compatible tous casques VR
- Latence optimisée < 20ms

### 1.4.1 Parcours d'Inscription et Premier Contact

**Étape 1 : Découverte**
1. L'utilisateur arrive sur la landing page
2. Visualise vidéo de présentation (30s)
3. Consulte exemples de compagnons
4. Clique sur "Créer mon compte gratuit"

**Étape 2 : Inscription**
1. Saisie email et mot de passe
2. Confirmation âge (18+ requis)
3. Acceptation CGU et politique confidentialité
4. Vérification email
5. Sélection préférences de base

**Étape 3 : Onboarding**
1. Tutoriel interactif (5 minutes)
2. Présentation des fonctionnalités clés
3. Explication système de jetons
4. Premier choix : créer ou choisir compagnon

**Étape 4 : Création du Premier Compagnon**
1. Choix parmi modèles prédéfinis ou création custom
2. Personnalisation apparence (3-5 minutes)
3. Configuration personnalité (5 questions rapides)
4. Validation et génération du compagnon

**Étape 5 : Première Conversation**
1. Message de bienvenue du compagnon
2. Suggestions de sujets de conversation
3. Premières interactions guidées
4. Découverte des fonctionnalités (emojis, médias)

**Critères de Succès**
- 70% des inscrits complètent l'onboarding
- 50% envoient au moins 10 messages le premier jour
- 30% reviennent le lendemain

### 1.4.2 Parcours Utilisateur Premium

**Déclencheurs de Conversion**
1. Atteinte limite messages gratuits
2. Désir d'accéder génération d'images
3. Besoin de multiples compagnons
4. Frustration fonctionnalités limitées

**Processus d'Upgrade**
1. Notification limite atteinte
2. Affichage comparatif des plans
3. Sélection plan souhaité
4. Processus paiement sécurisé (Stripe)
5. Activation immédiate fonctionnalités
6. Email de confirmation et facturation

### 1.4.3 Parcours d'Utilisation Quotidienne

**Matin (Utilisateur Régulier)**
1. Ouverture application
2. Notification message du compagnon
3. Conversation courte (5-10 minutes)
4. Planification de la journée assistée

**Pause Déjeuner**
1. Session courte détente
2. Génération d'image amusante
3. Partage sur réseau social du compagnon

**Soirée (Session Longue)**
1. Conversation approfondie (30-60 minutes)
2. Activités créatives (jeux, storytelling)
3. Génération de contenus visuels
4. Consultation capsule temporelle

## 1.5 Exigences Non-Fonctionnelles

### 1.5.1 Performance
- Temps de réponse textuelle < 1 seconde (99%)
- Génération d'images < 15 secondes (standard)
- Appels vocaux avec latence < 300ms
- Support de 100 000+ utilisateurs simultanés
- Temps de chargement initial < 3 secondes
- Optimisation consommation batterie mobile

### 1.5.2 Disponibilité
- Disponibilité du service 99.95%
- Maintenance planifiée hors heures de pointe
- Basculement automatique en cas de défaillance
- RTO (Recovery Time Objective) < 2 heures
- RPO (Recovery Point Objective) < 15 minutes
- Mode hors-ligne limité pour fonctionnalités essentielles

### 1.5.3 Sécurité
- Chiffrement HTTPS/TLS 1.3
- Chiffrement au repos (AES-256)
- Authentification multi-facteurs pour premium
- Audit de sécurité trimestriel
- Tests d'intrusion réguliers
- Conformité ISO 27001 et NIST
- Historique d'accès consultable

### 1.5.4 Accessibilité
- Compatibilité lecteurs d'écran
- Support mode sombre/contraste élevé
- Navigation clavier complète
- Conformité WCAG 2.1 niveau AA
- Support multilingue (FR, EN, ES, DE, ZH, JA)
- Alternatives textuelles pour contenus non-textuels

### 1.5.5 Scalabilité
- Architecture microservices
- Auto-scaling basé sur la charge
- Distribution géographique (CDN)
- Base de données distribuée
- Cache multi-niveaux
- Load balancing intelligent

---

# 2. SPÉCIFICATIONS TECHNIQUES COMPLÈTES

## 2.1 Architecture Globale

### 2.1.1 Vue d'Ensemble de l'Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    COUCHE PRÉSENTATION                          │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐           │
│  │  Web App     │ │  Mobile App  │ │  VR Client   │           │
│  │  (React)     │ │  (React      │ │  (Unity)     │           │
│  │              │ │   Native)    │ │              │           │
│  └──────────────┘ └──────────────┘ └──────────────┘           │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API GATEWAY & LOAD BALANCER                  │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐           │
│  │  NGINX       │ │  Spring Cloud│ │  Rate        │           │
│  │  Reverse     │ │  Gateway     │ │  Limiting    │           │
│  │  Proxy       │ │              │ │              │           │
│  └──────────────┘ └──────────────┘ └──────────────┘           │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     SERVICE MESH LAYER                          │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐           │
│  │Service       │ │Load          │ │Circuit       │           │
│  │Discovery     │ │Balancing     │ │Breakers      │           │
│  └──────────────┘ └──────────────┘ └──────────────┘           │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MICROSERVICES CORE                           │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐           │
│  │User Service  │ │Companion     │ │Conversation  │           │
│  │              │ │Service       │ │Service       │           │
│  └──────────────┘ └──────────────┘ └──────────────┘           │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐           │
│  │Image Gen     │ │Audio         │ │Payment       │           │
│  │Service       │ │Service       │ │Service       │           │
│  └──────────────┘ └──────────────┘ └──────────────┘           │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    COUCHE DONNÉES                               │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐           │
│  │PostgreSQL    │ │MongoDB       │ │Redis         │           │
│  │(Relations)   │ │(Documents)   │ │(Cache)       │           │
│  └──────────────┘ └──────────────┘ └──────────────┘           │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐           │
│  │Elasticsearch │ │S3/MinIO      │ │Kafka         │           │
│  │(Recherche)   │ │(Fichiers)    │ │(Events)      │           │
│  └──────────────┘ └──────────────┘ └──────────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

### 2.1.2 Stack Technologique Détaillée

#### Backend
- **Langage** : Java 21 (OpenJDK) avec Virtual Threads
- **Framework** : Spring Boot 3.2+ avec Spring Cloud
- **Build Tool** : Maven 3.9+
- **Architecture** : Microservices multi-modules
- **API** : REST (Spring Web) + gRPC pour inter-services
- **Documentation API** : OpenAPI 3.0 / Swagger

#### Frontend Web
- **Framework** : React 18+ avec TypeScript 5+
- **State Management** : Redux Toolkit
- **UI Library** : Material-UI ou Tailwind CSS
- **HTTP Client** : Axios avec React Query
- **Real-time** : Socket.io-client
- **Build** : Vite ou Webpack 5

#### Frontend Mobile
- **Framework** : React Native 0.73+
- **Navigation** : React Navigation
- **State Management** : Redux Toolkit
- **UI Components** : React Native Paper
- **Platform** : Expo SDK 50+ pour déploiement

#### Bases de Données
- **PostgreSQL 16+** : Données relationnelles (utilisateurs, abonnements)
- **MongoDB 7+** : Conversations, profils compagnons
- **Redis 7+** : Cache, sessions, rate limiting, pub/sub
- **Elasticsearch 8.11+** : Recherche full-text, analytics

#### Infrastructure
- **Conteneurisation** : Docker 24+
- **Orchestration** : Kubernetes 1.29+
- **CI/CD** : Jenkins / GitLab CI
- **Monitoring** : Prometheus + Grafana
- **Logging** : ELK Stack (Elasticsearch, Logstash, Kibana)
- **Tracing** : Jaeger

#### IA / ML
- **LLM** : Intégration API (OpenAI, Anthropic) + modèles locaux
- **Frameworks Java ML** : DJL (Deep Java Library), ONNX Runtime
- **Génération Images** : Stable Diffusion via API
- **Audio** : Whisper (STT), Coqui TTS (TTS)

## 2.2 Architecture Microservices Détaillée

### 2.2.1 User Service

#### Responsabilités
- Gestion des comptes utilisateurs
- Authentification et autorisation
- Gestion des profils
- Gestion des abonnements et jetons

#### Endpoints Principaux
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/logout
POST   /api/v1/auth/refresh
GET    /api/v1/users/{id}
PUT    /api/v1/users/{id}
DELETE /api/v1/users/{id}
GET    /api/v1/users/{id}/subscriptions
POST   /api/v1/users/{id}/subscriptions
GET    /api/v1/users/{id}/tokens
POST   /api/v1/users/{id}/tokens/purchase
```

#### Technologies
- Spring Boot 3.2
- Spring Security 6.x (JWT + OAuth2)
- Spring Data JPA
- PostgreSQL

#### Modèle de Données
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Column(nullable = false)
    private String username;
    
    private LocalDate birthDate;
    
    @Enumerated(EnumType.STRING)
    private UserRole role;
    
    private boolean emailVerified;
    private boolean active;
    
    @OneToOne(cascade = CascadeType.ALL)
    private Subscription subscription;
    
    @OneToOne(cascade = CascadeType.ALL)
    private TokenWallet tokenWallet;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Entity
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan plan;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean autoRenewal;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal monthlyPrice;
}

@Entity
public class TokenWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private Integer balance;
    private Integer totalEarned;
    private Integer totalSpent;
    
    @OneToMany(cascade = CascadeType.ALL)
    private List<TokenTransaction> transactions;
}
```

### 2.2.2 Companion Service

#### Responsabilités
- Création et gestion des compagnons
- Personnalisation et configuration
- Gestion de l'évolution des compagnons
- Stockage des modèles prédéfinis

#### Endpoints Principaux
```
POST   /api/v1/companions
GET    /api/v1/companions/{id}
PUT    /api/v1/companions/{id}
DELETE /api/v1/companions/{id}
GET    /api/v1/companions/user/{userId}
GET    /api/v1/companions/templates
POST   /api/v1/companions/{id}/evolve
POST   /api/v1/companions/{id}/freeze-traits
```

#### Technologies
- Spring Boot 3.2
- Spring Data MongoDB
- Redis (cache)

#### Modèle de Données
```java
@Document(collection = "companions")
public class Companion {
    @Id
    private String id;
    
    private String userId;
    private String name;
    
    @Embedded
    private Appearance appearance;
    
    @Embedded
    private Personality personality;
    
    @Embedded
    private Voice voice;
    
    private String backstory;
    private LocalDateTime createdAt;
    private LocalDateTime lastEvolutionDate;
    
    @Embedded
    private GeneticProfile geneticProfile;
    
    private boolean isPublic;
    private int likeCount;
}

public class Appearance {
    private String gender;
    private String hairColor;
    private String eyeColor;
    private String skinTone;
    private String bodyType;
    private Integer age;
    private Map<String, String> customFeatures;
    private String avatarImageUrl;
}

public class Personality {
    private Map<String, Integer> traits; // Key: trait name, Value: 0-100
    private List<String> interests;
    private List<String> dislikes;
    private String humorStyle;
    private String communicationStyle;
}

public class GeneticProfile {
    private Map<String, Gene> genes;
    private List<String> dominantTraits;
    private List<String> recessiveTraits;
    private boolean frozen;
}
```

### 2.2.3 Conversation Service

#### Responsabilités
- Gestion des conversations
- Orchestration des appels IA
- Stockage de l'historique
- Gestion du contexte conversationnel

#### Endpoints Principaux
```
POST   /api/v1/conversations
GET    /api/v1/conversations/{id}
DELETE /api/v1/conversations/{id}
GET    /api/v1/conversations/user/{userId}
POST   /api/v1/conversations/{id}/messages
GET    /api/v1/conversations/{id}/messages
DELETE /api/v1/conversations/{id}/messages/{messageId}
POST   /api/v1/conversations/{id}/export
```

#### Technologies
- Spring Boot 3.2
- Spring WebFlux (reactive)
- MongoDB (historique)
- Redis (contexte actif)
- Kafka (événements)

#### Modèle de Données
```java
@Document(collection = "conversations")
public class Conversation {
    @Id
    private String id;
    
    private String userId;
    private String companionId;
    private String title;
    
    @Field("messages")
    private List<Message> messages;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    
    private boolean isEphemeral;
    private LocalDateTime expiresAt;
    
    @Indexed
    private List<String> tags;
}

public class Message {
    private String id;
    
    @Enumerated(EnumType.STRING)
    private MessageSender sender; // USER, COMPANION
    
    private String content;
    
    @Enumerated(EnumType.STRING)
    private MessageType type; // TEXT, IMAGE, AUDIO, VIDEO
    
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
    
    private List<Reaction> reactions;
}

public class ConversationContext {
    private String conversationId;
    private List<String> recentTopics;
    private Map<String, Object> userPreferences;
    private EmotionalState emotionalState;
    private int messageCount;
}
```

### 2.2.4 Image Generation Service

#### Responsabilités
- Génération d'images via IA
- Gestion de la file d'attente
- Stockage et optimisation des images
- Galerie utilisateur

#### Endpoints Principaux
```
POST   /api/v1/images/generate
GET    /api/v1/images/{id}
DELETE /api/v1/images/{id}
GET    /api/v1/images/user/{userId}
POST   /api/v1/images/{id}/favorite
GET    /api/v1/images/{id}/download
```

#### Technologies
- Spring Boot 3.2
- Kafka (file d'attente)
- S3/MinIO (stockage)
- PostgreSQL (métadonnées)
- Redis (cache)

#### Modèle de Données
```java
@Entity
public class GeneratedImage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private UUID userId;
    private String prompt;
    
    @Enumerated(EnumType.STRING)
    private ImageStyle style;
    
    private String resolution;
    
    @Enumerated(EnumType.STRING)
    private GenerationStatus status;
    
    private String storageUrl;
    private String thumbnailUrl;
    
    private Integer seed;
    private Map<String, Object> parameters;
    
    private boolean isFavorite;
    private boolean isPublic;
    
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}

public enum GenerationStatus {
    QUEUED, PROCESSING, COMPLETED, FAILED
}

public enum ImageStyle {
    REALISTIC, ANIME, ARTISTIC, CARTOON, CINEMATIC
}
```

### 2.2.5 Audio Service

#### Responsabilités
- Traitement audio (STT/TTS)
- Gestion des appels vocaux
- Synthèse vocale personnalisée
- Enregistrement et stockage

#### Endpoints Principaux
```
POST   /api/v1/audio/transcribe
POST   /api/v1/audio/synthesize
POST   /api/v1/audio/calls/initiate
GET    /api/v1/audio/calls/{id}
DELETE /api/v1/audio/calls/{id}
```

#### Technologies
- Spring Boot 3.2
- WebRTC (appels temps réel)
- Whisper API (STT)
- Coqui TTS ou ElevenLabs (TTS)
- S3/MinIO (stockage audio)

### 2.2.6 Payment Service

#### Responsabilités
- Traitement des paiements
- Gestion des abonnements
- Gestion des jetons
- Facturation et reçus

#### Endpoints Principaux
```
POST   /api/v1/payments/subscribe
POST   /api/v1/payments/cancel-subscription
POST   /api/v1/payments/purchase-tokens
GET    /api/v1/payments/invoices
GET    /api/v1/payments/history
POST   /api/v1/webhooks/stripe
```

#### Technologies
- Spring Boot 3.2
- Stripe SDK
- PostgreSQL (transactions)
- Kafka (événements paiement)

## 2.3 Modèle de Données Complet

### 2.3.1 Schéma PostgreSQL

```sql
-- USERS AND AUTHENTICATION
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    birth_date DATE,
    role VARCHAR(20) DEFAULT 'USER',
    email_verified BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    plan VARCHAR(20) NOT NULL, -- FREE, STANDARD, PREMIUM, VIP
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    auto_renewal BOOLEAN DEFAULT TRUE,
    monthly_price DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE token_wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE UNIQUE,
    balance INTEGER DEFAULT 0,
    total_earned INTEGER DEFAULT 0,
    total_spent INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE token_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID REFERENCES token_wallets(id),
    type VARCHAR(20) NOT NULL, -- PURCHASE, EARN, SPEND
    amount INTEGER NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- IMAGE GENERATION
CREATE TABLE generated_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    prompt TEXT NOT NULL,
    style VARCHAR(50),
    resolution VARCHAR(20),
    status VARCHAR(20) DEFAULT 'QUEUED',
    storage_url TEXT,
    thumbnail_url TEXT,
    seed INTEGER,
    parameters JSONB,
    is_favorite BOOLEAN DEFAULT FALSE,
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- PAYMENTS
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    status VARCHAR(20) DEFAULT 'PENDING',
    stripe_payment_intent_id VARCHAR(255),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- MODERATION
CREATE TABLE moderation_incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    conversation_id VARCHAR(255),
    message_id VARCHAR(255),
    incident_type VARCHAR(50),
    severity VARCHAR(20),
    status VARCHAR(20) DEFAULT 'PENDING',
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX idx_images_user ON generated_images(user_id);
CREATE INDEX idx_images_status ON generated_images(status);
CREATE INDEX idx_transactions_user ON payment_transactions(user_id);
```

### 2.3.2 Schéma MongoDB

```javascript
// Companions Collection
db.createCollection("companions", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["userId", "name", "appearance", "personality"],
            properties: {
                userId: { bsonType: "string" },
                name: { bsonType: "string" },
                appearance: {
                    bsonType: "object",
                    properties: {
                        gender: { bsonType: "string" },
                        hairColor: { bsonType: "string" },
                        eyeColor: { bsonType: "string" },
                        avatarImageUrl: { bsonType: "string" }
                    }
                },
                personality: {
                    bsonType: "object",
                    properties: {
                        traits: { bsonType: "object" },
                        interests: { bsonType: "array" }
                    }
                },
                createdAt: { bsonType: "date" }
            }
        }
    }
});

db.companions.createIndex({ "userId": 1 });
db.companions.createIndex({ "name": 1 });
db.companions.createIndex({ "isPublic": 1, "likeCount": -1 });

// Conversations Collection
db.createCollection("conversations", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["userId", "companionId", "messages"],
            properties: {
                userId: { bsonType: "string" },
                companionId: { bsonType: "string" },
                messages: {
                    bsonType: "array",
                    items: {
                        bsonType: "object",
                        required: ["id", "sender", "content", "timestamp"],
                        properties: {
                            id: { bsonType: "string" },
                            sender: { enum: ["USER", "COMPANION"] },
                            content: { bsonType: "string" },
                            type: { enum: ["TEXT", "IMAGE", "AUDIO"] },
                            timestamp: { bsonType: "date" }
                        }
                    }
                }
            }
        }
    }
});

db.conversations.createIndex({ "userId": 1, "lastMessageAt": -1 });
db.conversations.createIndex({ "tags": 1 });
db.conversations.createIndex({ "isEphemeral": 1, "expiresAt": 1 });
```

## 2.4 Diagrammes Techniques

### 2.4.1 Diagramme de Séquence - Envoi de Message

```
User         WebApp       Gateway      ConvService    AIService     CompService
 │             │             │              │             │              │
 │─Send Msg───>│             │              │             │              │
 │             │─POST────────>│              │             │              │
 │             │  /messages  │              │             │              │
 │             │             │─Validate────>│             │              │
 │             │             │   Token      │             │              │
 │             │             │<─────────────│             │              │
 │             │             │              │             │              │
 │             │             │─Store Msg───>│             │              │
 │             │             │              │─Get Context─>│              │
 │             │             │              │<────────────│              │
 │             │             │              │             │              │
 │             │             │              │─Get Comp───────────────────>│
 │             │             │              │   Profile   │              │
 │             │             │              │<─────────────────────────────│
 │             │             │              │             │              │
 │             │             │              │─Generate───>│              │
 │             │             │              │  Response   │              │
 │             │             │              │<────────────│              │
 │             │             │              │             │              │
 │             │             │<─Response────│             │              │
 │             │<────────────│              │             │              │
 │<────────────│             │              │             │              │
 │   Display   │             │              │             │              │
```

### 2.4.2 Diagramme de Déploiement Kubernetes

```
┌─────────────────────────────────────────────────────────┐
│                    KUBERNETES CLUSTER                   │
│                                                         │
│  ┌────────────────────────────────────────────────┐   │
│  │              INGRESS CONTROLLER                │   │
│  │  (NGINX with SSL Termination & Load Balancing)│   │
│  └──────────────────────┬─────────────────────────┘   │
│                         │                              │
│  ┌──────────────────────┴─────────────────────────┐   │
│  │              API GATEWAY PODs                  │   │
│  │  (Spring Cloud Gateway - 3 replicas)          │   │
│  └──────────────────────┬─────────────────────────┘   │
│                         │                              │
│  ┌──────────────────────┴─────────────────────────┐   │
│  │           MICROSERVICES DEPLOYMENTS            │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐    │   │
│  │  │User Svc  │  │Comp Svc  │  │Conv Svc  │    │   │
│  │  │(3 pods)  │  │(2 pods)  │  │(4 pods)  │    │   │
│  │  └──────────┘  └──────────┘  └──────────┘    │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐    │   │
│  │  │Image Svc │  │Audio Svc │  │Pay Svc   │    │   │
│  │  │(2 pods)  │  │(2 pods)  │  │(2 pods)  │    │   │
│  │  └──────────┘  └──────────┘  └──────────┘    │   │
│  └────────────────────────────────────────────────┘   │
│                                                         │
│  ┌────────────────────────────────────────────────┐   │
│  │           STATEFUL SERVICES                    │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐    │   │
│  │  │PostgreSQL│  │MongoDB   │  │Redis     │    │   │
│  │  │StatefulSet│ │StatefulSet│ │StatefulSet   │   │
│  │  └──────────┘  └──────────┘  └──────────┘    │   │
│  │  ┌──────────┐  ┌──────────┐                  │   │
│  │  │Kafka     │  │ElasticSch│                  │   │
│  │  │StatefulSet│ │StatefulSet│                  │   │
│  │  └──────────┘  └──────────┘                  │   │
│  └────────────────────────────────────────────────┘   │
│                                                         │
│  ┌────────────────────────────────────────────────┐   │
│  │          MONITORING & OBSERVABILITY            │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐    │   │
│  │  │Prometheus│  │Grafana   │  │Jaeger    │    │   │
│  │  └──────────┘  └──────────┘  └──────────┘    │   │
│  └────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

---

# 3. DOSSIER D'ARCHITECTURE

## 3.1 Architecture Java Multi-Module

### 3.1.1 Structure Projet Maven

```
nexus-ai-parent/
├── pom.xml                          # Parent POM
├── README.md
├── .gitignore
│
├── nexus-core/                      # Module Core
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexusai/core/
│       │   ├── domain/              # Entités domaine
│       │   ├── dto/                 # Data Transfer Objects
│       │   ├── exception/           # Exceptions métier
│       │   ├── util/                # Utilitaires
│       │   └── constant/            # Constantes
│       └── test/
│
├── nexus-auth/                      # Module Authentification
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexusai/auth/
│       │   ├── config/              # Config Spring Security
│       │   ├── controller/          # REST Controllers
│       │   ├── service/             # Services métier
│       │   ├── repository/          # JPA Repositories
│       │   ├── security/            # JWT, OAuth2
│       │   └── dto/                 # DTOs spécifiques
│       └── test/
│
├── nexus-companion/                 # Module Compagnons
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexusai/companion/
│       │   ├── controller/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── model/               # Modèles MongoDB
│       │   └── dto/
│       └── test/
│
├── nexus-conversation/              # Module Conversations
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexusai/conversation/
│       │   ├── controller/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── websocket/           # WebSocket handlers
│       │   └── dto/
│       └── test/
│
├── nexus-ai-engine/                 # Module IA
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexusai/ai/
│       │   ├── service/
│       │   ├── client/              # Clients API IA
│       │   ├── model/
│       │   └── prompt/              # Gestion prompts
│       └── test/
│
├── nexus-media/                     # Module Média
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexusai/media/
│       │   ├── controller/
│       │   ├── service/
│       │   │   ├── image/           # Génération images
│       │   │   └── audio/           # Traitement audio
│       │   └── dto/
│       └── test/
│
├── nexus-payment/                   # Module Paiement
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexusai/payment/
│       │   ├── controller/
│       │   ├── service/
│       │   ├── webhook/             # Webhooks Stripe
│       │   └── dto/
│       └── test/
│
├── nexus-api/                       # API Gateway
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexusai/api/
│       │   ├── config/
│       │   ├── filter/              # Filtres gateway
│       │   ├── route/               # Configuration routes
│       │   └── security/
│       └── test/
│
├── nexus-web/                       # Frontend Web (React)
│   ├── package.json
│   ├── public/
│   └── src/
│       ├── components/
│       ├── pages/
│       ├── services/
│       ├── store/                   # Redux
│       └── utils/
│
├── nexus-commons/                   # Utilitaires partagés
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexusai/commons/
│       │   ├── util/
│       │   ├── validator/
│       │   └── mapper/
│       └── test/
│
└── nexus-deployment/                # Scripts déploiement
    ├── docker/
    │   ├── Dockerfile.user-service
    │   ├── Dockerfile.companion-service
    │   └── ...
    ├── kubernetes/
    │   ├── deployments/
    │   ├── services/
    │   ├── configmaps/
    │   └── secrets/
    └── scripts/
```

### 3.1.2 POM Parent

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.nexusai</groupId>
    <artifactId>nexus-ai-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <name>NexusAI Parent</name>
    <description>Parent POM for NexusAI Companion Platform</description>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    
    <modules>
        <module>nexus-core</module>
        <module>nexus-auth</module>
        <module>nexus-companion</module>
        <module>nexus-conversation</module>
        <module>nexus-ai-engine</module>
        <module>nexus-media</module>
        <module>nexus-payment</module>
        <module>nexus-api</module>
        <module>nexus-commons</module>
    </modules>
    
    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Versions des dépendances -->
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        <lombok.version>1.18.30</lombok.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <jwt.version>0.12.3</jwt.version>
        <springdoc.version>2.3.0</springdoc.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- JWT -->
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jwt.version}</version>
            </dependency>
            
            <!-- Lombok -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
                <scope>provided</scope>
            </dependency>
            
            <!-- MapStruct -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
            
            <!-- SpringDoc OpenAPI -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <dependencies>
        <!-- Dépendances communes à tous les modules -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                </plugin>
                
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.11.0</version>
                    <configuration>
                        <source>21</source>
                        <target>21</target>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                            <path>
                                <groupId>org.mapstruct</groupId>
                                <artifactId>mapstruct-processor</artifactId>
                                <version>${mapstruct.version}</version>
                            </path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### 3.1.3 Exemple Module nexus-auth

#### POM nexus-auth
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.nexusai</groupId>
        <artifactId>nexus-ai-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>nexus-auth</artifactId>
    <name>NexusAI Authentication Service</name>
    
    <dependencies>
        <!-- Module Core -->
        <dependency>
            <groupId>com.nexusai</groupId>
            <artifactId>nexus-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- PostgreSQL Driver -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        
        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        
        <!-- OpenAPI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

#### Classes Principales nexus-auth

**UserService.java**
```java
package com.nexusai.auth.service;

import com.nexusai.auth.dto.RegistrationRequest;
import com.nexusai.auth.dto.UserResponse;
import com.nexusai.core.domain.User;
import com.nexusai.core.exception.ResourceNotFoundException;
import java.util.Optional;

/**
 * Service pour la gestion des utilisateurs.
 * 
 * Ce service fournit les opérations CRUD de base pour les utilisateurs
 * ainsi que la gestion des jetons et des abonnements.
 * 
 * @author NexusAI Team
 * @version 1.0
 * @since 1.0
 */
public interface UserService {
    
    /**
     * Recherche un utilisateur par son nom d'utilisateur.
     * 
     * @param username le nom d'utilisateur à rechercher
     * @return un Optional contenant l'utilisateur s'il existe
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Recherche un utilisateur par son email.
     * 
     * @param email l'email à rechercher
     * @return un Optional contenant l'utilisateur s'il existe
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Crée un nouvel utilisateur.
     * 
     * @param request les données d'inscription
     * @return l'utilisateur créé
     * @throws IllegalArgumentException si l'email existe déjà
     */
    UserResponse register(RegistrationRequest request);
    
    /**
     * Met à jour les informations d'un utilisateur.
     * 
     * @param userId l'identifiant de l'utilisateur
     * @param updateData les nouvelles données
     * @return l'utilisateur mis à jour
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    UserResponse update(String userId, UserUpdateRequest updateData);
    
    /**
     * Consomme des jetons pour un utilisateur.
     * 
     * @param userId l'identifiant de l'utilisateur
     * @param amount le nombre de jetons à consommer
     * @return true si l'opération a réussi
     * @throws IllegalArgumentException si le solde est insuffisant
     */
    boolean consumeTokens(String userId, int amount);
    
    /**
     * Ajoute des jetons à un utilisateur.
     * 
     * @param userId l'identifiant de l'utilisateur
     * @param amount le nombre de jetons à ajouter
     */
    void addTokens(String userId, int amount);
}
```

**UserServiceImpl.java**
```java
package com.nexusai.auth.service.impl;

import com.nexusai.auth.dto.RegistrationRequest;
import com.nexusai.auth.dto.UserResponse;
import com.nexusai.auth.mapper.UserMapper;
import com.nexusai.auth.repository.UserRepository;
import com.nexusai.auth.service.UserService;
import com.nexusai.core.domain.User;
import com.nexusai.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implémentation du service de gestion des utilisateurs.
 * 
 * @author NexusAI Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    
    @Override
    public Optional<User> findByUsername(String username) {
        log.debug("Recherche utilisateur par username: {}", username);
        return userRepository.findByUsername(username);
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        log.debug("Recherche utilisateur par email: {}", email);
        return userRepository.findByEmail(email);
    }
    
    @Override
    @Transactional
    public UserResponse register(RegistrationRequest request) {
        log.info("Inscription d'un nouvel utilisateur: {}", request.getEmail());
        
        // Vérification unicité email
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Tentative d'inscription avec email existant: {}", request.getEmail());
            throw new IllegalArgumentException("Email déjà utilisé");
        }
        
        // Création utilisateur
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .emailVerified(false)
                .active(true)
                .build();
        
        // Initialisation wallet
        TokenWallet wallet = TokenWallet.builder()
                .balance(100) // Jetons de bienvenue
                .totalEarned(100)
                .totalSpent(0)
                .build();
        user.setTokenWallet(wallet);
        
        // Initialisation abonnement gratuit
        Subscription subscription = Subscription.builder()
                .plan(SubscriptionPlan.FREE)
                .startDate(LocalDateTime.now())
                .autoRenewal(false)
                .build();
        user.setSubscription(subscription);
        
        User savedUser = userRepository.save(user);
        log.info("Utilisateur créé avec succès: ID={}", savedUser.getId());
        
        return userMapper.toUserResponse(savedUser);
    }
    
    @Override
    @Transactional
    public boolean consumeTokens(String userId, int amount) {
        log.debug("Consommation de {} jetons pour user {}", amount, userId);
        
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        TokenWallet wallet = user.getTokenWallet();
        
        if (wallet.getBalance() < amount) {
            log.warn("Solde insuffisant pour user {}: {} < {}", 
                    userId, wallet.getBalance(), amount);
            throw new IllegalArgumentException("Solde de jetons insuffisant");
        }
        
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setTotalSpent(wallet.getTotalSpent() + amount);
        
        userRepository.save(user);
        log.info("Jetons consommés avec succès. Nouveau solde: {}", wallet.getBalance());
        
        return true;
    }
    
    @Override
    @Transactional
    public void addTokens(String userId, int amount) {
        log.debug("Ajout de {} jetons pour user {}", amount, userId);
        
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        TokenWallet wallet = user.getTokenWallet();
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setTotalEarned(wallet.getTotalEarned() + amount);
        
        userRepository.save(user);
        log.info("Jetons ajoutés avec succès. Nouveau solde: {}", wallet.getBalance());
    }
}
```

**UserController.java**
```java
package com.nexusai.auth.controller;

import com.nexusai.auth.dto.RegistrationRequest;
import com.nexusai.auth.dto.UserResponse;
import com.nexusai.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la gestion des utilisateurs.
 * 
 * @author NexusAI Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestion des utilisateurs")
public class UserController {
    
    private final UserService userService;
    
    /**
     * Inscription d'un nouvel utilisateur.
     */
    @PostMapping("/register")
    @Operation(summary = "Inscription", 
               description = "Crée un nouveau compte utilisateur")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegistrationRequest request) {
        
        UserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Récupération du profil utilisateur connecté.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mon profil", 
               description = "Récupère les informations de l'utilisateur connecté")
    public ResponseEntity<UserResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        
        return ResponseEntity.ok(userMapper.toUserResponse(user));
    }
    
    /**
     * Ajout de jetons (admin uniquement).
     */
    @PostMapping("/{userId}/tokens")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Ajouter des jetons", 
               description = "Ajoute des jetons à un utilisateur (admin)")
    public ResponseEntity<Void> addTokens(
            @PathVariable String userId,
            @RequestParam int amount) {
        
        userService.addTokens(userId, amount);
        return ResponseEntity.ok().build();
    }
}
```

## 3.2 Patterns et Bonnes Pratiques

### 3.2.1 Clean Architecture

**Séparation des Couches**
```
┌────────────────────────────────────┐
│     Presentation Layer (API)       │  Controllers, DTOs, Mappers
├────────────────────────────────────┤
│     Application Layer (Service)    │  Use Cases, Business Logic
├────────────────────────────────────┤
│     Domain Layer (Core)            │  Entities, Value Objects
├────────────────────────────────────┤
│     Infrastructure Layer           │  Repositories, External APIs
└────────────────────────────────────┘
```

### 3.2.2 Design Patterns Utilisés

1. **Repository Pattern**
   - Abstraction de l'accès aux données
   - Séparation logique métier / persistance

2. **Service Layer Pattern**
   - Encapsulation de la logique métier
   - Transactions gérées au niveau service

3. **DTO Pattern**
   - Transfert de données entre couches
   - Validation des entrées utilisateur

4. **Factory Pattern**
   - Création d'objets complexes (Compagnons)
   - Initialisation de configurations

5. **Strategy Pattern**
   - Différentes stratégies de génération IA
   - Algorithmes de tarification

6. **Observer Pattern**
   - Événements système (Kafka)
   - Notifications utilisateurs

### 3.2.3 Gestion des Erreurs

**GlobalExceptionHandler.java**
```java
package com.nexusai.core.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Gestionnaire global des exceptions.
 * 
 * Capture et formate toutes les exceptions de l'application
 * pour renvoyer des réponses cohérentes aux clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex) {
        
        log.warn("Ressource non trouvée: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .code("RESOURCE_NOT_FOUND")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex) {
        
        log.warn("Argument invalide: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .code("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex) {
        
        log.error("Erreur interne: ", ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("Une erreur interne s'est produite")
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

# 4. USER STORIES COMPLÈTES

## 4.1 Organisation des Epics

### Epic 1: Gestion des Utilisateurs
### Epic 2: Création et Gestion des Compagnons
### Epic 3: Conversations et Interactions
### Epic 4: Génération de Médias
### Epic 5: Système de Paiement
### Epic 6: Sécurité et Modération

---

## EPIC 1: GESTION DES UTILISATEURS

### US-001: Inscription Utilisateur
**En tant que** visiteur  
**Je veux** créer un compte  
**Afin de** pouvoir utiliser l'application

**Critères d'Acceptation:**
- [ ] Formulaire avec email, username, mot de passe
- [ ] Validation email unique
- [ ] Mot de passe fort (8+ caractères, maj, chiffres, spéciaux)
- [ ] Email de confirmation envoyé
- [ ] Compte créé avec plan gratuit
- [ ] 100 jetons de bienvenue offerts

**Tâches Techniques:**
- [ ] Créer endpoint POST /api/v1/auth/register
- [ ] Implémenter validation DTO
- [ ] Créer service d'envoi d'email
- [ ] Créer tests unitaires et d'intégration
- [ ] Documenter API avec Swagger

**Story Points:** 5  
**Priorité:** Critique  
**Sprint:** 1

---

### US-002: Connexion Utilisateur
**En tant qu'** utilisateur enregistré  
**Je veux** me connecter  
**Afin d'** accéder à mon compte

**Critères d'Acceptation:**
- [ ] Connexion par email/mot de passe
- [ ] Génération token JWT (24h)
- [ ] Refresh token (7 jours)
- [ ] Blocage après 5 tentatives échouées
- [ ] Option "Se souvenir de moi"

**Tâches Techniques:**
- [ ] Créer endpoint POST /api/v1/auth/login
- [ ] Implémenter JwtTokenProvider
- [ ] Config Spring Security
- [ ] Gestion refresh tokens dans Redis
- [ ] Tests sécurité

**Story Points:** 5  
**Priorité:** Critique  
**Sprint:** 1

---

### US-003: Gestion du Profil
**En tant qu'** utilisateur connecté  
**Je veux** modifier mon profil  
**Afin de** personnaliser mes informations

**Critères d'Acceptation:**
- [ ] Modification username, avatar, préférences
- [ ] Changement mot de passe
- [ ] Gestion notifications
- [ ] Préférences de confidentialité
- [ ] Historique des modifications

**Tâches Techniques:**
- [ ] Endpoints GET/PUT /api/v1/users/me
- [ ] Upload avatar (S3/MinIO)
- [ ] Validation changements
- [ ] Audit trail modifications
- [ ] Tests

**Story Points:** 3  
**Priorité:** Haute  
**Sprint:** 2

---

### US-004: Gestion des Abonnements
**En tant qu'** utilisateur  
**Je veux** souscrire à un abonnement premium  
**Afin d'** accéder à plus de fonctionnalités

**Critères d'Acceptation:**
- [ ] Affichage comparatif des plans
- [ ] Souscription plan (Standard/Premium/VIP)
- [ ] Paiement sécurisé (Stripe)
- [ ] Activation immédiate
- [ ] Email de confirmation
- [ ] Facturation mensuelle automatique

**Tâches Techniques:**
- [ ] Intégration Stripe API
- [ ] Endpoints abonnements
- [ ] Webhooks Stripe
- [ ] Service facturation
- [ ] Tests paiement

**Story Points:** 8  
**Priorité:** Haute  
**Sprint:** 3

---

### US-005: Système de Jetons
**En tant qu'** utilisateur  
**Je veux** acheter des jetons  
**Afin de** consommer des fonctionnalités payantes

**Critères d'Acceptation:**
- [ ] Affichage solde de jetons
- [ ] Packs de jetons (100, 500, 1000)
- [ ] Paiement par Stripe
- [ ] Ajout immédiat au wallet
- [ ] Historique des transactions
- [ ] Jetons quotidiens gratuits

**Tâches Techniques:**
- [ ] Service TokenWallet
- [ ] Endpoints jetons
- [ ] Intégration paiement
- [ ] Job quotidien jetons gratuits
- [ ] Tests

**Story Points:** 5  
**Priorité:** Moyenne  
**Sprint:** 3

---

## EPIC 2: CRÉATION ET GESTION DES COMPAGNONS

### US-006: Création de Compagnon (Modèle Prédéfini)
**En tant qu'** utilisateur  
**Je veux** créer un compagnon à partir d'un modèle  
**Afin de** commencer rapidement

**Critères d'Acceptation:**
- [ ] Galerie de 1000+ modèles prédéfinis
- [ ] Filtres par genre, style, personnalité
- [ ] Prévisualisation du modèle
- [ ] Personnalisation du nom
- [ ] Limite selon abonnement (1/3/10/illimité)
- [ ] Création instantanée

**Tâches Techniques:**
- [ ] Collection MongoDB "companion_templates"
- [ ] Endpoint GET /api/v1/companions/templates
- [ ] Service CompanionFactory
- [ ] Endpoint POST /api/v1/companions
- [ ] Tests

**Story Points:** 5  
**Priorité:** Critique  
**Sprint:** 2

---

### US-007: Personnalisation Avancée du Compagnon
**En tant qu'** utilisateur premium  
**Je veux** personnaliser entièrement mon compagnon  
**Afin de** créer un avatar unique

**Critères d'Acceptation:**
- [ ] Customisation apparence (cheveux, yeux, peau, corps)
- [ ] Configuration traits de personnalité (sliders)
- [ ] Sélection centres d'intérêt
- [ ] Définition backstory personnalisée
- [ ] Choix style de communication
- [ ] Prévisualisation en temps réel

**Tâches Techniques:**
- [ ] UI formulaire multi-étapes
- [ ] Service PersonalityEngine
- [ ] Validation cohérence traits
- [ ] Génération avatar IA (optionnel)
- [ ] Tests

**Story Points:** 8  
**Priorité:** Haute  
**Sprint:** 4

---

### US-008: Évolution Génétique du Compagnon
**En tant qu'** utilisateur VIP  
**Je veux** que mon compagnon évolue naturellement  
**Afin d'** avoir des interactions plus dynamiques

**Critères d'Acceptation:**
- [ ] Évolution progressive des traits (1%/semaine)
- [ ] Basée sur les interactions utilisateur
- [ ] Notification des changements
- [ ] Visualisation arbre génétique
- [ ] Option "gel" de traits (coût jetons)
- [ ] Historique des évolutions

**Tâches Techniques:**
- [ ] Algorithme évolution génétique
- [ ] Job hebdomadaire calcul évolution
- [ ] Service GeneticProfileManager
- [ ] UI visualisation évolution
- [ ] Tests algorithmes

**Story Points:** 13  
**Priorité:** Basse  
**Sprint:** 10

---

### US-009: Gestion Multiple Compagnons
**En tant qu'** utilisateur premium  
**Je veux** gérer plusieurs compagnons  
**Afin de** varier les interactions

**Critères d'Acceptation:**
- [ ] Liste de tous mes compagnons
- [ ] Archivage/suppression
- [ ] Partage public avec lien
- [ ] Statistiques par compagnon
- [ ] Basculement rapide entre compagnons
- [ ] Import/Export configurations

**Tâches Techniques:**
- [ ] Endpoints CRUD compagnons
- [ ] Service ArchiveManager
- [ ] Génération liens partage
- [ ] Service statistiques
- [ ] Tests

**Story Points:** 5  
**Priorité:** Moyenne  
**Sprint:** 5

---

## EPIC 3: CONVERSATIONS ET INTERACTIONS

### US-010: Chat Textuel de Base
**En tant qu'** utilisateur  
**Je veux** converser par texte avec mon compagnon  
**Afin d'** avoir des interactions engageantes

**Critères d'Acceptation:**
- [ ] Interface chat temps réel
- [ ] Envoi/réception messages instantané
- [ ] Support emojis et formatage texte
- [ ] Indicateur "en train d'écrire"
- [ ] Temps de réponse < 2 secondes
- [ ] Historique scrollable

**Tâches Techniques:**
- [ ] WebSocket configuration
- [ ] Service ConversationEngine
- [ ] Intégration LLM API
- [ ] Service ContextManager
- [ ] UI composant Chat
- [ ] Tests

**Story Points:** 8  
**Priorité:** Critique  
**Sprint:** 2

---

### US-011: Mémoire Conversationnelle
**En tant qu'** utilisateur  
**Je veux** que mon compagnon se souvienne de nos échanges  
**Afin d'** avoir des conversations cohérentes

**Critères d'Acceptation:**
- [ ] Mémoire court terme (session)
- [ ] Mémoire long terme (sauvegardée)
- [ ] Références aux conversations passées
- [ ] Continuité entre sessions
- [ ] Limite selon abonnement (90j/1an/illimité)

**Tâches Techniques:**
- [ ] Service MemoryManager
- [ ] Stockage vecteurs embeddings
- [ ] Recherche sémantique
- [ ] Job nettoyage historique
- [ ] Tests

**Story Points:** 8  
**Priorité:** Haute  
**Sprint:** 3

---

### US-012: Envoi de Médias dans le Chat
**En tant qu'** utilisateur  
**Je veux** partager des images dans le chat  
**Afin d'** enrichir les conversations

**Critères d'Acceptation:**
- [ ] Upload d'images (PNG, JPG, max 10MB)
- [ ] Prévisualisation thumbnails
- [ ] Réactions du compagnon aux images
- [ ] Envoi de GIFs
- [ ] Partage de liens avec preview

**Tâches Techniques:**
- [ ] Upload service (S3/MinIO)
- [ ] Génération thumbnails
- [ ] Vision API pour analyse images
- [ ] Link preview service
- [ ] Tests

**Story Points:** 5  
**Priorité:** Moyenne  
**Sprint:** 5

---

### US-013: Recherche dans l'Historique
**En tant qu'** utilisateur  
**Je veux** rechercher dans mes conversations  
**Afin de** retrouver des informations

**Critères d'Acceptation:**
- [ ] Barre de recherche
- [ ] Recherche par mots-clés
- [ ] Filtres par date, compagnon, tags
- [ ] Surlignage des résultats
- [ ] Navigation rapide vers le message

**Tâches Techniques:**
- [ ] Indexation Elasticsearch
- [ ] Endpoint recherche
- [ ] UI composant Search
- [ ] Highlighting service
- [ ] Tests

**Story Points:** 5  
**Priorité:** Basse  
**Sprint:** 6

---

### US-014: Conversations Éphémères
**En tant qu'** utilisateur  
**Je veux** des conversations temporaires  
**Afin de** préserver ma confidentialité

**Critères d'Acceptation:**
- [ ] Option "Mode éphémère"
- [ ] Messages supprimés après 24h
- [ ] Notification du mode actif
- [ ] Pas de sauvegarde historique
- [ ] Impossibilité d'export

**Tâches Techniques:**
- [ ] Flag "ephemeral" sur conversations
- [ ] Job quotidien suppression
- [ ] Désactivation exports
- [ ] Tests

**Story Points:** 3  
**Priorité:** Basse  
**Sprint:** 7

---

## EPIC 4: GÉNÉRATION DE MÉDIAS

### US-015: Génération d'Images IA
**En tant qu'** utilisateur premium  
**Je veux** générer des images par IA  
**Afin de** visualiser des scènes personnalisées

**Critères d'Acceptation:**
- [ ] Saisie prompt textuel
- [ ] Sélection style (réaliste, anime, artistique)
- [ ] Choix résolution (512, 1024, 2048)
- [ ] File d'attente si charge élevée
- [ ] Génération en 10-30 secondes
- [ ] Coût en jetons (10/20/50)

**Tâches Techniques:**
- [ ] Intégration Stable Diffusion API
- [ ] Service ImageGenerationQueue
- [ ] Worker async génération
- [ ] Stockage S3/MinIO
- [ ] Tests

**Story Points:** 8  
**Priorité:** Haute  
**Sprint:** 4

---

### US-016: Galerie d'Images
**En tant qu'** utilisateur  
**Je veux** organiser mes images générées  
**Afin de** les retrouver facilement

**Critères d'Acceptation:**
- [ ] Liste toutes images générées
- [ ] Organisation par albums
- [ ] Tags personnalisés
- [ ] Favoris
- [ ] Recherche par prompt/tags
- [ ] Téléchargement HD

**Tâches Techniques:**
- [ ] Service GalleryManager
- [ ] Endpoints CRUD albums
- [ ] Service tagging
- [ ] UI galerie avec lazy loading
- [ ] Tests

**Story Points:** 5  
**Priorité:** Moyenne  
**Sprint:** 5

---

### US-017: Messages Vocaux
**En tant qu'** utilisateur standard  
**Je veux** envoyer des messages vocaux  
**Afin de** communiquer plus naturellement

**Critères d'Acceptation:**
- [ ] Enregistrement audio (max 2 min)
- [ ] Lecture audio player
- [ ] Transcription automatique (STT)
- [ ] Réponse textuelle ou vocale du compagnon
- [ ] Limite 5 min/mois (gratuit), illimité (premium)

**Tâches Techniques:**
- [ ] UI enregistreur audio
- [ ] Service AudioProcessor
- [ ] Intégration Whisper STT
- [ ] Stockage audio (S3/MinIO)
- [ ] Tests

**Story Points:** 5  
**Priorité:** Moyenne  
**Sprint:** 6

---

### US-018: Appels Vocaux
**En tant qu'** utilisateur premium  
**Je veux** appeler vocalement mon compagnon  
**Afin d'** avoir des conversations en temps réel

**Critères d'Acceptation:**
- [ ] Initiation d'appel
- [ ] Connexion WebRTC
- [ ] Latence < 300ms
- [ ] Qualité audio HD
- [ ] Durée illimitée (premium)
- [ ] Enregistrement optionnel

**Tâches Techniques:**
- [ ] Configuration WebRTC
- [ ] Serveur TURN/STUN
- [ ] Service VoiceCallManager
- [ ] Intégration TTS streaming
- [ ] Tests

**Story Points:** 13  
**Priorité:** Basse  
**Sprint:** 8

---

### US-019: Synthèse Vocale Personnalisée
**En tant qu'** utilisateur VIP  
**Je veux** personnaliser la voix de mon compagnon  
**Afin d'** augmenter l'immersion

**Critères d'Acceptation:**
- [ ] Sélection voix prédéfinies (10+)
- [ ] Paramètres voix (pitch, speed, tone)
- [ ] Prévisualisation
- [ ] Clonage voix (upload échantillon)
- [ ] Styles émotionnels (joyeux, triste, excité)

**Tâches Techniques:**
- [ ] Intégration ElevenLabs/Coqui TTS
- [ ] Service VoiceCustomization
- [ ] Voice cloning pipeline
- [ ] Tests

**Story Points:** 13  
**Priorité:** Basse  
**Sprint:** 9

---

## EPIC 5: SYSTÈME DE PAIEMENT

### US-020: Intégration Stripe
**En tant que** système  
**Je veux** traiter les paiements via Stripe  
**Afin de** monétiser l'application

**Critères d'Acceptation:**
- [ ] Configuration compte Stripe
- [ ] Webhooks configurés
- [ ] Gestion des erreurs paiement
- [ ] Conformité PCI-DSS
- [ ] Logs transactions
- [ ] Mode test et production

**Tâches Techniques:**
- [ ] Intégration Stripe SDK
- [ ] Service PaymentProcessor
- [ ] Endpoints webhooks
- [ ] Gestion 3D Secure
- [ ] Tests

**Story Points:** 8  
**Priorité:** Critique  
**Sprint:** 3

---

### US-021: Facturation Automatique
**En tant qu'** utilisateur abonné  
**Je veux** être facturé automatiquement  
**Afin de** ne pas interrompre