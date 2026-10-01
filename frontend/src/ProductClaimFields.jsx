import React from "react";
import { isFieldRequired } from "./products.js";

const input = "w-full border border-[#E4E1D8] rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#16303F]";
const label = "text-sm text-[#16303F] block mb-1";

/** Renders the claim form for any insurance type from the backend's field definitions. */
export default function ProductClaimFields({ product, values, onChange, errors, heading }) {
  if (!product || product.claimFields.length === 0) return null;
  const set = (key, value) => onChange({ ...values, [key]: value });

  return (
    <div className="space-y-5 pt-1">
      <div className="text-xs uppercase tracking-wide text-[#4B5563]">{heading || `${product.label} claim details`}</div>
      {product.claimFields.map((f) => {
        const required = isFieldRequired(f, values);
        const error = errors[`detail_${f.key}`];
        const id = `detail-${f.key}`;
        return (
          <div key={f.key}>
            {f.type === "boolean" ? (
              <label className="flex items-center gap-2 text-sm text-[#16303F]">
                <input type="checkbox" checked={values[f.key] === true} onChange={(e) => set(f.key, e.target.checked)} />
                {f.label}
              </label>
            ) : (
              <>
                <label htmlFor={id} className={label}>{f.label}{required && <span className="text-[#B0463D]"> *</span>}</label>
                {f.type === "select" ? (
                  <select id={id} className={input} value={values[f.key] || ""} onChange={(e) => set(f.key, e.target.value)}>
                    <option value="">Select</option>
                    {f.options.map((o) => <option key={o} value={o}>{o}</option>)}
                  </select>
                ) : (
                  <input id={id} className={input} value={values[f.key] || ""} maxLength={300}
                    type={f.type === "date" ? "date" : f.type === "number" ? "number" : "text"}
                    min={f.type === "number" ? 0 : undefined}
                    onChange={(e) => set(f.key, e.target.value)} />
                )}
              </>
            )}
            {f.help && <p className="text-xs text-[#4B5563] mt-1">{f.help}</p>}
            {error && <p className="text-xs text-[#B0463D] mt-1">{error}</p>}
          </div>
        );
      })}
    </div>
  );
}

export function DocumentChecklist({ product }) {
  if (!product || !product.requiredDocuments?.length) return null;
  return (
    <div className="bg-[#FAF9F5] border border-[#E4E1D8] rounded p-4 text-xs text-[#4B5563]">
      <div className="text-[#16303F] text-sm mb-2">Documents you'll usually need</div>
      <ul className="list-disc pl-4 space-y-1">
        {product.requiredDocuments.map((d) => <li key={d}>{d}</li>)}
      </ul>
    </div>
  );
}
