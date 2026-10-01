import React, { useEffect, useRef, useState } from "react";
import api from "./api/client.js";

const MAX_BYTES = 6 * 1024 * 1024;
const ACCEPTED = ["image/jpeg", "image/png", "application/pdf"];
const TYPES = [
  { value: "CLAIM_PHOTO", label: "Photo" },
  { value: "CLAIM_BILL", label: "Bill or invoice" },
  { value: "CLAIM_ESTIMATE", label: "Repair estimate" },
  { value: "OTHER", label: "Other" },
];

const toBase64 = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result).split(",")[1]);
    reader.onerror = () => reject(new Error("Couldn't read the file."));
    reader.readAsDataURL(file);
  });

/** Upload supporting documents against a filed claim. The adjuster sees them with the claim. */
export default function ClaimDocuments({ claimId }) {
  const [docs, setDocs] = useState([]);
  const [documentType, setDocumentType] = useState("CLAIM_PHOTO");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const fileInput = useRef(null);

  const load = () =>
    api.get("/api/documents").then(({ data }) => setDocs(data.filter((d) => d.claimId === claimId))).catch(() => {});

  useEffect(() => { load(); }, [claimId]);

  const upload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setMessage("");
    if (!ACCEPTED.includes(file.type)) { setMessage("Upload a JPG, PNG or PDF file."); return; }
    if (file.size > MAX_BYTES) { setMessage("That file is too large. The limit is about 6 MB."); return; }
    setBusy(true);
    try {
      await api.post("/api/documents", {
        claimId, documentType, fileName: file.name, contentType: file.type, base64Content: await toBase64(file),
      });
      await load();
    } catch (err) {
      setMessage(err?.response?.data?.message || "Couldn't upload that file. Please try again.");
    } finally {
      setBusy(false);
      if (fileInput.current) fileInput.current.value = "";
    }
  };

  return (
    <div className="mt-6 bg-white border border-[#E4E1D8] rounded-lg p-5 text-sm">
      <div className="text-[#16303F]">Add supporting documents</div>
      <p className="text-xs text-[#4B5563] mt-1">Photos, bills and estimates help your claim get reviewed faster.</p>
      <div className="flex flex-wrap items-center gap-3 mt-3">
        <select value={documentType} onChange={(e) => setDocumentType(e.target.value)}
          className="border border-[#E4E1D8] rounded px-2 py-1.5 text-sm">
          {TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
        </select>
        <input ref={fileInput} type="file" accept=".jpg,.jpeg,.png,.pdf" onChange={upload} disabled={busy} className="text-xs" />
      </div>
      {busy && <p className="text-xs text-[#4B5563] mt-2">Uploading…</p>}
      {message && <p className="text-xs text-[#B0463D] mt-2">{message}</p>}
      {docs.length > 0 && (
        <ul className="mt-3 space-y-1 text-xs text-[#4B5563]">
          {docs.map((d) => <li key={d.id}>{d.fileName} <span className="text-[#16303F]">({d.documentType.replace("CLAIM_", "").toLowerCase()})</span></li>)}
        </ul>
      )}
    </div>
  );
}
