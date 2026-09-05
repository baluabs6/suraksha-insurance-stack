# Suraksha Insurance — full stack demo

A working insurance platform: React frontend, Spring Boot REST API, PostgreSQL,
Redis, all containerized. This implements the architecture designed earlier in
this conversation — JWT auth with a Redis-backed refresh token, double-submit
cookie CSRF protection, and the Policy / Claims / Payment domains.

## What's real vs. simplified

**Real and working:**
- Registration, login, JWT issuance, refresh, logout
- Double-submit cookie CSRF protection between frontend and backend
- Policies, claims (with validation), and payments backed by PostgreSQL
- Redis-backed refresh tokens (revocable on logout)
- Role field on users (CUSTOMER / AGENT / CLAIMS_ADJUSTER / ADMIN), ready for RBAC on new endpoints
- **Async fraud scoring**: filing a claim publishes a `claim.submitted` Kafka
  event; a separate `fraud-detection-service` consumes it, scores the claim
  against policy coverage and open-claims signals, writes the risk score back
  onto the claim, and publishes `claim.flagged` if it crosses a threshold.
  This is a real second microservice — its own `pom.xml`, own container, own
  Kafka consumer group — deployed and scaled independently of the main
  backend, exactly as designed in the architecture doc.
- **Seven AI features, in a fourth service (`ai-service`)**, each calling the
  real Anthropic API:
  - **Claim triage summaries** — consumes `claim.flagged` and writes a short,
    factual internal note for adjusters (shown on the dashboard as "AI triage
    note"). Only runs for flagged claims, not every claim.
  - **Support chatbot** — floating chat widget on every authenticated page,
    answers general policy/claims questions using the customer's own policy
    summaries as context. Won't speculate on a specific claim's outcome — the
    system prompt explicitly forbids that.
  - **Policy recommendation engine** — rule-based gap analysis (what plan
    types the customer *doesn't* have) shown on the Policies page, with the
    LLM writing only the one-sentence pitch on top of a gap the rules already
    found — so recommendations stay deterministic even without an API key.
  - **Claim document/photo analysis** (`POST /api/ai/documents/analyze`) —
    takes a base64 photo attached to a claim (damage photo, bill, estimate)
    and Claude's vision input to write a short, factual description an
    adjuster can skim, and note whether it's consistent with the claimant's
    own description. Never estimates cost or comments on legitimacy.
  - **Natural-language claim filing assistant**
    (`POST /api/ai/claim-assistant/extract`) — customer describes an incident
    in plain English; the model returns a draft (matched policy, incident
    date, amount, a cleaned description, and clarifying questions for
    whatever's missing) to prefill the claim form. The customer still reviews
    and edits every field before the existing `POST /api/claims` call fires —
    this endpoint never files anything itself.
  - **Renewal/premium insight** (`POST /api/ai/renewal-insight`) — same
    deterministic-factors-plus-LLM-narrative pattern as the recommendation
    engine: rule-based factors (days to renewal, claims count, risk flags)
    are computed first, and the model only writes the plain-language
    explanation on top of them — it never invents a new premium figure.
  - **Adjuster claim-history Q&A** (`POST /api/ai/adjuster/query`) — an
    adjuster asks a question in plain English, scoped to a policy or a risk
    level (or the 25 most recent claims if unscoped); the model answers only
    from the retrieved claim records and is told to say so when the records
    don't support an answer, rather than guess.

**Simplified for a demo (call these out before production use):**
- `backend` itself is still one Spring Boot service, not split into separate
  auth/policy/claims/payment microservices — the package structure mirrors
  the intended boundaries, so splitting later is mostly moving packages into
  their own deployable apps.
- **Payments now use a real Razorpay integration** — order creation via the
  Orders API, checkout handed off to Razorpay's modal, and confirmation via
  a signature-verified webhook (`POST /api/payments/webhook`), never the
  client-side callback. Without `RAZORPAY_KEY_ID`/`RAZORPAY_KEY_SECRET` set,
  it falls back to instant-mock mode (marks the payment PAID immediately) so
  the demo still runs without a merchant account.
- Fraud scoring is rule-based (amount vs. coverage ratio, open-claims count),
  not a trained ML model — `FraudScoringService` is where you'd swap in a
  real model's inference call.
- `fraud-detection-service` reads/writes the `claims`/`policies` tables
  directly rather than calling the backend's API — acceptable for a shared
  Postgres instance today, but the honest long-term fix is to either give it
  its own database populated via events, or have it call back through the
  backend's API instead of touching its tables.
- The new claim-assistant/document-analysis/renewal-insight/adjuster-query
  endpoints have no dedicated frontend UI yet — `frontend/src/api/aiClient.js`
  has thin wrapper functions for all four (`analyzeClaimDocument`,
  `extractClaimFromNarrative`, `getRenewalInsight`, `askAdjuster`) ready to
  wire into components, same call pattern as the existing chat widget.
- The adjuster Q&A endpoint has no role check in this demo — see the note in
  `AdjusterQueryController` for what a production setup needs.
- `JWT_SECRET` and DB passwords in `docker-compose.yml` are placeholders —
  replace them and use a secrets manager for anything beyond local dev.
- `ai-service`'s chat and recommendation endpoints are **not behind auth** in
  this demo — the frontend passes policy context directly, and anyone who can
  reach port 8082 can call them. Fine for local exploration, not for a real
  deployment: put this service behind the API gateway with the same JWT
  validation as the others before exposing it publicly.
- Without an `ANTHROPIC_API_KEY`, all three AI features still run — they
  return clearly-labeled fallback text instead of failing, so the rest of the
  stack isn't blocked on having a key.

## Run it

Requires Docker and Docker Compose.

To enable the real AI features (otherwise they run in fallback mode — see
above), export your Anthropic API key before starting the stack:

```bash
export ANTHROPIC_API_KEY=sk-ant-...
```

To enable real payments instead of instant-mock mode, also set your Razorpay
test-mode credentials (from the Razorpay dashboard → Settings → API Keys):

```bash
export RAZORPAY_KEY_ID=rzp_test_...
export RAZORPAY_KEY_SECRET=...
export RAZORPAY_WEBHOOK_SECRET=...   # set when configuring the webhook below
```

Then:

```bash
docker compose up --build
```

Without any of these set, everything else works identically — AI features
return fallback text, and "Pay premium" marks the payment PAID instantly
instead of opening a real checkout.

**To actually receive webhook confirmations**, Razorpay's servers need to
reach your backend over the public internet, which `localhost:8080` isn't.
For local testing, tunnel it (e.g. `ngrok http 8080`) and register
`https://<your-tunnel>/api/payments/webhook` in the Razorpay dashboard under
Settings → Webhooks, subscribed to `payment.captured` and `payment.failed`,
using the same secret you set as `RAZORPAY_WEBHOOK_SECRET`.

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Fraud-detection service: http://localhost:8081 (no public endpoints — it only consumes/produces Kafka events and updates the database)
- AI service: http://localhost:8082 (`/api/ai/chat`, `/api/ai/recommendations`,
  `/api/ai/documents/analyze`, `/api/ai/claim-assistant/extract`,
  `/api/ai/renewal-insight`, `/api/ai/adjuster/query`; triage summaries run
  via Kafka, no endpoint to call directly)
- Kafka broker: localhost:9092

Demo login: `demo@suraksha.in` / `Demo@1234` (seeded automatically on first startup).

To see the fraud flow in action: log in, file a claim for close to the full
coverage amount of a policy, then check the claim again a few seconds later
(refresh the dashboard) — `riskScore` and `riskLevel` will be populated once
the fraud-detection-service has consumed and scored it. Watch it happen live
with `docker compose logs -f fraud-detection-service`.

Startup order note: `fraud-detection-service` waits for `backend` to start,
but not for it to finish creating the `risk_score`/`risk_level` columns on
first boot. If it logs a schema error on a completely fresh `docker compose up`,
restart just that service: `docker compose restart fraud-detection-service`.

## Run without Docker (local dev)

**Backend** (needs Java 21, Maven, a running PostgreSQL and Redis):
```bash
cd backend
mvn spring-boot:run
```

**Frontend** (needs Node 20+):
```bash
cd frontend
npm install
npm run dev
```

## API summary

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | /api/auth/register | none | Create account |
| POST | /api/auth/login | none | Log in, sets auth cookies |
| POST | /api/auth/refresh | refresh cookie | Get a new access token |
| POST | /api/auth/logout | any | Revoke refresh token, clear cookies |
| GET | /api/auth/me | required | Current user profile |
| GET | /api/policies | required | List your policies |
| GET | /api/policies/{id} | required | One policy |
| GET | /api/claims | required | List your claims |
| POST | /api/claims | required | File a claim |
| GET | /api/payments | required | Payment history |
| POST | /api/payments/{policyId}/pay | required | Pay premium (mocked) |

## Kubernetes deployment

The `k8s/` folder has manifests for everything above, matching the
architecture designed earlier: one Deployment + Service per microservice,
HPAs on the two services that need to absorb traffic spikes (backend,
fraud-detection-service), a deny-by-default NetworkPolicy set that only opens
the specific paths services actually need, and an Ingress with TLS.

**Before applying anything:**
1. Build and push the four images (`backend`, `fraud-detection-service`,
   `ai-service`, `frontend`) to a registry your cluster can pull from, and
   update the `image:` field in each manifest — they currently point at
   `suraksha/*:latest`, which only works if you've built those tags locally
   and your cluster can see your local image cache (e.g. `kind load
   docker-image` or Minikube's Docker daemon).
2. Fill in `02-secrets.yaml` with real values — every field is a placeholder.
   Prefer `kubectl create secret generic suraksha-secrets --from-literal=...`
   or an External Secrets Operator over editing the YAML directly.
3. Point the Ingress hosts in `40-ingress.yaml` at your actual domains and
   have cert-manager (or similar) issue the `suraksha-tls` certificate.

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-configmap.yaml
kubectl apply -f k8s/02-secrets.yaml
kubectl apply -f k8s/10-postgres.yaml -f k8s/11-redis.yaml -f k8s/12-kafka.yaml
kubectl apply -f k8s/20-backend.yaml -f k8s/21-fraud-detection.yaml -f k8s/22-ai-service.yaml -f k8s/23-frontend.yaml
kubectl apply -f k8s/30-network-policy.yaml
kubectl apply -f k8s/40-ingress.yaml
```

(Or just `kubectl apply -f k8s/` — the numeric prefixes keep apply order
sane even though Kubernetes doesn't strictly require it.)

**What's genuinely production-shaped here:** zero-downtime rolling updates on
`backend`, resource requests/limits on every container, readiness/liveness
probes wired to real Actuator endpoints, non-root containers, and a
NetworkPolicy that actually restricts traffic instead of leaving the mesh
flat.

**What's still a placeholder:** in-cluster Postgres/Redis/Kafka (swap for
managed services — RDS/ElastiCache/MSK — before real traffic), no
Prometheus/Grafana/Alertmanager installed (the annotations and `/actuator/prometheus`
endpoint are ready for a Prometheus Operator `ServiceMonitor` to pick up),
and no Helm chart yet — these are raw manifests, worth templating once you
have more than one environment (staging/prod) to manage.

## Next steps toward production

1. Split `auth` / `policy` / `claims` / `payment` packages into separate
   Spring Boot services (each with its own `pom.xml` and Dockerfile) once
   they need to scale or deploy independently — `fraud-detection-service`
   and `ai-service` are working examples of this pattern already.
2. Replace the rule-based `FraudScoringService` with a trained model
   (scikit-learn/XGBoost served via a small Python inference endpoint, or
   ONNX runtime in-process) — the Kafka plumbing around it doesn't change.
3. Give `fraud-detection-service` and `ai-service` their own datastores
   instead of reading the backend's tables directly, or have them call back
   through the backend's API.
4. Add idempotency handling on the webhook (Razorpay can retry a delivery —
   the handler currently re-applies the same status update harmlessly, but a
   production version should record processed event IDs explicitly).
5. Move secrets into Vault or your cloud provider's secrets manager instead
   of the `k8s/02-secrets.yaml` template.
6. Install Prometheus Operator + Grafana + Alertmanager and add
   `ServiceMonitor` resources targeting the `/actuator/prometheus` endpoints
   already exposed — including a consumer-lag alert on `fraud-detection-service`
   and `ai-service`'s Kafka groups.
7. Turn `k8s/` into a Helm chart once you have more than one environment
   (staging/prod) to template values for.
8. Put `ai-service` behind the same gateway-level JWT validation as the
   other services before exposing it publicly.
