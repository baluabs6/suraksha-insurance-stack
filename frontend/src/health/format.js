export const rupees = (n) => `₹${Number(n || 0).toLocaleString("en-IN")}`;

export const EMPTY_HEALTH_CLAIM = {
  memberId: "", hospitalName: "", admissionDate: "", dischargeDate: "", diagnosis: "",
  treatingDoctor: "", accidental: false,
  roomCharges: "", procedureCharges: "", medicineCharges: "", diagnosticCharges: "", otherCharges: "",
};

export const BILL_FIELDS = [
  ["roomCharges", "Room charges"],
  ["procedureCharges", "Surgery / procedure & doctor fees"],
  ["medicineCharges", "Medicines & consumables"],
  ["diagnosticCharges", "Diagnostics & tests"],
  ["otherCharges", "Other charges"],
];

export const billTotal = (h) =>
  BILL_FIELDS.reduce((sum, [key]) => sum + (Number(h[key]) || 0), 0);
