import React, { useEffect, useState } from "react";
import api from "../api/client.js";
import { BILL_FIELDS, billTotal, rupees } from "./format.js";

const input = "w-full border border-[#E4E1D8] rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#16303F]";
const label = "text-sm text-[#16303F] block mb-1";
const err = (e) => e && <p className="text-xs text-[#B0463D] mt-1">{e}</p>;

export default function HealthClaimFields({ policyId, values, onChange, errors }) {
  const [members, setMembers] = useState(null);

  useEffect(() => {
    setMembers(null);
    api.get(`/api/policies/${policyId}/members`).then(({ data }) => setMembers(data)).catch(() => setMembers([]));
  }, [policyId]);

  const set = (patch) => onChange({ ...values, ...patch });

  return (
    <div className="space-y-5">
      <div>
        <label className={label}>Who was treated?</label>
        <select className={input} value={values.memberId} onChange={(e) => set({ memberId: e.target.value })}>
          <option value="">Select an insured member</option>
          {(members || []).map((m) => <option key={m.id} value={m.id}>{m.fullName}</option>)}
        </select>
        {members && members.length === 0 && (
          <p className="text-xs text-[#8A6412] mt-1">No members on this policy yet — add them from My policies first.</p>
        )}
        {err(errors.memberId)}
      </div>

      <div>
        <label className={label}>Hospital</label>
        <input className={input} value={values.hospitalName} maxLength={200} onChange={(e) => set({ hospitalName: e.target.value })} />
        {err(errors.hospitalName)}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className={label}>Admission date</label>
          <input type="date" className={input} value={values.admissionDate} onChange={(e) => set({ admissionDate: e.target.value })} />
          {err(errors.admissionDate)}
        </div>
        <div>
          <label className={label}>Discharge date</label>
          <input type="date" className={input} value={values.dischargeDate} onChange={(e) => set({ dischargeDate: e.target.value })} />
          {err(errors.dischargeDate)}
        </div>
      </div>

      <div>
        <label className={label}>Diagnosis / reason for admission</label>
        <input className={input} value={values.diagnosis} maxLength={300} onChange={(e) => set({ diagnosis: e.target.value })} />
        {err(errors.diagnosis)}
      </div>

      <div>
        <label className={label}>Treating doctor (optional)</label>
        <input className={input} value={values.treatingDoctor} maxLength={120} onChange={(e) => set({ treatingDoctor: e.target.value })} />
      </div>

      <label className="flex items-center gap-2 text-sm text-[#16303F]">
        <input type="checkbox" checked={values.accidental} onChange={(e) => set({ accidental: e.target.checked })} />
        The admission was due to an accident
      </label>

      <fieldset>
        <legend className={label}>Itemised bill (₹)</legend>
        <div className="space-y-3">
          {BILL_FIELDS.map(([key, text]) => (
            <div key={key} className="flex items-center gap-3">
              <span className="text-sm text-[#4B5563] flex-1">{text}</span>
              <input type="number" min="0" className={`${input} max-w-[9rem]`} value={values[key]} aria-label={text}
                onChange={(e) => set({ [key]: e.target.value })} />
            </div>
          ))}
        </div>
        <div className="flex justify-between mt-3 pt-3 border-t border-[#E4E1D8] text-sm">
          <span className="text-[#4B5563]">Total claimed</span>
          <span className="text-[#16303F] font-medium">{rupees(billTotal(values))}</span>
        </div>
        {err(errors.amount)}
      </fieldset>
    </div>
  );
}
