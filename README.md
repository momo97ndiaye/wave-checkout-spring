# wave-checkout-spring

Intégration de l'API [Wave Checkout](https://docs.wave.com/checkout) dans une application Spring Boot :
création d'un paiement, réception des webhooks signés, réconciliation.

![CI](https://github.com/momo97ndiaye/wave-checkout-spring/actions/workflows/ci.yml/badge.svg)

## Fonctionnement

1. `POST /api/payments` enregistre le paiement (statut `PENDING`) et crée une session Wave.
   La réponse contient `launchUrl`, l'adresse vers laquelle rediriger le client.
2. Wave appelle `POST /webhooks/wave` quand le paiement aboutit ou échoue.
   - La signature `Wave-Signature` (HMAC-SHA256 du timestamp + corps brut) est vérifiée ; une requête de plus de 5 minutes est refusée.
   - Chaque événement n'est traité qu'une fois (table `processed_event`) : Wave peut renvoyer le même événement.
3. `POST /api/payments/{ref}/reconcile` relit l'état chez Wave si un webhook a été perdu.

Un paiement `SUCCEEDED` ne change plus de statut, quel que soit l'ordre d'arrivée des événements.

## Stack

Java 17 · Spring Boot 3.3 · Spring Data JPA · PostgreSQL · Flyway · RestClient · springdoc-openapi · JUnit 5 · GitHub Actions

## Lancer en local

```bash
docker compose up -d
export WAVE_API_KEY=...          # clé API Wave Business
export WAVE_SIGNING_SECRET=...   # secret de signature des webhooks
mvn spring-boot:run
```

Documentation de l'API : http://localhost:8080/swagger-ui.html

```bash
curl -X POST localhost:8080/api/payments -H 'Content-Type: application/json' -d '{"amount": 5000}'
```

## Tests

```bash
mvn verify
```

Les tests n'appellent jamais Wave : l'API est simulée (`MockRestServiceServer`) et la base est H2.
Ils couvrent la vérification de signature, le client Wave, et le parcours complet paiement → webhook → statut.

## Configuration

| Variable | Rôle |
|---|---|
| `WAVE_API_KEY` | clé API (en-tête `Authorization: Bearer`) |
| `WAVE_SIGNING_SECRET` | secret de signature des webhooks |
| `WAVE_SUCCESS_URL`, `WAVE_ERROR_URL` | pages de retour après paiement |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | base PostgreSQL |
