import React, { useState } from "react";
import api from "./api/client.js";

const inr = (n) => `₹${Number(n).toLocaleString("en-IN")}`;
const RELATIONSHIPS = ["Spouse", "Child", "Parent", "Sibling", "Other"];
const input = "w-full border border-[#E4E1D8] rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#16303F]";

/** After-sale actions on one policy card: cancel with a refund preview, and (life policies) set the nominee. */
export default function PolicyServicing({ policy, onChanged }) {
  const [panel, setPanel] = useState(null); // "cancel" | "nominee" | null
  const [refund, setRefund] = useState(null);
  const [nominee, setNominee] = useState({ nomineeName: policy.attributes?.nomineeName || "", nomineeRelationship: policy.attributes?.nomineeRelationship || "" });
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");

  const inForce = policy.status === "ACTIVE" || policy.status === "PENDING_PAYMENT";
  if (!inForce) {
    return policy.status === "CANCELLED" && policy.refundAmount != null
      ? <div className="mt-3 text-xs text-[#4B5563]">Cancelled. Refund due: {inr(policy.refundAmount)}.</div>
      : null;
  }

  const error = (err, fallback) => setMessage(err?.response?.data?.message || fallback);

  const openCancel = async () => {
    setPanel("cancel"); setMessage(""); setRefund(null);
    try {
      const { data } = await api.get(`/api/policies/${policy.id}/cancellation-quote`);
      setRefund(data);
    } catch (err) { error(err, "Couldn't work out the refund."); }
  };

  const confirmCancel = async () => {
    setBusy(true); setMessage("");
    try {
      const { data } = await api.post(`/api/policies/${policy.id}/cancel`);
      setMessage(data.message);
      setPanel(null);
      onChanged();
    } catch (err) { error(err, "Couldn't cancel the policy."); }
    finally { setBusy(false); }
  };

  const saveNominee = async (e) => {
    e.preventDefault();
    setBusy(true); setMessage("");
    try {
      await api.patch(`/api/policies/${policy.id}/nominee`, nominee);
      setPanel(null);
      onChanged();
    } catch (err) {
      const fieldErrors = err?.response?.data?.errors;
      error(err, Array.isArray(fieldErrors) ? fieldErrors.join(" ") : "Couldn't save the nominee.");
    } finally { setBusy(false); }
  };

  return (
    <div className="mt-3 pt-3 border-t border-[#E4E1D8] text-xs">
      <div className="flex gap-4">
        {policy.type === "LIFE" && <button onClick={() => { setPanel(panel === "nominee" ? null : "nominee"); setMessage(""); }} className="text-[#16303F] underline">Nominee</button>}
        <button onClick={panel === "cancel" ? () => setPanel(null) : openCancel} className="text-[#B0463D] underline">Cancel policy</button>
      </div>

      {panel === "cancel" && (
        <div className="mt-3 bg-[#FAF9F5] border border-[#E4E1D8] rounded p-3 space-y-2">
          {refund ? (
            <>
              <p className="text-[#4B5563]">{refund.note}</p>
              <p className="text-[#16303F]">Refund due: {inr(refund.refund)}</p>
              <button onClick={confirmCancel} disabled={busy} className="bg-[#B0463D] text-white px-3 py-1.5 rounded disabled:opacity-60">
                {busy ? "Cancelling…" : "Yes, cancel this policy"}
              </button>
            </>
          ) : !message && <p className="text-[#4B5563]">Working out your refund…</p>}
        </div>
      )}

      {panel === "nominee" && (
        <form onSubmit={saveNominee} className="mt-3 space-y-3">
          <input className={input} placeholder="Nominee's full name" value={nominee.nomineeName}
            onChange={(e) => setNominee({ ...nominee, nomineeName: e.target.value })} maxLength={100} />
          <select className={input} value={nominee.nomineeRelationship}
            onChange={(e) => setNominee({ ...nominee, nomineeRelationship: e.target.value })}>
            <option value="">Relationship to you</option>
            {RELATIONSHIPS.map((r) => <option key={r} value={r}>{r}</option>)}
          </select>
          <button type="submit" disabled={busy} className="bg-[#16303F] text-white px-3 py-1.5 rounded disabled:opacity-60">{busy ? "Saving…" : "Save nominee"}</button>
        </form>
      )}

      {message && <p className="mt-2 text-[#B0463D]">{message}</p>}
    </div>
  );
}
