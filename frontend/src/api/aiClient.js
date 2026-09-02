import axios from "axios";

const AI_API_BASE_URL = import.meta.env.VITE_AI_API_BASE_URL || "http://localhost:8082";

// ai-service isn't behind the same auth as the main backend in this demo —
// see the comment in ChatController/RecommendationController for what a
// production setup needs (gateway-level JWT validation, no client-supplied context).
const aiApi = axios.create({ baseURL: AI_API_BASE_URL });

export default aiApi;
