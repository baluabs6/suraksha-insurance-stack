import React, { useState } from "react";
import { MessageCircle, X, Send, Sparkles, RotateCcw } from "lucide-react";
import aiApi, { clearChatConversation } from "./api/aiClient.js";

const GREETING = { role: "assistant", text: "Hi! Ask me anything about how your policies, claims, or payments work." };

// One id per conversation. The server keeps the history (Spring AI chat memory, per signed-in user),
// so the browser only has to send this id plus the new message.
const newConversationId = () =>
  (typeof crypto !== "undefined" && crypto.randomUUID)
    ? crypto.randomUUID()
    : `c${Date.now().toString(36)}${Math.random().toString(36).slice(2, 10)}`;

export default function ChatWidget() {
  const [open, setOpen] = useState(false);
  const [conversationId, setConversationId] = useState(newConversationId);
  const [messages, setMessages] = useState([GREETING]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);

  // Start over: forget the stored history on the server, then begin a fresh conversation.
  const newChat = async () => {
    const old = conversationId;
    setConversationId(newConversationId());
    setMessages([GREETING]);
    try { await clearChatConversation(old); } catch { /* best effort */ }
  };

  const send = async (e) => {
    e.preventDefault();
    const text = input.trim();
    if (!text) return;
    setMessages((m) => [...m, { role: "user", text }]);
    setInput("");
    setSending(true);
    try {
      const { data } = await aiApi.post("/api/ai/chat", { message: text, conversationId });
      setMessages((m) => [...m, { role: "assistant", text: data.reply }]);
    } catch {
      setMessages((m) => [...m, { role: "assistant", text: "Sorry, I couldn't reach the assistant right now." }]);
    } finally {
      setSending(false);
    }
  };

  if (!open) {
    return (
      <button
        onClick={() => setOpen(true)}
        className="fixed bottom-6 right-6 w-12 h-12 rounded-full bg-[#16303F] text-white flex items-center justify-center shadow-lg hover:bg-[#1F4B4F] transition-colors z-30"
        aria-label="Open support chat"
      >
        <MessageCircle className="w-5 h-5" />
      </button>
    );
  }

  return (
    <div className="fixed bottom-6 right-6 w-80 max-w-[calc(100vw-3rem)] bg-white border border-[#E4E1D8] rounded-lg shadow-xl flex flex-col z-30" style={{ height: 420 }}>
      <div className="flex items-center justify-between px-4 py-3 border-b border-[#E4E1D8]">
        <div className="flex items-center gap-2 text-sm text-[#16303F]">
          <Sparkles className="w-4 h-4 text-[#B8863C]" /> Suraksha assistant
        </div>
        <div className="flex items-center gap-3">
          <button onClick={newChat} aria-label="Start a new chat" title="New chat"><RotateCcw className="w-4 h-4 text-[#4B5563]" /></button>
          <button onClick={() => setOpen(false)} aria-label="Close chat"><X className="w-4 h-4 text-[#4B5563]" /></button>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
        {messages.map((m, i) => (
          <div key={i} className={`text-sm ${m.role === "user" ? "text-right" : ""}`}>
            <span className={`inline-block px-3 py-2 rounded-lg max-w-[85%] ${m.role === "user" ? "bg-[#16303F] text-white" : "bg-[#F5F4EF] text-[#16303F]"}`}>
              {m.text}
            </span>
          </div>
        ))}
        {sending && <div className="text-sm text-[#4B5563]">Thinking…</div>}
      </div>
      <form onSubmit={send} className="flex items-center gap-2 border-t border-[#E4E1D8] p-3">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask a question…"
          className="flex-1 text-sm border border-[#E4E1D8] rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#16303F]"
        />
        <button type="submit" disabled={sending} className="w-9 h-9 rounded bg-[#16303F] text-white flex items-center justify-center disabled:opacity-60">
          <Send className="w-4 h-4" />
        </button>
      </form>
    </div>
  );
}
