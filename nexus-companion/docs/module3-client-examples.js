// ============================================================================
// FICHIER: client-examples/javascript/companion-client.js
// Description: Client JavaScript pour l'API Companion
// ============================================================================

/**
 * Client JavaScript pour l'API Companion Management
 * 
 * @example
 * const client = new CompanionClient('http://localhost:8083', 'your-jwt-token');
 * const companion = await client.createCompanion({ name: 'Luna', ... });
 */

class CompanionClient {
    constructor(baseUrl, authToken) {
        this.baseUrl = baseUrl;
        this.authToken = authToken;
    }

    /**
     * Crée un nouveau compagnon
     */
    async createCompanion(companionData) {
        const response = await fetch(`${this.baseUrl}/api/v1/companions`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.authToken}`
            },
            body: JSON.stringify(companionData)
        });

        if (!response.ok) {
            throw new Error(`Erreur ${response.status}: ${await response.text()}`);
        }

        return await response.json();
    }

    /**
     * Récupère un compagnon par ID
     */
    async getCompanion(companionId) {
        const response = await fetch(
            `${this.baseUrl}/api/v1/companions/${companionId}`,
            {
                headers: {
                    'Authorization': `Bearer ${this.authToken}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(`Erreur ${response.status}: ${await response.text()}`);
        }

        return await response.json();
    }

    /**
     * Met à jour un compagnon
     */
    async updateCompanion(companionId, updates) {
        const response = await fetch(
            `${this.baseUrl}/api/v1/companions/${companionId}`,
            {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.authToken}`
                },
                body: JSON.stringify(updates)
            }
        );

        if (!response.ok) {
            throw new Error(`Erreur ${response.status}: ${await response.text()}`);
        }

        return await response.json();
    }

    /**
     * Fait évoluer un compagnon
     */
    async evolveCompanion(companionId, evolutionParams = {}) {
        const response = await fetch(
            `${this.baseUrl}/api/v1/companions/${companionId}/evolve`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.authToken}`
                },
                body: JSON.stringify(evolutionParams)
            }
        );

        if (!response.ok) {
            throw new Error(`Erreur ${response.status}: ${await response.text()}`);
        }

        return await response.json();
    }

    /**
     * Fusionne deux compagnons
     */
    async mergeCompanions(companion1Id, companion2Id, newName, ratio = 0.5) {
        const response = await fetch(
            `${this.baseUrl}/api/v1/companions/merge`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.authToken}`
                },
                body: JSON.stringify({
                    companion1Id,
                    companion2Id,
                    newCompanionName: newName,
                    ratio
                })
            }
        );

        if (!response.ok) {
            throw new Error(`Erreur ${response.status}: ${await response.text()}`);
        }

        return await response.json();
    }

    /**
     * Récupère la galerie publique
     */
    async getPublicGallery(page = 0, size = 20) {
        const response = await fetch(
            `${this.baseUrl}/api/v1/companions/public?page=${page}&size=${size}`
        );

        if (!response.ok) {
            throw new Error(`Erreur ${response.status}: ${await response.text()}`);
        }

        return await response.json();
    }

    /**
     * Like un compagnon public
     */
    async likeCompanion(companionId) {
        const response = await fetch(
            `${this.baseUrl}/api/v1/companions/${companionId}/like`,
            {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${this.authToken}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(`Erreur ${response.status}: ${await response.text()}`);
        }
    }
}

// ============================================================================
// FICHIER: client-examples/usage-example.js
// Description: Exemple d'utilisation complète
// ============================================================================

/**
 * Exemple d'utilisation du client Companion
 */

async function main() {
    const client = new CompanionClient(
        'http://localhost:8083',
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
    );

    try {
        // 1. Créer un compagnon
        console.log('📝 Création d\'un compagnon...');
        const companion = await client.createCompanion({
            name: 'Luna',
            appearance: {
                gender: 'FEMALE',
                hairColor: 'BLONDE',
                eyeColor: 'BLUE',
                skinTone: 'FAIR',
                bodyType: 'ATHLETIC',
                age: 25
            },
            personality: {
                traits: {
                    openness: 85,
                    conscientiousness: 70,
                    extraversion: 75,
                    agreeableness: 85,
                    neuroticism: 30,
                    humor: 70,
                    empathy: 90,
                    jealousy: 20,
                    curiosity: 85,
                    confidence: 75,
                    playfulness: 80,
                    assertiveness: 65,
                    sensitivity: 75,
                    rationality: 60,
                    creativity: 90,
                    loyalty: 85,
                    independence: 55,
                    patience: 70,
                    adventurousness: 75,
                    romanticism: 80
                },
                interests: ['art', 'music', 'literature'],
                dislikes: ['violence', 'dishonesty'],
                humorStyle: 'WITTY',
                communicationStyle: 'WARM'
            },
            voice: {
                voiceId: 'voice-001',
                pitch: 1.0,
                speed: 1.0,
                style: 'FRIENDLY'
            },
            backstory: 'Luna est une âme créative qui trouve la beauté dans les détails.'
        });

        console.log('✅ Compagnon créé:', companion.id);

        // 2. Récupérer le compagnon
        console.log('\n🔍 Récupération du compagnon...');
        const retrieved = await client.getCompanion(companion.id);
        console.log('✅ Compagnon récupéré:', retrieved.name);

        // 3. Faire évoluer
        console.log('\n🧬 Évolution du compagnon...');
        const evolved = await client.evolveCompanion(companion.id, {
            intensity: 5,
            targetTraits: ['empathy', 'creativity']
        });
        console.log('✅ Compagnon évolué');
        console.log('   Empathie:', evolved.personality.traits.empathy);
        console.log('   Créativité:', evolved.personality.traits.creativity);

        // 4. Mettre à jour
        console.log('\n📝 Mise à jour du compagnon...');
        const updated = await client.updateCompanion(companion.id, {
            isPublic: true
        });
        console.log('✅ Compagnon rendu public');

        // 5. Like
        console.log('\n❤️  Like du compagnon...');
        await client.likeCompanion(companion.id);
        console.log('✅ Like enregistré');

        // 6. Galerie publique
        console.log('\n🖼️  Récupération de la galerie...');
        const gallery = await client.getPublicGallery(0, 10);
        console.log(`✅ ${gallery.totalElements} compagnons publics trouvés`);

    } catch (error) {
        console.error('❌ Erreur:', error.message);
    }
}

// Exécuter l'exemple
if (typeof window === 'undefined') {
    // Node.js
    main();
}

// ============================================================================
// FICHIER: tests/load-test.js
// Description: Tests de charge avec K6
// ============================================================================

/**
 * Script de test de charge K6
 * 
 * Installation: brew install k6  (macOS)
 * Exécution: k6 run load-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Métriques personnalisées
const errorRate = new Rate('errors');
const companionCreationTime = new Trend('companion_creation_time');

// Configuration du test
export const options = {
    stages: [
        { duration: '2m', target: 50 },   // Montée à 50 utilisateurs
        { duration: '5m', target: 50 },   // Maintien à 50 utilisateurs
        { duration: '2m', target: 100 },  // Montée à 100 utilisateurs
        { duration: '5m', target: 100 },  // Maintien à 100 utilisateurs
        { duration: '2m', target: 0 },    // Descente à 0
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500'],  // 95% des requêtes < 500ms
        'errors': ['rate<0.01'],              // Taux d'erreur < 1%
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8083';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || 'test-token';

/**
 * Fonction de setup (exécutée une fois)
 */
export function setup() {
    console.log('🚀 Démarrage du test de charge...');
    return { baseUrl: BASE_URL, token: AUTH_TOKEN };
}

/**
 * Fonction principale (exécutée par chaque VU)
 */
export default function(data) {
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${data.token}`
    };

    // Scénario 1: Créer un compagnon
    const companionPayload = JSON.stringify({
        name: `TestCompanion-${__VU}-${__ITER}`,
        appearance: {
            gender: 'FEMALE',
            hairColor: 'BLONDE',
            eyeColor: 'BLUE',
            skinTone: 'FAIR',
            bodyType: 'ATHLETIC',
            age: 25
        },
        personality: {
            traits: generateRandomTraits(),
            interests: ['music', 'art'],
            humorStyle: 'WITTY',
            communicationStyle: 'WARM'
        },
        voice: {
            voiceId: 'voice-001',
            pitch: 1.0,
            speed: 1.0,
            style: 'FRIENDLY'
        },
        backstory: 'Test companion'
    });

    const createStart = new Date();
    const createRes = http.post(
        `${data.baseUrl}/api/v1/companions`,
        companionPayload,
        { headers }
    );
    companionCreationTime.add(new Date() - createStart);

    const createSuccess = check(createRes, {
        'companion created': (r) => r.status === 201,
        'has companion id': (r) => JSON.parse(r.body).id !== undefined,
    });

    errorRate.add(!createSuccess);

    if (createSuccess) {
        const companion = JSON.parse(createRes.body);

        // Scénario 2: Récupérer le compagnon
        const getRes = http.get(
            `${data.baseUrl}/api/v1/companions/${companion.id}`,
            { headers }
        );

        check(getRes, {
            'companion retrieved': (r) => r.status === 200,
        });

        // Scénario 3: Mettre à jour
        const updatePayload = JSON.stringify({
            isPublic: Math.random() > 0.5
        });

        const updateRes = http.put(
            `${data.baseUrl}/api/v1/companions/${companion.id}`,
            updatePayload,
            { headers }
        );

        check(updateRes, {
            'companion updated': (r) => r.status === 200,
        });
    }

    // Scénario 4: Galerie publique
    const galleryRes = http.get(
        `${data.baseUrl}/api/v1/companions/public?page=0&size=20`
    );

    check(galleryRes, {
        'gallery retrieved': (r) => r.status === 200,
    });

    sleep(1); // Pause de 1 seconde entre les itérations
}

/**
 * Génère des traits aléatoires
 */
function generateRandomTraits() {
    const traits = {};
    const traitNames = [
        'openness', 'conscientiousness', 'extraversion', 'agreeableness',
        'neuroticism', 'humor', 'empathy', 'jealousy', 'curiosity',
        'confidence', 'playfulness', 'assertiveness', 'sensitivity',
        'rationality', 'creativity', 'loyalty', 'independence',
        'patience', 'adventurousness', 'romanticism'
    ];

    traitNames.forEach(trait => {
        traits[trait] = Math.floor(Math.random() * 101);
    });

    return traits;
}

/**
 * Fonction de teardown (exécutée une fois à la fin)
 */
export function teardown(data) {
    console.log('✅ Test de charge terminé');
}

// ============================================================================
// FICHIER: tests/integration-test.sh
// Description: Script de tests d'intégration
// ============================================================================

#!/bin/bash
# tests/integration-test.sh

set -e

echo "🧪 Démarrage des tests d'intégration"

BASE_URL="http://localhost:8083"
AUTH_TOKEN="test-token-123"

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Fonction de test
test_endpoint() {
    local name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected_status=$5

    echo -e "${YELLOW}Test: ${name}${NC}"

    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X ${method} \
            -H "Authorization: Bearer ${AUTH_TOKEN}" \
            "${BASE_URL}${endpoint}")
    else
        response=$(curl -s -w "\n%{http_code}" -X ${method} \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ${AUTH_TOKEN}" \
            -d "${data}" \
            "${BASE_URL}${endpoint}")
    fi

    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$status_code" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ Succès (${status_code})${NC}"
        echo "$body"
        return 0
    else
        echo -e "${RED}✗ Échec (attendu: ${expected_status}, reçu: ${status_code})${NC}"
        echo "$body"
        return 1
    fi
}

# Attendre que le service soit prêt
echo "⏳ Attente du service..."
timeout 60 bash -c 'until curl -f ${BASE_URL}/actuator/health; do sleep 2; done'
echo -e "${GREEN}✓ Service prêt${NC}\n"

# Test 1: Health Check
test_endpoint "Health Check" "GET" "/actuator/health" "" 200

echo ""

# Test 2: Galerie publique (sans auth)
test_endpoint "Galerie Publique" "GET" "/api/v1/companions/public" "" 200

echo ""

# Test 3: Créer un compagnon
COMPANION_DATA='{
  "name": "TestCompanion",
  "appearance": {
    "gender": "FEMALE",
    "hairColor": "BLONDE",
    "eyeColor": "BLUE",
    "skinTone": "FAIR",
    "bodyType": "ATHLETIC",
    "age": 25
  },
  "personality": {
    "traits": {
      "openness": 80,
      "conscientiousness": 70,
      "extraversion": 75,
      "agreeableness": 85,
      "neuroticism": 30,
      "humor": 70,
      "empathy": 90,
      "jealousy": 20,
      "curiosity": 85,
      "confidence": 75,
      "playfulness": 80,
      "assertiveness": 65,
      "sensitivity": 75,
      "rationality": 60,
      "creativity": 85,
      "loyalty": 90,
      "independence": 55,
      "patience": 70,
      "adventurousness": 75,
      "romanticism": 80
    },
    "interests": ["music"],
    "humorStyle": "WITTY",
    "communicationStyle": "WARM"
  },
  "voice": {
    "voiceId": "voice-001",
    "pitch": 1.0,
    "speed": 1.0,
    "style": "FRIENDLY"
  }
}'

COMPANION_RESPONSE=$(test_endpoint "Créer Compagnon" "POST" "/api/v1/companions" "$COMPANION_DATA" 201)
COMPANION_ID=$(echo "$COMPANION_RESPONSE" | jq -r '.id')

echo -e "\n${GREEN}Compagnon créé avec ID: ${COMPANION_ID}${NC}\n"

# Test 4: Récupérer le compagnon
test_endpoint "Récupérer Compagnon" "GET" "/api/v1/companions/${COMPANION_ID}" "" 200

echo ""

# Test 5: Mettre à jour
UPDATE_DATA='{"isPublic": true}'
test_endpoint "Mettre à Jour" "PUT" "/api/v1/companions/${COMPANION_ID}" "$UPDATE_DATA" 200

echo ""

# Test 6: Faire évoluer
EVOLVE_DATA='{"intensity": 5}'
test_endpoint "Évoluer" "POST" "/api/v1/companions/${COMPANION_ID}/evolve" "$EVOLVE_DATA" 200

echo ""

# Test 7: Supprimer
test_endpoint "Supprimer" "DELETE" "/api/v1/companions/${COMPANION_ID}" "" 204

echo -e "\n${GREEN}✅ Tous les tests d'intégration ont réussi!${NC}"

// ============================================================================
// FICHIER: postman/NexusAI-Companion.postman_collection.json
// Description: Collection Postman complète
// ============================================================================

/**
 * Collection Postman pour l'API Companion
 * Importer ce fichier dans Postman pour tester l'API
 */

{
  "info": {
    "name": "NexusAI - Companion API",
    "description": "Collection complète pour tester l'API Companion Management",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8083",
      "type": "string"
    },
    {
      "key": "authToken",
      "value": "your-jwt-token-here",
      "type": "string"
    },
    {
      "key": "companionId",
      "value": "",
      "type": "string"
    }
  ],
  "item": [
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{baseUrl}}/actuator/health",
          "host": ["{{baseUrl}}"],
          "path": ["actuator", "health"]
        }
      }
    },
    {
      "name": "Créer Compagnon",
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "pm.test('Status code is 201', function () {",
              "    pm.response.to.have.status(201);",
              "});",
              "",
              "const response = pm.response.json();",
              "pm.collectionVariables.set('companionId', response.id);"
            ]
          }
        }
      ],
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{authToken}}"
          },
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"name\": \"Luna\",\n  \"appearance\": {\n    \"gender\": \"FEMALE\",\n    \"hairColor\": \"BLONDE\",\n    \"eyeColor\": \"BLUE\",\n    \"skinTone\": \"FAIR\",\n    \"bodyType\": \"ATHLETIC\",\n    \"age\": 25\n  },\n  \"personality\": {\n    \"traits\": {\n      \"openness\": 85,\n      \"empathy\": 90,\n      \"creativity\": 90\n    },\n    \"interests\": [\"art\", \"music\"],\n    \"humorStyle\": \"WITTY\",\n    \"communicationStyle\": \"WARM\"\n  },\n  \"voice\": {\n    \"voiceId\": \"voice-001\",\n    \"pitch\": 1.0,\n    \"speed\": 1.0\n  }\n}"
        },
        "url": {
          "raw": "{{baseUrl}}/api/v1/companions",
          "host": ["{{baseUrl}}"],
          "path": ["api", "v1", "companions"]
        }
      }
    },
    {
      "name": "Récupérer Compagnon",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{authToken}}"
          }
        ],
        "url": {
          "raw": "{{baseUrl}}/api/v1/companions/{{companionId}}",
          "host": ["{{baseUrl}}"],
          "path": ["api", "v1", "companions", "{{companionId}}"]
        }
      }
    },
    {
      "name": "Galerie Publique",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{baseUrl}}/api/v1/companions/public?page=0&size=20",
          "host": ["{{baseUrl}}"],
          "path": ["api", "v1", "companions", "public"],
          "query": [
            {"key": "page", "value": "0"},
            {"key": "size", "value": "20"}
          ]
        }
      }
    }
  ]
}