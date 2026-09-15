# Suraksha Insurance

## What is this application all about?

Suraksha is a full-stack insurance platform demo covering the core customer
and back-office journeys for an insurer: registering and logging in, viewing
policies, filing claims, paying premiums, and having those claims triaged
and scored for fraud risk. On top of that operational core, it layers a set
of AI-assisted features (claim triage summaries, a support chatbot, a
natural-language claim-filing assistant, document/photo analysis, policy
recommendations, renewal insights, and an adjuster Q&A tool) that use the
Anthropic API to help customers and staff move faster without replacing the
deterministic business logic underneath them.

## About Application Stack

- **Frontend:** React (Vite), served on port 5173 in local dev
- **Backend API:** Spring Boot (Java 21), the main REST service for auth,
  policies, claims, and payments — port 8080
- **Fraud detection service:** a separate Spring Boot microservice that
  consumes claim events and scores fraud risk — port 8081
- **AI service:** a separate Spring Boot microservice wrapping the
  Anthropic API for all AI-assisted features — port 8082
- **Database:** PostgreSQL
- **Cache / session store:** Redis (refresh tokens)
- **Messaging:** Kafka (claim submission/flagging events between backend and
  fraud-detection-service)
- **Payments:** Razorpay (Orders API + signature-verified webhook)
- **Containerization:** Docker Compose for local dev, Kubernetes manifests
  (`k8s/`) for deployment

## About Application Architecture

Suraksha is split into four independently deployable services around a
shared PostgreSQL database:

1. **`backend`** — the primary Spring Boot API. Owns authentication (JWT
   access tokens + Redis-backed refresh tokens, double-submit-cookie CSRF
   protection), and the Policy, Claims, and Payment domains. Publishes a
   `claim.submitted` Kafka event whenever a claim is filed.
2. **`fraud-detection-service`** — consumes `claim.submitted`, scores the
   claim against policy coverage and open-claims signals, writes the risk
   score back to the database, and publishes `claim.flagged` for claims that
   cross a risk threshold. Has no public endpoints; it only talks to
   Kafka and the database.
3. **`ai-service`** — consumes `claim.flagged` to generate adjuster triage
   notes, and exposes REST endpoints for the chatbot, document/photo
   analysis, the claim-filing assistant, renewal insights, and adjuster
   Q&A, all backed by the Anthropic API with rule-based fallbacks when no
   API key is configured.
4. **`frontend`** — the React SPA that customers, agents, and adjusters use,
   talking to `backend` and `ai-service` over REST.

Service-to-service calls (fraud-detection-service and ai-service into
backend's internal endpoints) are authenticated with a shared internal
service token, separate from customer-facing JWTs. In Kubernetes, a
deny-by-default NetworkPolicy set restricts traffic to only the paths each
service actually needs, and an Ingress with TLS fronts the public-facing
services.
