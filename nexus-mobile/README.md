# NexusAI Mobile

Application mobile React Native/Expo pour la plateforme NexusAI.

## Stack Technique

- **React Native** avec Expo SDK 50
- **TypeScript**
- **React Navigation** pour la navigation
- **Zustand** pour le state management
- **React Query** pour le data fetching
- **Expo Secure Store** pour le stockage sécurisé
- **Axios** pour les requêtes HTTP

## Installation

```bash
# Installer les dépendances
npm install

# Lancer l'application
npm start
```

## Scripts Disponibles

| Commande | Description |
|----------|-------------|
| `npm start` | Lance Expo Go |
| `npm run android` | Lance sur Android |
| `npm run ios` | Lance sur iOS |
| `npm run web` | Lance sur le web |
| `npm run lint` | Lint le code |
| `npm test` | Lance les tests |

## Structure du Projet

```
nexus-mobile/
├── assets/                # Images et icônes
├── src/
│   ├── components/       # Composants réutilisables
│   ├── screens/          # Écrans de l'app
│   │   ├── auth/        # Login, Register
│   │   ├── home/        # Dashboard
│   │   ├── companion/   # Gestion compagnons
│   │   ├── conversation/# Chat
│   │   └── profile/     # Profil utilisateur
│   ├── navigation/       # Configuration navigation
│   ├── services/         # API services
│   ├── store/            # State management
│   ├── hooks/            # Custom hooks
│   ├── types/            # Types TypeScript
│   └── utils/            # Utilitaires
├── App.tsx               # Point d'entrée
├── app.json              # Config Expo
└── package.json
```

## Fonctionnalités

### Authentification
- Inscription avec validation
- Connexion avec JWT
- Stockage sécurisé des tokens
- Refresh automatique
- Déconnexion

### Compagnons
- Liste des compagnons
- Création/édition
- Configuration personnalité
- Suppression

### Conversations
- Liste des conversations
- Chat avec le compagnon
- Envoi de messages
- Historique

### Profil
- Affichage du profil
- Paramètres
- Déconnexion

## Configuration

### Variables d'environnement

Créez un fichier `.env` :

```env
EXPO_PUBLIC_API_URL=http://localhost:8080/api/v1
```

## Build

### Development Build

```bash
# Android
npx expo run:android

# iOS
npx expo run:ios
```

### Production Build

```bash
# EAS Build (recommandé)
npx eas build --platform all

# ou localement
npx expo build:android
npx expo build:ios
```

## Publication

### TestFlight (iOS)

```bash
npx eas submit --platform ios
```

### Play Store (Android)

```bash
npx eas submit --platform android
```

## Architecture

```
┌─────────────────────────────────────────────┐
│                   App.tsx                    │
│           (Providers & Navigation)           │
├─────────────────────────────────────────────┤
│              RootNavigator                   │
│    ┌─────────────┐  ┌─────────────────┐    │
│    │AuthNavigator│  │  MainNavigator  │    │
│    │ - Login     │  │ - Home Tab      │    │
│    │ - Register  │  │ - Companions Tab│    │
│    └─────────────┘  │ - Conversations │    │
│                     │ - Profile Tab   │    │
│                     └─────────────────┘    │
├─────────────────────────────────────────────┤
│                  Services                    │
│  ┌─────────┐ ┌────────────┐ ┌───────────┐ │
│  │ api.ts  │ │authService │ │companion  │ │
│  │ (Axios) │ │            │ │Service    │ │
│  └─────────┘ └────────────┘ └───────────┘ │
├─────────────────────────────────────────────┤
│                   Store                      │
│  ┌─────────────┐    ┌─────────────┐        │
│  │ authStore   │    │ chatStore   │        │
│  │ (Zustand)   │    │ (Zustand)   │        │
│  └─────────────┘    └─────────────┘        │
└─────────────────────────────────────────────┘
```

## Tests

```bash
# Tests unitaires
npm test

# Coverage
npm test -- --coverage
```
