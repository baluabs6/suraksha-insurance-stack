package com.suraksha.backend.health;

import com.suraksha.backend.claims.dto.ClaimRequest;
import com.suraksha.backend.policy.Policy;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Field-level rules for health claims. Returns a customer-facing message, or null if the request is fine. */
public final class HealthClaimValidator {

    private HealthClaimValidator() {}

    public static String validate(ClaimRequest req, Policy policy, LocalDate today) {
        if (req.getMemberId() == null) return "Select which insured member was treated.";
        if (isBlank(req.getHospitalName())) return "Enter the hospital name.";
        if (isBlank(req.getDiagnosis())) return "Enter the diagnosis or reason for admission.";
        if (req.getAdmissionDate() == null || req.getDischargeDate() == null) {
            return "Enter the admission and discharge dates.";
        }
        if (req.getDischargeDate().isBefore(req.getAdmissionDate())) {
            return "The discharge date can't be before the admission date.";
        }
        if (req.getDischargeDate().isAfter(today)) return "The discharge date can't be in the future.";
        if (req.getAdmissionDate().isBefore(policy.getStartDate()) || req.getAdmissionDate().isAfter(policy.getEndDate())) {
            return "The admission date falls outside this policy's coverage period.";
        }
        BigDecimal billTotal = billTotal(req);
        if (billTotal.signum() <= 0) return "Enter the itemised bill amounts.";
        if (billTotal.compareTo(req.getClaimAmount()) != 0) {
            return "The itemised bill total doesn't match the claim amount.";
        }
        return null;
    }

    public static BigDecimal billTotal(ClaimRequest req) {
        return nz(req.getRoomCharges()).add(nz(req.getProcedureCharges())).add(nz(req.getMedicineCharges()))
                .add(nz(req.getDiagnosticCharges())).add(nz(req.getOtherCharges()));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
