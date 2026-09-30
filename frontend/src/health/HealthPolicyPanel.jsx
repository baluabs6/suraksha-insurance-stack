import React, { useEffect, useState } from "react";
import { Users, Trash2, Plus } from "lucide-react";
import api from "../api/client.js";
import { rupees } from "./format.js";

const RELATIONSHIPS = [
  ["SELF", "Myself"], ["SPOUSE", "Spouse"], ["CHILD", "Child"], ["PARENT", "Parent"], ["OTHER", "Other"],
];
const relLabel = (r) => (RELATIONSHIPS.find(([v]) => v === r) || [r, r])[1];

function CoverageMeter({ coverage }) {
  const total = Number(coverage.sumInsured) || 1;
  const pct = (n) => `${Math.min(100, (Number(n) / total) * 100)}%`;
  return (
    <div>
      <div className="flex justify-between text-sm">
        <span className="text-[#4B5563]">Sum insured remaining</span>
        <span className="text-[#16303F] font-medium">{rupees(coverage.remaining)} of {rupees(coverage.sumInsured)}</span>
      </div>
      <div className="mt-2 h-2 rounded-full bg-[#F1EFE8] overflow-hidden flex" role="img"
        aria-label={`${rupees(coverage.used)} used, ${rupees(coverage.pending)} in pending claims`}>
        <div className="bg-[#16303F]" style={{ width: pct(coverage.used) }} />
        <div className="bg-[#B8863C]" style={{ width: pct(coverage.pending) }} />
      </div>
      <div className="flex gap-4 mt-2 text-xs text-[#4B5563]">
        <span><span className="inline-block w-2 h-2 rounded-full bg-[#16303F] mr-1" />Used {rupees(coverage.used)}</span>
        <span><span className="inline-block w-2 h-2 rounded-full bg-[#B8863C] mr-1" />In review {rupees(coverage.pending)}</span>
      </div>
      <ul className="mt-3 text-xs text-[#4B5563] space-y-1">
        <li>Room rent limit: {coverage.roomRentCapPerDay ? `${rupees(coverage.roomRentCapPerDay)} per day` : "no limit"}</li>
        <li>Co-payment: {coverage.coPayPercent}%</li>
        <li>Initial waiting period: {coverage.initialWaitingDays} days (accidents excluded)</li>
        <li>Pre-existing conditions: {coverage.preExistingWaitingMonths}-month waiting period</li>
      </ul>
    </div>
  );
}

const EMPTY = { fullName: "", dateOfBirth: "", relationship: "SELF", preExistingConditions: "" };

export default function HealthPolicyPanel({ policy }) {
  const [coverage, setCoverage] = useState(null);
  const [members, setMembers] = useState([]);
  const [adding, setAdding] = useState(false);
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const load = () => {
    api.get(`/api/policies/${policy.id}/coverage`).then(({ data }) => setCoverage(data)).catch(() => setCoverage(null));
    api.get(`/api/policies/${policy.id}/members`).then(({ data }) => setMembers(data)).catch(() => setMembers([]));
  };
  useEffect(load, [policy.id]);

  const addMember = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.fullName.trim() || !form.dateOfBirth) {
      setError("Enter the member's name and date of birth.");
      return;
    }
    setBusy(true);
    try {
      await api.post(`/api/policies/${policy.id}/members`, form);
      setForm(EMPTY);
      setAdding(false);
      load();
    } catch (err) {
      setError(err?.response?.data?.message || err?.response?.data?.errors?.[0] || "Couldn't add the member.");
    } finally {
      setBusy(false);
    }
  };

  const removeMember = async (m) => {
    if (!window.confirm(`Remove ${m.fullName} from this policy?`)) return;
    try {
      await api.delete(`/api/policies/${policy.id}/members/${m.id}`);
      load();
    } catch (err) {
      setError(err?.response?.data?.message || "Couldn't remove the member.");
    }
  };

  const input = "w-full border border-[#E4E1D8] rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#16303F]";

  return (
    <div className="mt-4 pt-4 border-t border-[#E4E1D8] space-y-5">
      {coverage && <CoverageMeter coverage={coverage} />}

      <div>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-sm text-[#16303F]"><Users className="w-4 h-4" strokeWidth={1.5} /> Insured members</div>
          {!adding && members.length < 6 && (
            <button onClick={() => setAdding(true)} className="text-xs text-[#16303F] flex items-center gap-1 hover:underline">
              <Plus className="w-3 h-3" /> Add member
            </button>
          )}
        </div>
        <ul className="mt-2 divide-y divide-[#E4E1D8]">
          {members.map((m) => (
            <li key={m.id} className="py-2 flex items-start justify-between text-sm">
              <div>
                <div className="text-[#16303F]">{m.fullName} <span className="text-xs text-[#4B5563]">· {relLabel(m.relationship)}</span></div>
                <div className="text-xs text-[#4B5563]">Born {m.dateOfBirth}{m.preExistingConditions ? ` · Declared: ${m.preExistingConditions}` : ""}</div>
              </div>
              <button onClick={() => removeMember(m)} aria-label={`Remove ${m.fullName}`} className="text-[#4B5563] hover:text-[#B0463D]">
                <Trash2 className="w-4 h-4" strokeWidth={1.5} />
              </button>
            </li>
          ))}
          {members.length === 0 && <li className="py-2 text-xs text-[#4B5563]">No members added yet. Add the people covered so claims can be filed for them.</li>}
        </ul>

        {adding && (
          <form onSubmit={addMember} className="mt-3 space-y-3 bg-[#FAF9F5] border border-[#E4E1D8] rounded p-3">
            <input className={input} placeholder="Full name" value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
            <div className="grid grid-cols-2 gap-3">
              <input type="date" className={input} value={form.dateOfBirth} aria-label="Date of birth"
                onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })} />
              <select className={input} value={form.relationship} aria-label="Relationship"
                onChange={(e) => setForm({ ...form, relationship: e.target.value })}>
                {RELATIONSHIPS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
              </select>
            </div>
            <input className={input} placeholder="Pre-existing conditions declared (optional, comma separated)"
              value={form.preExistingConditions} maxLength={500}
              onChange={(e) => setForm({ ...form, preExistingConditions: e.target.value })} />
            <div className="flex gap-2">
              <button type="submit" disabled={busy} className="text-sm px-3 py-1.5 rounded bg-[#16303F] text-white disabled:opacity-60">{busy ? "Saving…" : "Save member"}</button>
              <button type="button" onClick={() => { setAdding(false); setError(""); }} className="text-sm px-3 py-1.5 rounded border border-[#E4E1D8] text-[#4B5563]">Cancel</button>
            </div>
          </form>
        )}
        {error && <p className="text-xs text-[#B0463D] mt-2">{error}</p>}
      </div>
    </div>
  );
}
