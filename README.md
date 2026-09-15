# Suraksha Insurance — Full Stack Demo

## About the Application

Suraksha is a working insurance platform that lets a customer register, log
in, buy and view policies, file claims, and pay premiums, while giving
internal staff (agents, claims adjusters, admins) the data and tooling to
process those claims. Beyond the core policy/claims/payment workflow, the
platform layers on two automated capabilities:

- **Automated fraud scoring** — every claim filed is scored in the
  background against policy coverage and the customer's open-claims history,
  and flagged for review when it crosses a risk threshold.
- **AI-assisted workflows** — a support chatbot for customers, a policy
  recommendation engine, claim document/photo analysis, a natural-language
  claim filing assistant, renewal/premium insight generation, and an
  adjuster-facing claim-history Q&A tool, all backed by the Anthropic API.

The application is built as a demo that is largely functional end-to-end
(real auth, real database-backed policies/claims/payments, a real second
microservice for fraud detection, real AI features) while intentionally
simplifying a handful of things — such as running the core domains as one
Spring Boot service rather than several, and defaulting payments to an
instant-mock mode when no payment gateway credentials are supplied — so it
can be run locally with a single command.

## About the Application Stack

**Frontend**
- React (via Vite) with Tailwind CSS
- Talks to the backend and AI service over REST (`src/api/client.js`,
  `src/api/aiClient.js`)
- Includes a floating chat widget (`ChatWidget.jsx`) wired to the AI service

**Backend (`backend/`)**
- Java 21 / Spring Boot
- Owns authentication (JWT + Redis-backed refresh tokens, double-submit
  cookie CSRF protection), Policies, Claims, and Payments
- PostgreSQL for persistence, Redis for refresh-token storage
- Publishes `claim.submitted` events to Kafka when a claim is filed
- Integrates with Razorpay for real payment processing (Orders API +
  signature-verified webhook), falling back to instant-mock payments when no
  Razorpay credentials are configured

**Fraud Detection Service (`fraud-detection-service/`)**
- Independent Java 21 / Spring Boot microservice with its own `pom.xml`,
  container, and Kafka consumer group
- Consumes `claim.submitted` events, scores claims using a rule-based engine
  (amount-to-coverage ratio, open-claims count), writes the risk score back
  onto the claim, and publishes `claim.flagged` when a threshold is crossed

**AI Service (`ai-service/`)**
- Separate Java service that wraps the Anthropic API for all AI-driven
  features (claim triage summaries, support chat, recommendations, document
  analysis, claim-filing assistant, renewal insight, adjuster Q&A)
- Runs in a labeled fallback mode when no `ANTHROPIC_API_KEY` is set, so the
  rest of the stack is never blocked on having a key

**Infrastructure**
- PostgreSQL — primary datastore
- Redis — refresh-token storage
- Kafka — event backbone between the backend and the fraud-detection service
- Docker Compose for local orchestration; a full Kubernetes manifest set
  (`k8s/`) for cluster deployment, including namespace, configmap, secrets,
  Deployments/Services for all four app images, HPAs, a deny-by-default
  NetworkPolicy, and an Ingress with TLS

## About the Application Architecture

The system is organized as four independently deployable services sitting
behind a shared data and eventing layer:

```
                     ┌─────────────┐
                     │  Frontend   │
                     │ (React/Vite)│
                     └──────┬──────┘
                            │ REST (JWT + CSRF)
              ┌─────────────┼───────────────┐
              ▼                             ▼
      ┌───────────────┐             ┌───────────────┐
      │    Backend    │             │  AI Service   │
      │ (Spring Boot) │             │ (Spring Boot) │
      │ Auth / Policy │             │  Anthropic-   │
      │ Claims/Payment│             │ backed features│
      └───┬───────┬───┘             └───────┬───────┘
          │       │                          │
          │       │ claim.submitted          │ reads claim/policy
          │       ▼ (Kafka)                  ▼ records for context
          │  ┌─────────────────────┐   ┌───────────┐
          │  │ Fraud Detection Svc │──▶│ Postgres  │
          │  │  (Spring Boot)      │   │           │
          │  │ scores + writes back│   └───────────┘
          │  │ risk, emits         │
          │  │ claim.flagged       │
          │  └─────────────────────┘
          ▼
     ┌───────────┐   ┌─────────┐
     │ Postgres  │   │  Redis  │
     └───────────┘   └─────────┘
```

- **Request path:** the frontend authenticates against the backend using
  JWTs with a Redis-backed refresh token and double-submit cookie CSRF
  protection, then calls Policy/Claims/Payment REST endpoints directly.
- **Event path:** filing a claim publishes a `claim.submitted` event to
  Kafka. The fraud-detection service consumes it independently, writes a
  risk score back onto the claim row, and emits `claim.flagged` for
  high-risk claims — which the AI service consumes in turn to generate a
  triage note for adjusters.
- **AI path:** the AI service is called directly by the frontend for
  synchronous features (chat, recommendations, document analysis, claim
  assistant, renewal insight, adjuster Q&A) and via Kafka for the
  asynchronous triage-summary feature.
- **Deployment shape:** each of the four services (frontend, backend,
  fraud-detection-service, ai-service) has its own Dockerfile and, for
  Kubernetes, its own Deployment/Service manifest, so any service can be
  scaled or redeployed independently — the `k8s/` manifests include HPAs on
  the backend and fraud-detection-service specifically because those absorb
  traffic spikes.
- **Network boundaries:** a deny-by-default NetworkPolicy restricts traffic
  to only the paths services actually need, and an Ingress with TLS is the
  single public entry point.
