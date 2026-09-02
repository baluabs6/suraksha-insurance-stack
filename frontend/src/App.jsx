import React, { useEffect, useState } from "react";
import {
  Shield, FileText, CreditCard, User, LogOut, ChevronRight,
  CheckCircle2, Menu, AlertCircle, Clock, TrendingUp, HeartPulse, Car, Umbrella,
} from "lucide-react";
import api from "./api/client.js";
import aiApi from "./api/aiClient.js";
import ChatWidget from "./ChatWidget.jsx";

const planIcon = (type) => (type === "HEALTH" ? HeartPulse : type === "MOTOR" ? Car : Umbrella);

const statusStyle = (status) => {
  const good = ["ACTIVE", "PAID", "SETTLED", "APPROVED", "LOW"];
  const pending = ["UNDER_REVIEW", "SUBMITTED", "PENDING", "MEDIUM"];
  const risky = ["HIGH"];
  if (good.includes(status)) return "bg-[#E7F0EA] text-[#2E6E52]";
  if (pending.includes(status)) return "bg-[#FBF1DF] text-[#8A6412]";
  if (risky.includes(status)) return "bg-[#F7E6E3] text-[#B0463D]";
  return "bg-[#F1EFE8] text-[#4B5563]";
};

const inr = (n) => `₹${Number(n).toLocaleString("en-IN")}`;

function Nav({ onLogin }) {
  return (
    <header className="border-b border-[#E4E1D8]">
      <div className="max-w-6xl mx-auto px-6 py-5 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Shield className="w-5 h-5 text-[#16303F]" strokeWidth={1.75} />
          <span className="font-serif text-lg text-[#16303F]">Suraksha</span>
        </div>
        <nav className="hidden md:flex items-center gap-8 text-sm text-[#4B5563]">
          <span>Plans</span>
          <span>Claims</span>
          <span>About</span>
        </nav>
        <button onClick={onLogin} className="text-sm px-4 py-2 rounded border border-[#16303F] text-[#16303F] hover:bg-[#16303F] hover:text-white transition-colors">
          Log in
        </button>
      </div>
    </header>
  );
}

function Landing({ onLogin }) {
  const plans = [
    { type: "HEALTH", name: "CarePlus Family", from: "₹649/month", line: "Covers the whole family under one policy, no sub-limits on room rent." },
    { type: "MOTOR", name: "DriveSecure Comprehensive", from: "₹399/month", line: "Zero depreciation cover with a 60-minute garage cashless promise." },
    { type: "LIFE", name: "LifeShield Term 30", from: "₹899/month", line: "₹50 lakh cover with a decision on your application within 48 hours." },
  ];
  return (
    <div className="bg-[#F5F4EF] min-h-screen">
      <Nav onLogin={onLogin} />
      <section className="max-w-6xl mx-auto px-6 pt-16 pb-14">
        <div className="max-w-2xl">
          <h1 className="font-serif text-4xl md:text-5xl leading-tight text-[#16303F]">
            Coverage that shows up when you need it.
          </h1>
          <p className="mt-5 text-lg text-[#4B5563] max-w-lg">
            File a claim, track it, and get paid without chasing an agent.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <button onClick={onLogin} className="px-5 py-3 rounded bg-[#16303F] text-white text-sm hover:bg-[#1F4B4F] transition-colors">
              Get a quote
            </button>
            <button onClick={onLogin} className="px-5 py-3 rounded border border-[#16303F] text-[#16303F] text-sm hover:bg-white transition-colors">
              Track a claim
            </button>
          </div>
        </div>
      </section>
      <section className="max-w-6xl mx-auto px-6 py-16">
        <h2 className="font-serif text-2xl text-[#16303F] mb-8">Plans people actually keep</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {plans.map((p) => {
            const Icon = planIcon(p.type);
            return (
              <div key={p.name} className="bg-white border border-[#E4E1D8] rounded-lg p-6">
                <Icon className="w-6 h-6 text-[#B8863C]" strokeWidth={1.5} />
                <h3 className="font-serif text-lg text-[#16303F] mt-4">{p.name}</h3>
                <p className="text-sm text-[#4B5563] mt-2 leading-relaxed">{p.line}</p>
                <div className="mt-5 flex items-center justify-between">
                  <span className="text-sm text-[#16303F]">From {p.from}</span>
                  <button onClick={onLogin} className="text-sm text-[#16303F] flex items-center gap-1">
                    View plan <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </section>
      <footer className="border-t border-[#E4E1D8]">
        <div className="max-w-6xl mx-auto px-6 py-8 text-sm text-[#4B5563] flex flex-wrap items-center justify-between gap-3">
          <span>Demo login: demo@suraksha.in / Demo@1234</span>
          <span>IRDAI registration: demo-000000</span>
        </div>
      </footer>
    </div>
  );
}

function LoginPage({ onBack, onLoggedIn }) {
  const [email, setEmail] = useState("demo@suraksha.in");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    if (!email.trim() || !password.trim()) {
      setError("Enter both your email and password to continue.");
      return;
    }
    setLoading(true);
    try {
      const { data: user } = await api.post("/api/auth/login", { email, password });
      onLoggedIn(user);
    } catch (err) {
      setError(err?.response?.data?.message || "Couldn't log in. Check the API is running.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F5F4EF] flex items-center justify-center px-6">
      <div className="w-full max-w-sm">
        <button onClick={onBack} className="text-sm text-[#4B5563] mb-6">← Back to home</button>
        <div className="bg-white border border-[#E4E1D8] rounded-lg p-8">
          <div className="flex items-center gap-2 mb-6">
            <Shield className="w-5 h-5 text-[#16303F]" strokeWidth={1.75} />
            <span className="font-serif text-lg text-[#16303F]">Suraksha</span>
          </div>
          <h1 className="font-serif text-xl text-[#16303F] mb-1">Log in to your account</h1>
          <p className="text-sm text-[#4B5563] mb-6">Demo account is pre-filled — just add the password.</p>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="text-sm text-[#16303F] block mb-1">Email</label>
              <input type="text" value={email} onChange={(e) => setEmail(e.target.value)}
                className="w-full border border-[#E4E1D8] rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#16303F]" />
            </div>
            <div>
              <label className="text-sm text-[#16303F] block mb-1">Password</label>
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                placeholder="Demo@1234"
                className="w-full border border-[#E4E1D8] rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#16303F]" />
            </div>
            {error && (
              <div className="flex items-center gap-2 text-sm text-[#B0463D]">
                <AlertCircle className="w-4 h-4" /> {error}
              </div>
            )}
            <button type="submit" disabled={loading} className="w-full bg-[#16303F] text-white text-sm py-2.5 rounded hover:bg-[#1F4B4F] transition-colors disabled:opacity-60">
              {loading ? "Logging in…" : "Log in"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

function Sidebar({ active, setActive, onLogout, mobileOpen, setMobileOpen }) {
  const items = [
    { key: "dashboard", label: "Dashboard", icon: TrendingUp },
    { key: "policies", label: "My policies", icon: Shield },
    { key: "claim", label: "File a claim", icon: FileText },
    { key: "payments", label: "Payments", icon: CreditCard },
    { key: "profile", label: "Profile", icon: User },
  ];
  const content = (
    <div className="flex flex-col h-full">
      <div className="flex items-center gap-2 px-6 py-5 border-b border-[#E4E1D8]">
        <Shield className="w-5 h-5 text-[#16303F]" strokeWidth={1.75} />
        <span className="font-serif text-lg text-[#16303F]">Suraksha</span>
      </div>
      <nav className="flex-1 py-4">
        {items.map(({ key, label, icon: Icon }) => (
          <button key={key} onClick={() => { setActive(key); setMobileOpen(false); }}
            className={`w-full flex items-center gap-3 px-6 py-2.5 text-sm text-left ${active === key ? "bg-[#EDEBE2] text-[#16303F]" : "text-[#4B5563] hover:bg-[#F5F4EF]"}`}>
            <Icon className="w-4 h-4" strokeWidth={1.75} />
            {label}
          </button>
        ))}
      </nav>
      <button onClick={onLogout} className="flex items-center gap-3 px-6 py-4 text-sm text-[#4B5563] border-t border-[#E4E1D8]">
        <LogOut className="w-4 h-4" strokeWidth={1.75} /> Log out
      </button>
    </div>
  );
  return (
    <>
      <div className="hidden md:flex md:w-60 md:flex-col border-r border-[#E4E1D8] bg-white shrink-0">{content}</div>
      {mobileOpen && (
        <div className="fixed inset-0 z-20 md:hidden">
          <div className="absolute inset-0 bg-black/30" onClick={() => setMobileOpen(false)} />
          <div className="absolute left-0 top-0 bottom-0 w-64 bg-white">{content}</div>
        </div>
      )}
    </>
  );
}

function Dashboard({ userName, policies, claims }) {
  const openClaims = claims.filter((c) => c.status !== "SETTLED").length;
  return (
    <div>
      <h1 className="font-serif text-2xl text-[#16303F]">Welcome back, {userName?.split(" ")[0]}</h1>
      <p className="text-sm text-[#4B5563] mt-1">Here's where things stand across your policies.</p>
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-6">
        <div className="bg-white border border-[#E4E1D8] rounded-lg p-5">
          <div className="text-sm text-[#4B5563]">Active policies</div>
          <div className="font-serif text-2xl text-[#16303F] mt-1">{policies.length}</div>
        </div>
        <div className="bg-white border border-[#E4E1D8] rounded-lg p-5">
          <div className="text-sm text-[#4B5563]">Open claims</div>
          <div className="font-serif text-2xl text-[#16303F] mt-1">{openClaims}</div>
        </div>
        <div className="bg-white border border-[#E4E1D8] rounded-lg p-5">
          <div className="text-sm text-[#4B5563]">Policies on file</div>
          <div className="font-serif text-2xl text-[#16303F] mt-1">{policies.length}</div>
        </div>
      </div>
      <div className="mt-8">
        <h2 className="font-serif text-lg text-[#16303F] mb-3">Recent claims</h2>
        <div className="bg-white border border-[#E4E1D8] rounded-lg divide-y divide-[#E4E1D8]">
          {claims.length === 0 && <div className="px-5 py-4 text-sm text-[#4B5563]">No claims filed yet.</div>}
          {claims.map((c) => (
            <div key={c.id} className="px-5 py-4">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-sm text-[#16303F]">{c.policy?.policyNumber}</div>
                  <div className="text-xs text-[#4B5563] mt-0.5">{inr(c.claimAmount)}</div>
                </div>
                <div className="flex items-center gap-2">
                  {c.riskLevel && (
                    <span className={`text-xs px-2.5 py-1 rounded-full ${statusStyle(c.riskLevel)}`}>{c.riskLevel} risk</span>
                  )}
                  <span className={`text-xs px-2.5 py-1 rounded-full ${statusStyle(c.status)}`}>{c.status.replace("_", " ")}</span>
                </div>
              </div>
              {c.aiSummary && (
                <p className="text-xs text-[#4B5563] mt-2 bg-[#F5F4EF] rounded p-2 leading-relaxed">
                  <span className="text-[#16303F]">AI triage note: </span>{c.aiSummary}
                </p>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function Policies({ policies }) {
  const [recommendations, setRecommendations] = useState([]);
  const [loadingRecs, setLoadingRecs] = useState(true);

  useEffect(() => {
    const ownedTypes = policies.map((p) => p.type);
    aiApi.post("/api/ai/recommendations", { ownedTypes })
      .then(({ data }) => setRecommendations(data))
      .catch(() => setRecommendations([]))
      .finally(() => setLoadingRecs(false));
  }, [policies]);

  return (
    <div>
      <h1 className="font-serif text-2xl text-[#16303F]">My policies</h1>
      <p className="text-sm text-[#4B5563] mt-1">Everything you're currently covered for.</p>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
        {policies.map((p) => {
          const Icon = planIcon(p.type);
          return (
            <div key={p.id} className="bg-white border border-[#E4E1D8] rounded-lg p-5">
              <div className="flex items-start justify-between">
                <Icon className="w-5 h-5 text-[#B8863C]" strokeWidth={1.5} />
                <span className={`text-xs px-2.5 py-1 rounded-full ${statusStyle(p.status)}`}>{p.status}</span>
              </div>
              <h3 className="font-serif text-base text-[#16303F] mt-3">{p.planName}</h3>
              <div className="text-xs text-[#4B5563] mt-0.5">{p.policyNumber}</div>
              <div className="mt-4 text-sm text-[#4B5563] space-y-1">
                <div className="flex justify-between"><span>Coverage</span><span className="text-[#16303F]">{inr(p.coverageAmount)}</span></div>
                <div className="flex justify-between"><span>Premium</span><span className="text-[#16303F]">{inr(p.premium)}/yr</span></div>
                <div className="flex justify-between"><span>Ends</span><span className="text-[#16303F]">{p.endDate}</span></div>
              </div>
            </div>
          );
        })}
      </div>

      {!loadingRecs && recommendations.length > 0 && (
        <div className="mt-10">
          <h2 className="font-serif text-lg text-[#16303F] mb-3">Recommended for you</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {recommendations.map((r) => {
              const Icon = planIcon(r.type);
              return (
                <div key={r.type} className="bg-white border border-dashed border-[#B8863C] rounded-lg p-5">
                  <Icon className="w-5 h-5 text-[#B8863C]" strokeWidth={1.5} />
                  <h3 className="font-serif text-base text-[#16303F] mt-3">{r.planName}</h3>
                  <p className="text-sm text-[#4B5563] mt-2">{r.reason}</p>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

function FileClaim({ policies, onFiled }) {
  const [policyId, setPolicyId] = useState("");
  const [amount, setAmount] = useState("");
  const [incidentDate, setIncidentDate] = useState("");
  const [description, setDescription] = useState("");
  const [errors, setErrors] = useState({});
  const [confirmed, setConfirmed] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = {};
    if (!policyId) errs.policyId = "Select which policy this claim is against.";
    if (!amount || Number(amount) <= 0) errs.amount = "Enter the amount you're claiming.";
    if (!incidentDate) errs.incidentDate = "Enter when the incident happened.";
    if (!description.trim()) errs.description = "Give a short description of what happened.";
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;

    setSubmitting(true);
    try {
      const { data: claim } = await api.post("/api/claims", {
        policyId, claimAmount: Number(amount), incidentDate, description,
      });
      setConfirmed(claim);
      onFiled(claim);
    } catch (err) {
      setErrors({ form: err?.response?.data?.message || "Couldn't submit the claim. Try again." });
    } finally {
      setSubmitting(false);
    }
  };

  if (confirmed) {
    return (
      <div className="max-w-md">
        <div className="bg-white border border-[#E4E1D8] rounded-lg p-8 text-center">
          <CheckCircle2 className="w-10 h-10 text-[#2E6E52] mx-auto" strokeWidth={1.5} />
          <h2 className="font-serif text-xl text-[#16303F] mt-4">Claim submitted</h2>
          <p className="text-sm text-[#4B5563] mt-2">
            Reference <span className="text-[#16303F]">{confirmed.id?.slice(0, 8).toUpperCase()}</span>. We'll notify you once it's reviewed.
          </p>
          <button onClick={() => { setConfirmed(null); setPolicyId(""); setAmount(""); setIncidentDate(""); setDescription(""); }}
            className="mt-6 text-sm px-4 py-2 rounded border border-[#16303F] text-[#16303F]">
            File another claim
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-xl">
      <h1 className="font-serif text-2xl text-[#16303F]">File a claim</h1>
      <p className="text-sm text-[#4B5563] mt-1">Have your policy number and details ready.</p>
      <form onSubmit={handleSubmit} className="mt-6 bg-white border border-[#E4E1D8] rounded-lg p-6 space-y-5">
        <div>
          <label className="text-sm text-[#16303F] block mb-1">Policy</label>
          <select value={policyId} onChange={(e) => setPolicyId(e.target.value)}
            className="w-full border border-[#E4E1D8] rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#16303F]">
            <option value="">Select a policy</option>
            {policies.map((p) => (<option key={p.id} value={p.id}>{p.policyNumber} — {p.planName}</option>))}
          </select>
          {errors.policyId && <p className="text-xs text-[#B0463D] mt-1">{errors.policyId}</p>}
        </div>
        <div>
          <label className="text-sm text-[#16303F] block mb-1">Amount you're claiming (₹)</label>
          <input type="number" value={amount} onChange={(e) => setAmount(e.target.value)}
            className="w-full border border-[#E4E1D8] rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#16303F]" placeholder="45000" />
          {errors.amount && <p className="text-xs text-[#B0463D] mt-1">{errors.amount}</p>}
        </div>
        <div>
          <label className="text-sm text-[#16303F] block mb-1">Date of incident</label>
          <input type="date" value={incidentDate} onChange={(e) => setIncidentDate(e.target.value)}
            className="w-full border border-[#E4E1D8] rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#16303F]" />
          {errors.incidentDate && <p className="text-xs text-[#B0463D] mt-1">{errors.incidentDate}</p>}
        </div>
        <div>
          <label className="text-sm text-[#16303F] block mb-1">What happened</label>
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3}
            className="w-full border border-[#E4E1D8] rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#16303F]" placeholder="Briefly describe the incident" />
          {errors.description && <p className="text-xs text-[#B0463D] mt-1">{errors.description}</p>}
        </div>
        {errors.form && (
          <div className="flex items-center gap-2 text-sm text-[#B0463D]"><AlertCircle className="w-4 h-4" /> {errors.form}</div>
        )}
        <button type="submit" disabled={submitting} className="w-full bg-[#16303F] text-white text-sm py-2.5 rounded hover:bg-[#1F4B4F] transition-colors disabled:opacity-60">
          {submitting ? "Submitting…" : "Submit claim"}
        </button>
      </form>
    </div>
  );
}

function Payments({ payments, policies, onPay }) {
  const [payingId, setPayingId] = useState(null);
  const [notice, setNotice] = useState("");

  const handlePay = async (policyId) => {
    setPayingId(policyId);
    setNotice("");
    try {
      const { data } = await api.post(`/api/payments/${policyId}/pay`);

      if (data.mock) {
        // No real gateway configured on the backend — it already marked the
        // payment PAID, same as the old mocked behavior.
        onPay(data.payment);
        setNotice("Payment recorded (demo mode — no payment gateway configured on the backend).");
        return;
      }

      // Real order created — hand off to Razorpay's checkout modal. The
      // payment only actually gets marked PAID by the backend's webhook,
      // never by this client-side handler, since a browser callback can't
      // be trusted for money.
      const options = {
        key: data.keyId,
        amount: Math.round(Number(data.amount) * 100),
        currency: data.currency,
        order_id: data.orderId,
        name: "Suraksha Insurance",
        description: "Premium payment",
        handler: function () {
          setNotice("Payment submitted — confirming with the bank. This can take a few seconds; refresh to see the updated status.");
        },
        modal: {
          ondismiss: function () {
            setNotice("Payment window closed before completing.");
          },
        },
      };
      const razorpay = new window.Razorpay(options);
      razorpay.open();
    } catch (err) {
      setNotice(err?.response?.data?.message || "Couldn't start the payment. Please try again.");
    } finally {
      setPayingId(null);
    }
  };

  const duePolicy = policies[0];
  return (
    <div>
      <h1 className="font-serif text-2xl text-[#16303F]">Payments</h1>
      <p className="text-sm text-[#4B5563] mt-1">Premium payments across all your policies.</p>
      <div className="bg-white border border-[#E4E1D8] rounded-lg mt-6 overflow-hidden">
        <div className="grid grid-cols-4 gap-4 px-5 py-3 text-xs text-[#4B5563] border-b border-[#E4E1D8]">
          <span>Policy</span><span>Method</span><span>Status</span><span className="text-right">Amount</span>
        </div>
        {payments.length === 0 && <div className="px-5 py-4 text-sm text-[#4B5563]">No payments yet.</div>}
        {payments.map((p) => (
          <div key={p.id} className="grid grid-cols-4 gap-4 px-5 py-4 text-sm border-b border-[#E4E1D8] last:border-0 items-center">
            <span className="text-[#16303F]">{p.policy?.policyNumber}</span>
            <span className="text-[#4B5563]">{p.method}</span>
            <span className={`text-xs px-2 py-1 rounded-full w-fit ${statusStyle(p.status)}`}>{p.status}</span>
            <span className="text-right text-[#16303F]">{inr(p.amount)}</span>
          </div>
        ))}
      </div>
      {notice && <p className="text-sm text-[#4B5563] mt-4">{notice}</p>}
      {duePolicy && (
        <div className="mt-6 bg-white border border-[#E4E1D8] rounded-lg p-5 flex items-center justify-between">
          <div>
            <div className="text-sm text-[#16303F]">Pay premium</div>
            <div className="text-xs text-[#4B5563] mt-0.5 flex items-center gap-1"><Clock className="w-3.5 h-3.5" /> {duePolicy.policyNumber}</div>
          </div>
          <button onClick={() => handlePay(duePolicy.id)} disabled={payingId === duePolicy.id}
            className="text-sm px-4 py-2 rounded bg-[#16303F] text-white disabled:opacity-60">
            {payingId === duePolicy.id ? "Processing…" : `Pay ${inr(duePolicy.premium)}`}
          </button>
        </div>
      )}
    </div>
  );
}

function Profile({ user }) {
  return (
    <div className="max-w-md">
      <h1 className="font-serif text-2xl text-[#16303F]">Profile</h1>
      <div className="bg-white border border-[#E4E1D8] rounded-lg p-6 mt-6 space-y-4">
        <div><div className="text-xs text-[#4B5563]">Full name</div><div className="text-sm text-[#16303F] mt-0.5">{user?.fullName}</div></div>
        <div><div className="text-xs text-[#4B5563]">Email</div><div className="text-sm text-[#16303F] mt-0.5">{user?.email}</div></div>
        <div>
          <div className="text-xs text-[#4B5563]">KYC status</div>
          <span className={`inline-block mt-1 text-xs px-2.5 py-1 rounded-full ${user?.kycVerified ? "bg-[#E7F0EA] text-[#2E6E52]" : "bg-[#FBF1DF] text-[#8A6412]"}`}>
            {user?.kycVerified ? "Verified" : "Pending"}
          </span>
        </div>
      </div>
    </div>
  );
}

export default function App() {
  const [page, setPage] = useState("landing"); // landing | login | app
  const [active, setActive] = useState("dashboard");
  const [mobileOpen, setMobileOpen] = useState(false);
  const [user, setUser] = useState(null);
  const [policies, setPolicies] = useState([]);
  const [claims, setClaims] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loadError, setLoadError] = useState("");

  const loadAppData = async () => {
    try {
      const [p, c, pay] = await Promise.all([
        api.get("/api/policies"),
        api.get("/api/claims"),
        api.get("/api/payments"),
      ]);
      setPolicies(p.data);
      setClaims(c.data);
      setPayments(pay.data);
    } catch {
      setLoadError("Couldn't load your account data. Check the backend is running.");
    }
  };

  useEffect(() => {
    if (page === "app") loadAppData();
  }, [page]);

  const handleLoggedIn = (loggedInUser) => {
    setUser(loggedInUser);
    setPage("app");
  };

  const handleLogout = async () => {
    try { await api.post("/api/auth/logout"); } catch { /* ignore */ }
    setUser(null);
    setPage("landing");
  };

  if (page === "landing") return <Landing onLogin={() => setPage("login")} />;
  if (page === "login") return <LoginPage onBack={() => setPage("landing")} onLoggedIn={handleLoggedIn} />;

  return (
    <div className="min-h-screen bg-[#F5F4EF] flex">
      <Sidebar active={active} setActive={setActive} onLogout={handleLogout} mobileOpen={mobileOpen} setMobileOpen={setMobileOpen} />
      <div className="flex-1 min-w-0">
        <div className="md:hidden flex items-center justify-between px-4 py-4 border-b border-[#E4E1D8] bg-white">
          <div className="flex items-center gap-2">
            <Shield className="w-5 h-5 text-[#16303F]" strokeWidth={1.75} />
            <span className="font-serif text-base text-[#16303F]">Suraksha</span>
          </div>
          <button onClick={() => setMobileOpen(true)}><Menu className="w-5 h-5 text-[#16303F]" /></button>
        </div>
        <div className="p-6 md:p-10">
          {loadError && <div className="mb-4 text-sm text-[#B0463D] flex items-center gap-2"><AlertCircle className="w-4 h-4" /> {loadError}</div>}
          {active === "dashboard" && <Dashboard userName={user?.fullName} policies={policies} claims={claims} />}
          {active === "policies" && <Policies policies={policies} />}
          {active === "claim" && <FileClaim policies={policies} onFiled={(c) => setClaims([c, ...claims])} />}
          {active === "payments" && <Payments payments={payments} policies={policies} onPay={(p) => setPayments([p, ...payments])} />}
          {active === "profile" && <Profile user={user} />}
        </div>
      </div>
      <ChatWidget policies={policies} />
    </div>
  );
}
