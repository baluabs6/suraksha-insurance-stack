# Suraksha Insurance — Feature Walkthrough & User Guide

This document walks through **every feature in the application**, step by
step, exactly as a customer, adjuster, or admin would experience it — and
explains *why* each one matters to the end user. For the technical
architecture, endpoints, and deployment instructions, see `README.md`; this
file is the "what does a person actually do, and what do they get out of
it" companion to that.

---

## 1. Account & Login

### What it does
Registration, login, session refresh, and logout, backed by JWT access
tokens and a Redis-backed refresh token — with CSRF protection on top so a
malicious site can't ride the customer's cookies to make requests on their
behalf.

### Step by step
1. Open the app at `http://localhost:5173` and click **Register**.
2. Enter full name, email, and password. The account is created with the
   `CUSTOMER` role by default.
3. Log in with the same credentials (or use the seeded demo account:
   `demo@suraksha.in` / `Demo@1234`).
4. The app sets secure, httpOnly auth cookies — nothing sensitive touches
   browser `localStorage`.
5. Sessions refresh silently in the background; logging out revokes the
   refresh token server-side, so a stolen cookie stops working immediately
   after logout instead of staying valid until it expires.

### Why it helps the end user
A customer never has to think about any of this — they just stay logged in
across visits without re-entering a password constantly, and they can trust
that logging out on a shared or public computer actually ends the session
rather than leaving it usable by the next person.

---

## 2. Dashboard

### What it does
A single landing page after login that summarizes the customer's policies,
recent claims (with status and risk info once scored), and payment history.

### Step by step
1. After login, the dashboard loads automatically.
2. Policies are shown as cards (type, plan name, coverage, premium, renewal
   date).
3. Claims show their current status (`SUBMITTED` → `UNDER_REVIEW` →
   `APPROVED`/`REJECTED` → `SETTLED`), and — once the fraud-detection
   service has scored them — a risk level badge and, for flagged claims, an
   **AI triage note** (see §7).

### Why it helps the end user
One screen answers "where do things stand with my insurance right now?"
instead of making the customer dig through separate pages for policies,
claims, and payments.

---

## 3. Policies

### What it does
Lists the policies the logged-in customer owns: health, motor, or life,
each with coverage amount, premium, and the active policy period.

### Step by step
1. Go to the **Policies** page.
2. See each policy's plan name, type, coverage amount, premium, and
   start/end dates.
3. Click into a policy for its full detail view.

### Why it helps the end user
Coverage details, premium amounts, and renewal dates are the numbers people
actually need when deciding whether to file a claim or budget for a
renewal — having them in one place avoids digging through paper policy
documents or calling support for basic facts.

---

## 4. Filing a Claim (standard form)

### What it does
Lets a customer file a claim against one of their policies, which is
validated, saved, and immediately queued for asynchronous fraud scoring.

### Step by step
1. From a policy or the Claims page, click **File a Claim**.
2. Select the policy, enter the claim amount, incident date, and a
   description of what happened.
3. Submit. The claim is created with status `SUBMITTED` and appears on the
   dashboard right away.
4. Behind the scenes, a `claim.submitted` event goes to Kafka, and
   `fraud-detection-service` scores it within a few seconds — refreshing
   the dashboard will show a `riskScore` and `riskLevel` once that
   finishes.

### Why it helps the end user
Filing doesn't block on a human reviewer being available — the customer
gets instant confirmation their claim was received, and the risk scoring
happens in the background so it doesn't slow down submission.

---

## 5. Payments (premium payment)

### What it does
Lets a customer pay a policy's premium — either through a real Razorpay
checkout (if the deployment has Razorpay credentials configured) or an
instant-mock "paid" flow for local demos.

### Step by step
1. From a policy card, click **Pay Premium**.
2. If Razorpay is configured: a checkout modal opens, the customer pays,
   and the payment is marked `PAID` only after Razorpay's signed webhook
   confirms it server-side (never from the browser's say-so).
3. If Razorpay isn't configured: the payment is marked `PAID` instantly so
   the rest of the demo still works.
4. The **Payments** page lists payment history with amount, method, and
   status.

### Why it helps the end user
The customer gets a real, familiar checkout experience (when configured)
instead of an insurer-specific payment form, and the server-side-only
confirmation means a flaky connection or closed browser tab can't leave
them wrongly charged or wrongly marked unpaid.

---

## 6. Async Fraud Scoring (background — not a page the user visits)

### What it does
Every submitted claim is automatically scored by `fraud-detection-service`
using rule-based signals: how large the claim is relative to the policy's
coverage, how many other open claims exist on the same policy, and a couple
of weaker heuristics. It writes a `riskScore` (0–1), a `riskLevel`
(LOW/MEDIUM/HIGH), and — for HIGH risk — publishes a `claim.flagged` event
that triggers the AI triage summary in §7.

### Step by step (what the customer sees, even though they don't trigger this directly)
1. File a claim close to the full coverage amount of a policy.
2. Wait a few seconds, then refresh the dashboard.
3. The claim now shows a risk level badge.

### Why it helps the end user
This runs on every claim without the customer waiting on it — claims that
are clearly low-risk can move through review faster because a human
adjuster isn't spending time confirming what a simple rule already ruled
out, and claims that do need scrutiny get flagged consistently instead of
depending on which adjuster happens to review them.

---

## 7. AI Claim Triage Summaries *(AI feature #1)*

### What it does
When a claim is flagged HIGH risk, `ai-service` consumes the
`claim.flagged` event and asks Claude to write a short, factual internal
note summarizing the claim and what to check — shown on the dashboard as
"AI triage note." It never recommends approving or denying anything.

### Step by step
1. File a claim large enough to get flagged (see §6).
2. Wait a few seconds after the risk badge appears.
3. Refresh the dashboard — the claim now shows an "AI triage note" alongside
   the risk badge.

### Why it helps the end user
Indirectly, but meaningfully: a flagged claim gets a same-second briefing
note instead of sitting in an adjuster's queue until they have time to
reconstruct the context themselves — which means flagged claims (including
legitimate ones that simply tripped a threshold) get reviewed and resolved
faster instead of stalling.

---

## 8. Support Chatbot *(AI feature #2)*

### What it does
A floating chat widget available on every authenticated page. It answers
general questions about how policies, claims, and payments work, using the
customer's own policy summaries as context — but it's explicitly forbidden
from speculating about whether a specific claim will be approved or
quoting a number it wasn't given.

### Step by step
1. Click the chat bubble in the corner of any page.
2. Ask a question — e.g. "how long does a motor claim usually take to
   review?" or "what's covered under my health plan?"
3. The assistant answers using the customer's actual policy context where
   relevant, and points them to support or the claims section for anything
   it shouldn't guess at (e.g. "will my claim be approved?").

### Why it helps the end user
Most policy questions are simple enough not to need a phone call or a
support ticket — the chatbot resolves those instantly, at any hour, while
still being honest about the things it genuinely can't answer instead of
making something up.

---

## 9. Policy Recommendation Engine *(AI feature #3)*

### What it does
Looks at what policy types (health/motor/life) the customer *doesn't* have
yet — a plain rule, not an LLM guess — and shows a card for each gap with a
one-sentence, LLM-written pitch explaining why that plan might be worth
considering.

### Step by step
1. Go to the **Policies** page.
2. Below the customer's existing policies, see recommendation cards for any
   plan types they don't currently hold.
3. Each card names a specific plan and a one-line reason it might be
   relevant.

### Why it helps the end user
The gap analysis itself is deterministic (never invented by the model), so
the customer is never shown a recommendation based on a hallucinated gap —
only real coverage they don't have. The AI's only job is making the pitch
readable instead of a dry feature list.

---

## 10. Claim Document/Photo Analysis *(AI feature #4)*

### What it does
A customer can attach a photo to a claim (vehicle damage, a medical bill, a
repair estimate) and get back a short, factual description of what's in the
image, plus a note on whether it looks consistent with their written
description — using Claude's image understanding. It never estimates cost
or comments on whether the claim looks legitimate.

### Step by step
1. While filing or viewing a claim, attach a photo.
2. The photo (as base64) and the claim's type/description are sent to
   `POST /api/ai/documents/analyze`.
3. Get back a short description, e.g. "The photo shows a dented rear bumper
   and cracked tail light, consistent with the claimant's description of a
   parking-lot collision."

### Why it helps the end user
The customer gets immediate confirmation their photo evidence actually
shows what they think it shows — catching a blurry or wrong photo before
submission instead of after an adjuster has already been waiting on it, and
adjusters get a written summary they can skim instead of opening every
image manually.

---

## 11. Natural-Language Claim Filing Assistant *(AI feature #5)*

### What it does
Instead of mapping their own story onto form fields, a customer can
describe what happened in plain English, and the assistant drafts the claim
form for them: which policy it's probably against, the incident date, an
estimated amount, a cleaned-up description, and a list of anything it
couldn't figure out (which becomes a clarifying question instead of a
guess). The customer still reviews and edits every field before anything is
actually submitted.

### Step by step
1. On the claim form, choose "describe it instead" (or similar entry
   point).
2. Type something like: *"My car got hit while parked outside my building
   yesterday, the bumper and tail light are damaged, looks like maybe
   ₹15,000 worth of repair."*
3. Submit the narrative to `POST /api/ai/claim-assistant/extract`.
4. The form pre-fills with the matched policy, incident date, amount, and a
   cleaned description — with any missing pieces called out explicitly
   (e.g. "What time did the incident happen?").
5. Review, correct anything, and submit normally through the existing claim
   flow.

### Why it helps the end user
Insurance forms ask for information in a structure that doesn't match how
people naturally describe an incident. This lets the customer say it the
way they'd tell a friend, and removes the friction of remembering exactly
which field means what — while never taking away their ability to review
and correct the draft before it's real.

---

## 12. Renewal / Premium Insight *(AI feature #6)*

### What it does
For a policy nearing renewal, this pulls a few deterministic facts (days
until renewal, how many claims were filed this term, how many were flagged
at what risk level, current coverage) and asks Claude to explain them in
plain language — without ever inventing a new premium number it wasn't
given.

### Step by step
1. On a policy nearing its end date, click "Why is my renewal like this?"
   (or similar).
2. The frontend sends the policy's own data plus its claims-history counts
   to `POST /api/ai/renewal-insight`.
3. Get back a short paragraph, e.g. explaining that a clean claims history
   this term typically supports stable renewal terms, or that one flagged
   claim is worth being aware of.

### Why it helps the end user
Renewal terms can feel opaque — this gives the customer an actual,
specific explanation grounded in their own claims history instead of a
generic "your premium may change at renewal" notice, without ever
promising a number the system doesn't actually have.

---

## 13. Adjuster Claim-History Q&A *(AI feature #7)*

### What it does
Lets a claims adjuster ask a plain-English question over a scoped slice of
claim history — by policy, by risk level, or the 25 most recent claims —
and get an answer generated strictly from those retrieved records. The
model is explicitly told to say "I don't have enough information" rather
than guess beyond what it was given.

### Step by step (adjuster/staff-facing, not customer-facing)
1. An adjuster opens the claim review tooling and enters a question, e.g.
   *"Has this policy had prior claims, and were any flagged?"*
2. They scope it to a policy ID or a risk level.
3. The question, plus the matching claim records, go to
   `POST /api/ai/adjuster/query`.
4. The answer cites specific claim IDs and sticks to what's in those
   records.

### Why it helps the end user
This one helps customers indirectly but concretely: adjusters spend less
time manually cross-referencing claim history record by record, which
means claims — including the customer's own — move through review and get
resolved faster, with decisions grounded in an actual, auditable read of
the data rather than a rushed skim.

---

## Summary — who benefits from what

| Feature | Direct user | What they actually get |
|---|---|---|
| Account & Login | Customer | Secure, low-friction access to their own account |
| Dashboard | Customer | One place to see everything at a glance |
| Policies | Customer | Clear view of coverage, premium, renewal dates |
| Filing a Claim | Customer | Fast, simple claim submission |
| Payments | Customer | Trustworthy premium payment, real or mocked |
| Fraud Scoring | Customer (indirect) | Faster review for low-risk claims |
| Claim Triage Summaries | Adjuster → Customer (indirect) | Flagged claims get reviewed faster |
| Support Chatbot | Customer | Instant answers to common questions, 24/7 |
| Policy Recommendations | Customer | Honest, relevant coverage suggestions |
| Document/Photo Analysis | Customer + Adjuster | Confidence evidence was captured correctly |
| Claim Filing Assistant | Customer | File a claim in their own words, not a form's |
| Renewal Insight | Customer | A real explanation instead of a vague notice |
| Adjuster Q&A | Adjuster → Customer (indirect) | Faster, more consistent claim decisions |

Every AI feature above is designed to fail safely: if `ANTHROPIC_API_KEY`
isn't configured, each one returns a clearly-labeled fallback response
instead of breaking the surrounding feature — so the rest of the platform
keeps working even without a live model connection.
