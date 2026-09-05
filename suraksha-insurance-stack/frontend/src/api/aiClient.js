import axios from "axios";

const AI_API_BASE_URL = import.meta.env.VITE_AI_API_BASE_URL || "http://localhost:8082";

// ai-service isn't behind the same auth as the main backend in this demo —
// see the comment in ChatController/RecommendationController for what a
// production setup needs (gateway-level JWT validation, no client-supplied context).
const aiApi = axios.create({ baseURL: AI_API_BASE_URL });

export default aiApi;

// --- Newer AI features (same no-auth-yet, client-supplied-context pattern
// as the chat/recommendations calls above — see the comment there) ---

// imageBase64 must be raw base64 (no "data:image/...;base64," prefix).
export function analyzeClaimDocument({ imageBase64, mediaType, claimType, claimantDescription }) {
  return aiApi
    .post("/api/ai/documents/analyze", { imageBase64, mediaType, claimType, claimantDescription })
    .then((res) => res.data);
}

// policies: [{ id, type, policyNumber, planName }]
export function extractClaimFromNarrative({ narrative, policies }) {
  return aiApi
    .post("/api/ai/claim-assistant/extract", { narrative, policies })
    .then((res) => res.data);
}

export function getRenewalInsight(payload) {
  return aiApi.post("/api/ai/renewal-insight", payload).then((res) => res.data);
}

// scope: { policyId } or { riskLevel } — omit both for the 25 most recent claims.
export function askAdjuster({ question, policyId, riskLevel }) {
  return aiApi
    .post("/api/ai/adjuster/query", { question, policyId, riskLevel })
    .then((res) => res.data);
}
