# Nexus Payment

Module de gestion des paiements et abonnements via Stripe.

## Responsabilités

- **Abonnements** : Gestion du cycle de vie des abonnements
- **Paiements** : Traitement sécurisé via Stripe
- **Webhooks** : Réception des événements Stripe
- **Factures** : Accès à l'historique de facturation
- **Plans** : Configuration des plans tarifaires

## Structure

```
nexus-payment/
├── src/main/java/com/nexusai/payment/
│   ├── service/
│   │   └── PaymentService.java         # Service principal
│   ├── webhook/
│   │   └── StripeWebhookHandler.java   # Handler webhooks
│   └── dto/
│       ├── CreateSubscriptionRequest.java
│       ├── SubscriptionResponse.java
│       ├── PaymentMethodDTO.java
│       ├── InvoiceDTO.java
│       ├── SetupIntentDTO.java
│       └── PlanDTO.java
```

## Plans d'Abonnement

| Plan | Prix | Messages/mois | Compagnons |
|------|------|---------------|------------|
| FREE | 0€ | 50 | 1 |
| BASIC | 9.99€ | 500 | 3 |
| PREMIUM | 19.99€ | Illimité | 10 |
| ENTERPRISE | Sur devis | Illimité | Illimité |

## Configuration Stripe

```yaml
nexusai:
  payment:
    stripe:
      api-key: ${STRIPE_API_KEY}
      webhook-secret: ${STRIPE_WEBHOOK_SECRET}
    plans:
      basic:
        price-id: price_xxxxx
      premium:
        price-id: price_yyyyy
```

## Flow d'Abonnement

```
1. Client: Setup Intent
   POST /api/v1/payments/setup-intent
   ↓
2. Client: Collecte carte (Stripe.js)
   ↓
3. Client: Créer abonnement
   POST /api/v1/payments/subscribe
   ↓
4. Stripe: Webhook payment_intent.succeeded
   ↓
5. Backend: Activer abonnement
```

## Webhooks Stripe

| Événement | Action |
|-----------|--------|
| `customer.subscription.created` | Créer abonnement |
| `customer.subscription.updated` | Mettre à jour |
| `customer.subscription.deleted` | Annuler |
| `invoice.paid` | Confirmer paiement |
| `invoice.payment_failed` | Notifier échec |

## Utilisation

```java
@Autowired
private PaymentService paymentService;

// Créer un abonnement
SubscriptionResponse subscription = paymentService.createSubscription(
    userId,
    CreateSubscriptionRequest.builder()
        .planId("premium")
        .paymentMethodId("pm_xxx")
        .build()
);

// Annuler
paymentService.cancelSubscription(userId);

// Récupérer factures
List<InvoiceDTO> invoices = paymentService.getInvoices(userId);
```

## Endpoints (via nexus-api)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/payments/setup-intent` | Créer setup intent |
| POST | `/api/v1/payments/subscribe` | S'abonner |
| GET | `/api/v1/payments/subscription` | Mon abonnement |
| POST | `/api/v1/payments/cancel` | Annuler |
| PUT | `/api/v1/payments/update` | Changer de plan |
| GET | `/api/v1/payments/invoices` | Mes factures |
| GET | `/api/v1/payments/payment-methods` | Mes moyens de paiement |
| POST | `/api/v1/webhooks/stripe` | Webhook Stripe |

## Sécurité

- Signature webhook vérifiée
- Pas de stockage de données carte (PCI DSS)
- Retry logic pour webhooks
- Idempotency keys

## Dépendances

- nexus-commons
- nexus-core

**Dépendances externes** :
- Stripe Java SDK

## Tests

```bash
mvn test -pl nexus-payment
```

## Stripe CLI (Dev)

```bash
# Écouter les webhooks en local
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe

# Déclencher un événement test
stripe trigger payment_intent.succeeded
```
