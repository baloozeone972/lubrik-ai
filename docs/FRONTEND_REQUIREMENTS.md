# Frontend & Mobile Requirements

Ce document décrit les spécifications pour les applications frontend (web) et mobile devant consommer l'API NexusAI.

## Vue d'Ensemble

NexusAI nécessite deux types de clients :
1. **Application Web** : Interface responsive pour desktop et tablette
2. **Applications Mobile** : iOS et Android natives ou cross-platform

## Stack Technologique Recommandée

### Application Web
| Composant | Technologie Recommandée | Alternatives |
|-----------|------------------------|--------------|
| Framework | **React 18+** | Vue.js 3, Next.js |
| State Management | **Zustand** ou Redux Toolkit | Jotai, Recoil |
| Styling | **Tailwind CSS** | Styled Components, CSS Modules |
| HTTP Client | **Axios** | fetch, TanStack Query |
| WebSocket | **Socket.io-client** | Native WebSocket |
| Forms | **React Hook Form** + Zod | Formik |
| UI Components | **shadcn/ui** | Radix UI, Headless UI |
| Routing | **React Router v6** | TanStack Router |

### Applications Mobile
| Composant | Technologie Recommandée | Alternatives |
|-----------|------------------------|--------------|
| Framework | **React Native** | Flutter, SwiftUI/Kotlin |
| Navigation | **React Navigation** | Expo Router |
| State | **Zustand** | Redux Toolkit, Jotai |
| HTTP | **Axios** | fetch |
| Storage | **MMKV** | AsyncStorage |
| Push Notifications | **Firebase Cloud Messaging** | OneSignal |

---

## Fonctionnalités par Écran

### 1. Authentification

#### Écran Login
```
- Email input
- Password input (avec toggle visibilité)
- Bouton "Se connecter"
- Lien "Mot de passe oublié"
- Lien "Créer un compte"
- OAuth buttons (optionnel: Google, Apple)
```

#### Écran Register
```
- Username input
- Email input
- Password input (avec indicateur force)
- Confirm password input
- Checkbox CGU
- Bouton "S'inscrire"
```

#### Écran Verify Email
```
- Message d'instruction
- Bouton "Renvoyer l'email"
- Lien vers inbox
```

### 2. Dashboard / Home

```
- Header avec avatar utilisateur
- Liste des conversations récentes
- Bouton "Nouvelle conversation"
- Liste des compagnons
- Stats rapides (messages aujourd'hui, etc.)
```

### 3. Compagnons

#### Liste des Compagnons
```
- Grid/List de compagnons avec:
  - Avatar
  - Nom
  - Description courte
  - Indicateur actif/draft
- Bouton "Créer un compagnon"
- Filtres (tous, actifs, brouillons)
```

#### Création/Édition Compagnon
```
- Upload avatar (crop, resize)
- Nom (required)
- Description
- Sélecteur de style (FRIENDLY, PROFESSIONAL, etc.)
- Éditeur de personnalité:
  - Traits (tags)
  - Spécialités (tags)
  - Ton
  - Instructions personnalisées (textarea)
- Bouton "Tester" (preview conversation)
- Bouton "Sauvegarder"
```

### 4. Conversation / Chat

#### Liste des Conversations
```
- Liste triée par date
- Pour chaque conversation:
  - Avatar compagnon
  - Nom compagnon
  - Dernier message (preview)
  - Date/heure
  - Badge non-lus
- Swipe actions (archiver, supprimer)
- Recherche
```

#### Écran Chat
```
- Header:
  - Bouton retour
  - Avatar + nom compagnon
  - Menu (options conversation)

- Zone messages:
  - Messages utilisateur (droite, couleur A)
  - Messages compagnon (gauche, couleur B)
  - Indicateur "typing..." pendant streaming
  - Support markdown dans les messages
  - Images inline
  - Timestamps groupés

- Zone input:
  - Textarea auto-resize
  - Bouton pièce jointe (image, audio)
  - Bouton envoyer
  - Indicateur caractères restants
```

#### Streaming en Temps Réel
```javascript
// WebSocket connection
const ws = new WebSocket(`wss://api.nexusai.com/ws/chat?token=${jwt}`);

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);

  if (data.type === 'CHUNK') {
    // Append to current message
    appendToMessage(data.content);
  } else if (data.type === 'DONE') {
    // Finalize message
    finalizeMessage(data.messageId);
  }
};
```

### 5. Profil & Paramètres

#### Profil
```
- Avatar (upload/change)
- Username (readonly)
- Email (avec vérification si changé)
- Bouton "Changer mot de passe"
- Section abonnement actuel
```

#### Paramètres
```
- Notifications (toggle)
- Thème (clair/sombre/auto)
- Langue
- Déconnexion
- Déconnexion tous appareils
- Supprimer compte (danger zone)
```

### 6. Abonnement / Paiement

#### Page Plans
```
- Comparatif des plans (FREE, BASIC, PREMIUM)
- Indicateur plan actuel
- Boutons "Choisir ce plan"
```

#### Checkout
```
- Résumé plan choisi
- Formulaire carte (Stripe Elements)
- Bouton "S'abonner"
- Sécurité badges
```

#### Gestion Abonnement
```
- Plan actuel
- Date renouvellement
- Historique factures (téléchargeables)
- Bouton "Changer de plan"
- Bouton "Annuler abonnement"
```

---

## Spécifications Techniques

### Authentication Flow

```
┌─────────────┐    POST /auth/login     ┌─────────────┐
│   Client    │ ────────────────────────▶│   Backend   │
│             │◀──────────────────────── │             │
└─────────────┘   { accessToken,        └─────────────┘
                    refreshToken }
        │
        │ Store tokens
        ▼
┌─────────────┐
│  Secure     │  accessToken: memory
│  Storage    │  refreshToken: secure storage
└─────────────┘
```

### Token Refresh Logic

```javascript
// Interceptor Axios
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      try {
        const { accessToken } = await refreshTokens();
        error.config.headers.Authorization = `Bearer ${accessToken}`;
        return axios(error.config);
      } catch {
        logout();
        redirectToLogin();
      }
    }
    return Promise.reject(error);
  }
);
```

### WebSocket Reconnection

```javascript
class ChatWebSocket {
  constructor(url) {
    this.url = url;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }

  connect() {
    this.ws = new WebSocket(this.url);

    this.ws.onclose = () => {
      if (this.reconnectAttempts < this.maxReconnectAttempts) {
        const delay = Math.min(1000 * 2 ** this.reconnectAttempts, 30000);
        setTimeout(() => this.connect(), delay);
        this.reconnectAttempts++;
      }
    };

    this.ws.onopen = () => {
      this.reconnectAttempts = 0;
    };
  }
}
```

### Offline Support (Mobile)

```javascript
// Queue messages when offline
const messageQueue = [];

const sendMessage = async (message) => {
  if (!isOnline()) {
    messageQueue.push(message);
    saveToLocalStorage(messageQueue);
    return;
  }

  await api.sendMessage(message);
};

// Sync when back online
const syncMessages = async () => {
  const queue = getFromLocalStorage('messageQueue');
  for (const message of queue) {
    await api.sendMessage(message);
  }
  clearQueue();
};
```

---

## Design System

### Couleurs

```css
:root {
  /* Primary */
  --primary-50: #eff6ff;
  --primary-500: #3b82f6;
  --primary-600: #2563eb;
  --primary-700: #1d4ed8;

  /* Neutral */
  --gray-50: #f9fafb;
  --gray-100: #f3f4f6;
  --gray-500: #6b7280;
  --gray-900: #111827;

  /* Semantic */
  --success: #10b981;
  --warning: #f59e0b;
  --error: #ef4444;

  /* Chat */
  --user-message-bg: #3b82f6;
  --companion-message-bg: #f3f4f6;
}
```

### Typography

```css
/* Headings */
h1: 2.25rem (36px), font-weight: 700
h2: 1.875rem (30px), font-weight: 600
h3: 1.5rem (24px), font-weight: 600

/* Body */
body: 1rem (16px), font-weight: 400
small: 0.875rem (14px), font-weight: 400

/* Font Family */
font-family: 'Inter', system-ui, sans-serif;
```

### Spacing

```
4px, 8px, 12px, 16px, 24px, 32px, 48px, 64px
```

### Breakpoints

```css
/* Mobile first */
sm: 640px
md: 768px
lg: 1024px
xl: 1280px
```

---

## Performance Requirements

| Métrique | Cible Web | Cible Mobile |
|----------|-----------|--------------|
| First Contentful Paint | < 1.5s | < 2s |
| Time to Interactive | < 3s | < 3.5s |
| Largest Contentful Paint | < 2.5s | < 3s |
| Bundle Size (gzipped) | < 200KB | N/A |
| App Size | N/A | < 50MB |
| Memory Usage | N/A | < 150MB |

---

## Accessibilité (a11y)

- WCAG 2.1 Level AA
- Navigation clavier complète
- Screen reader support
- Contrast ratio minimum 4.5:1
- Focus indicators visibles
- Labels sur tous les inputs
- Alt text sur les images

---

## Tests Requis

### Tests Unitaires
- Composants React/RN
- Hooks personnalisés
- Utilitaires

### Tests d'Intégration
- Flows d'authentification
- Flows de conversation
- Flows de paiement

### Tests E2E
- Cypress (Web)
- Detox (Mobile)

### Coverage Minimum
- 70% pour les composants
- 80% pour la logique métier

---

## Déploiement

### Web
```
Production: Vercel / Netlify / CloudFront
Staging: Vercel Preview
CI/CD: GitHub Actions
```

### Mobile
```
iOS: TestFlight → App Store
Android: Play Console Internal → Production
CI/CD: Fastlane + GitHub Actions
```

---

## Timeline Estimée

| Phase | Durée | Livrables |
|-------|-------|-----------|
| Setup & Auth | 2 semaines | Login, Register, Token management |
| Core Features | 4 semaines | Companions, Conversations, Chat |
| Payments | 2 semaines | Plans, Checkout, Subscription |
| Polish | 2 semaines | A11y, Performance, Tests |
| **Total** | **10 semaines** | MVP complet |
