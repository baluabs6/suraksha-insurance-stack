import { useEffect, useState } from "react";
import {
  Shield, HeartPulse, Car, Umbrella, Bike, Plane, Home, Activity,
  ShieldAlert, Smartphone, PawPrint, Briefcase, Ship,
} from "lucide-react";
import api from "./api/client.js";

// Icon keys come from the backend's product catalogue (GET /api/products).
const ICONS = {
  "heart-pulse": HeartPulse, car: Car, umbrella: Umbrella, bike: Bike, plane: Plane, home: Home,
  activity: Activity, "shield-alert": ShieldAlert, smartphone: Smartphone, "paw-print": PawPrint,
  briefcase: Briefcase, ship: Ship,
};

// Shown only if the catalogue can't be fetched (for example an older backend).
const FALLBACK = [
  { type: "HEALTH", label: "Health", category: "Health & Life", icon: "heart-pulse", planName: "CarePlus Family", fromPrice: "₹649/month",
    tagline: "Covers the whole family under one policy, no sub-limits on room rent.", dedicatedClaimFlow: true, claimFields: [], requiredDocuments: [] },
  { type: "MOTOR", label: "Car", category: "Vehicle", icon: "car", planName: "DriveSecure Comprehensive", fromPrice: "₹399/month",
    tagline: "Zero depreciation cover with a 60-minute garage cashless promise.", dedicatedClaimFlow: false, claimFields: [], requiredDocuments: [] },
  { type: "LIFE", label: "Life (Term)", category: "Health & Life", icon: "umbrella", planName: "LifeShield Term 30", fromPrice: "₹899/month",
    tagline: "₹50 lakh cover with a decision on your application within 48 hours.", dedicatedClaimFlow: false, claimFields: [], requiredDocuments: [] },
];

let cache = null;

export const productFor = (type) => (cache || FALLBACK).find((p) => p.type === type) || null;
export const planIcon = (type) => ICONS[productFor(type)?.icon] || Shield;

/** Loads the catalogue once per page load; call it near the root so every screen sees it. */
export function useProducts() {
  const [products, setProducts] = useState(cache || FALLBACK);
  useEffect(() => {
    if (cache) return;
    api.get("/api/products")
      .then(({ data }) => { if (Array.isArray(data) && data.length) { cache = data; setProducts(data); } })
      .catch(() => { /* keep the fallback */ });
  }, []);
  return products;
}

export const humanize = (key) =>
  key.replace(/([A-Z])/g, " $1").replace(/^./, (c) => c.toUpperCase());

const isBlank = (v) => v === undefined || v === null || String(v).trim() === "";

export const isFieldRequired = (field, details) =>
  field.required ||
  (!!field.requiredWhenKey &&
    String(details[field.requiredWhenKey] ?? "").toLowerCase() === String(field.requiredWhenValue).toLowerCase());

/** Same rules the server enforces, so people see problems before submitting. Keys are `detail_<fieldKey>`. */
export function validateProductDetails(product, details) {
  const errs = {};
  (product?.claimFields || []).forEach((f) => {
    const v = details[f.key];
    if (isBlank(v)) {
      if (isFieldRequired(f, details)) errs[`detail_${f.key}`] = `Enter ${f.label.toLowerCase()}.`;
    } else if (f.type === "number" && Number(v) < 0) {
      errs[`detail_${f.key}`] = `${f.label} can't be negative.`;
    }
  });
  return errs;
}

export const cleanDetails = (details) =>
  Object.fromEntries(Object.entries(details).filter(([, v]) => !isBlank(v)));
