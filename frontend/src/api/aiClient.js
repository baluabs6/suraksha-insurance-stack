import axios from "axios";

const AI_API_BASE_URL = import.meta.env.VITE_AI_API_BASE_URL || "http://localhost:8082";

// ai-service validates the same access_token cookie the backend issues, so requests must send credentials
// (and ai-service allows this origin via app.cors.allowed-origin).
const aiApi = axios.create({ baseURL: AI_API_BASE_URL, withCredentials: true });

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

// Forget the server-side history for one support-chat conversation ("New chat").
export function clearChatConversation(conversationId) {
  return aiApi.delete(`/api/ai/chat/${encodeURIComponent(conversationId)}`);
}

// Non-streaming chat. language: "en", "hi", "te", "ta", "kn", "ml", "mr", "bn" or "gu".
export function sendChat({ message, conversationId, language }) {
  return aiApi.post("/api/ai/chat", { message, conversationId, language }).then((res) => res.data.reply);
}

function parseSseEvent(raw) {
  let event = "message";
  const data = [];
  for (const line of raw.split("\n")) {
    if (line.startsWith("event:")) event = line.slice(6).trim();
    else if (line.startsWith("data:")) data.push(line.slice(5));
  }
  return { event, data: data.join("\n") };
}

// Streaming chat (English only). Calls onToken(text) as text arrives and resolves with the final, guard-checked
// text from the "done" event; the caller should show that text instead of what it streamed.
export async function streamChat({ message, conversationId }, onToken) {
  const res = await fetch(`${AI_API_BASE_URL}/api/ai/chat/stream`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
    body: JSON.stringify({ message, conversationId, language: "en" }),
  });
  if (!res.ok || !res.body) {
    const err = new Error("Chat stream failed");
    err.status = res.status;
    throw err;
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let finalText = null;

  for (;;) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split("\n\n");
    buffer = events.pop();
    for (const raw of events) {
      const { event, data } = parseSseEvent(raw);
      if (!data) continue;
      const text = JSON.parse(data);
      if (event === "token") onToken?.(text);
      else if (event === "done") finalText = text;
    }
  }
  return finalText;
}
