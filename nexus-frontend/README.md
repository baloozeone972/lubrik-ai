# NexusAI Frontend

Application web React pour la plateforme NexusAI.

## Stack Technique

- **React 18** avec TypeScript
- **Vite** pour le build et le dev server
- **Tailwind CSS** pour le styling
- **React Router** pour la navigation
- **Zustand** pour le state management
- **React Query** pour le data fetching
- **React Hook Form** + Zod pour les formulaires
- **Axios** pour les requêtes HTTP

## Installation

```bash
# Installer les dépendances
npm install

# Copier le fichier d'environnement
cp .env.example .env.local

# Lancer en développement
npm run dev
```

## Scripts Disponibles

| Commande | Description |
|----------|-------------|
| `npm run dev` | Lance le serveur de développement |
| `npm run build` | Build pour la production |
| `npm run preview` | Preview du build de production |
| `npm run lint` | Lint le code |
| `npm run test` | Lance les tests |

## Structure du Projet

```
nexus-frontend/
├── public/                 # Assets statiques
├── src/
│   ├── components/        # Composants React
│   │   ├── common/       # Composants réutilisables
│   │   ├── layout/       # Layout et navigation
│   │   ├── auth/         # Composants auth
│   │   ├── chat/         # Composants chat
│   │   └── companion/    # Composants companion
│   ├── pages/            # Pages/Routes
│   ├── services/         # API services
│   ├── store/            # State management (Zustand)
│   ├── hooks/            # Custom hooks
│   ├── types/            # Types TypeScript
│   ├── utils/            # Utilitaires
│   ├── App.tsx           # App principal
│   ├── main.tsx          # Point d'entrée
│   └── index.css         # Styles globaux
├── index.html
├── vite.config.ts
├── tailwind.config.js
├── tsconfig.json
└── package.json
```

## Fonctionnalités

### Authentification
- Inscription avec validation
- Connexion avec JWT
- Refresh token automatique
- Déconnexion

### Compagnons
- Liste des compagnons
- Création/édition de compagnons
- Upload d'avatar
- Configuration de personnalité

### Conversations
- Liste des conversations
- Chat en temps réel (WebSocket)
- Streaming des réponses IA
- Historique des messages

## Configuration

### Variables d'environnement

```env
VITE_API_URL=http://localhost:8080/api/v1
VITE_WS_URL=ws://localhost:8080/ws
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_xxxxx
```

### Proxy en développement

Le proxy est configuré dans `vite.config.ts` pour rediriger `/api` vers le backend.

## Build Production

```bash
npm run build
```

Le build sera généré dans `dist/`.

## Déploiement

### Vercel
```bash
vercel deploy
```

### Docker
```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
```
