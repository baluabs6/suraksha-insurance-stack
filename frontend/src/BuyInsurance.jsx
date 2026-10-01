import React, { useEffect, useMemo, useState } from "react";
import api from "./api/client.js";
import ProductClaimFields from "./ProductClaimFields.jsx";
import { planIcon, useProducts, validateProductDetails, cleanDetails } from "./products.js";

const inr = (n) => `₹${Number(n).toLocaleString("en-IN")}`;
const input = "w-full border border-[#E4E1D8] rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#16303F]";

/** Pick a type, answer its quote form, see a priced breakdown, then buy. The server recomputes the price on purchase. */
export default function BuyInsurance({ onPurchased }) {
  const products = useProducts();
  const [forms, setForms] = useState(null);
  const [type, setType] = useState("");
  const [coverage, setCoverage] = useState("");
  const [inputs, setInputs] = useState({});
  const [startDate, setStartDate] = useState("");
  const [errors, setErrors] = useState({});
  const [quote, setQuote] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    api.get("/api/quotes/forms").then(({ data }) => setForms(data)).catch(() => setMessage("Couldn't load the quote forms. Please try again."));
  }, []);

  const product = useMemo(() => products.find((p) => p.type === type), [products, type]);
  const spec = forms && type ? forms[type] : null;

  const reset = () => { setCoverage(""); setInputs({}); setStartDate(""); setErrors({}); setQuote(null); setMessage(""); };
  const choose = (t) => { setType(t); reset(); };
  const changeInputs = (v) => { setInputs(v); setQuote(null); };

  const payload = () => ({
    type,
    coverageAmount: spec.coverageFromField ? undefined : Number(coverage),
    inputs: cleanDetails(inputs),
    startDate: startDate || undefined,
  });

  const validate = () => {
    const errs = validateProductDetails({ claimFields: spec.fields }, inputs);
    if (!spec.coverageFromField && !coverage) errs.coverage = `Choose a ${spec.coverageLabel.toLowerCase()}.`;
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const getQuote = async (e) => {
    e.preventDefault();
    setMessage("");
    if (!validate()) return;
    setBusy(true);
    try {
      const { data } = await api.post("/api/quotes", payload());
      setQuote(data);
    } catch (err) {
      setQuote(null);
      setMessage(err?.response?.data?.message || "Couldn't get a quote. Please check your answers and try again.");
    } finally {
      setBusy(false);
    }
  };

  const buy = async () => {
    setBusy(true);
    setMessage("");
    try {
      const { data } = await api.post("/api/policies/purchase", payload());
      onPurchased(data.policy);
    } catch (err) {
      setMessage(err?.response?.data?.message || "Couldn't complete the purchase. Please try again.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="max-w-2xl">
      <h1 className="font-serif text-2xl text-[#16303F]">Buy insurance</h1>
      <p className="text-sm text-[#4B5563] mt-1">Pick a cover, answer a few questions and see the price before you pay.</p>

      <div className="grid grid-cols-2 md:grid-cols-3 gap-3 mt-6">
        {products.map((p) => {
          const Icon = planIcon(p.type);
          return (
            <button key={p.type} onClick={() => choose(p.type)} type="button"
              className={`text-left bg-white border rounded-lg p-4 ${type === p.type ? "border-[#16303F] ring-1 ring-[#16303F]" : "border-[#E4E1D8]"}`}>
              <Icon className="w-5 h-5 text-[#B8863C]" strokeWidth={1.5} />
              <div className="text-sm text-[#16303F] mt-2">{p.label}</div>
              <div className="text-xs text-[#4B5563] mt-0.5">{p.fromPrice.startsWith("₹") ? `From ${p.fromPrice}` : p.fromPrice}</div>
            </button>
          );
        })}
      </div>

      {product && spec && (
        <form onSubmit={getQuote} className="bg-white border border-[#E4E1D8] rounded-lg p-6 mt-6 space-y-5">
          <div>
            <div className="font-serif text-lg text-[#16303F]">{product.planName}</div>
            <p className="text-xs text-[#4B5563] mt-1">{product.tagline}</p>
          </div>

          {!spec.coverageFromField && (
            <div>
              <label className="text-sm text-[#16303F] block mb-1">{spec.coverageLabel} <span className="text-[#B0463D]">*</span></label>
              <select className={input} value={coverage} onChange={(e) => { setCoverage(e.target.value); setQuote(null); }}>
                <option value="">Select</option>
                {spec.coverageOptions.map((o) => <option key={o} value={o}>{inr(o)}</option>)}
              </select>
              {errors.coverage && <p className="text-xs text-[#B0463D] mt-1">{errors.coverage}</p>}
            </div>
          )}

          <ProductClaimFields product={{ label: product.label, claimFields: spec.fields }} values={inputs}
            onChange={changeInputs} errors={errors} heading="About you and what you're insuring" />

          <div>
            <label className="text-sm text-[#16303F] block mb-1">Start date (optional)</label>
            <input type="date" className={input} value={startDate} onChange={(e) => { setStartDate(e.target.value); setQuote(null); }} />
            <p className="text-xs text-[#4B5563] mt-1">Leave empty to start today. You can start up to 60 days ahead.</p>
          </div>

          <button type="submit" disabled={busy} className="w-full bg-[#16303F] text-white text-sm py-2.5 rounded hover:bg-[#1F4B4F] disabled:opacity-60">
            {busy && !quote ? "Calculating…" : "Get quote"}
          </button>
        </form>
      )}

      {message && <p className="text-sm text-[#B0463D] mt-4">{message}</p>}

      {quote && (
        <div className="bg-white border border-[#B8863C] rounded-lg p-6 mt-6">
          <div className="text-xs uppercase tracking-wide text-[#4B5563]">Your quote</div>
          <div className="mt-3 space-y-1.5 text-sm">
            {quote.breakdown.map((l, i) => (
              <div key={i} className="flex justify-between gap-4 text-[#4B5563]">
                <span>{l.label}</span><span className="text-[#16303F]">{Number(l.amount) < 0 ? "−" : ""}{inr(Math.abs(Number(l.amount)))}</span>
              </div>
            ))}
          </div>
          <div className="flex justify-between items-baseline border-t border-[#E4E1D8] mt-4 pt-4">
            <span className="text-sm text-[#16303F]">Premium {quote.premiumBasis}</span>
            <span className="font-serif text-2xl text-[#16303F]">{inr(quote.premium)}</span>
          </div>
          <div className="text-xs text-[#4B5563] mt-2">
            Cover of {inr(quote.coverageAmount)} from {quote.startDate} to {quote.endDate}. Prices exclude any applicable taxes and are illustrative.
          </div>
          <button onClick={buy} disabled={busy} className="mt-5 w-full bg-[#16303F] text-white text-sm py-2.5 rounded hover:bg-[#1F4B4F] disabled:opacity-60">
            {busy ? "Working…" : "Buy and continue to payment"}
          </button>
          <p className="text-xs text-[#4B5563] mt-2">Your cover starts once the payment is confirmed.</p>
        </div>
      )}
    </div>
  );
}
