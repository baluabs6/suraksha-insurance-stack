package com.suraksha.backend.health;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic health-claim assessment. No AI involved: the same inputs always give the same
 * numbers, and nothing here approves or rejects a claim. The result is an estimate stored on the
 * claim for the adjuster (and shown to the customer as "estimated"), plus review notes.
 *
 * Assumptions to confirm against your actual product wording / IRDAI guidance before go-live:
 *  - the room-rent proportionate deduction applies to room, procedure and "other" charges;
 *    medicines and diagnostics are not scaled;
 *  - co-pay is applied after the room-rent deduction;
 *  - the sum insured cap is applied last;
 *  - waiting periods are measured from this policy's start date (renewal continuity is not modelled);
 *  - the pre-existing-condition match is a plain-text heuristic and only ever raises a review note.
 */
public final class HealthClaimAssessor {

    private HealthClaimAssessor() {}

    public record Bill(BigDecimal room, BigDecimal procedure, BigDecimal medicines,
                       BigDecimal diagnostics, BigDecimal other) {
        public BigDecimal total() {
            return nz(room).add(nz(procedure)).add(nz(medicines)).add(nz(diagnostics)).add(nz(other));
        }
    }

    public record Input(Bill bill, LocalDate admission, LocalDate discharge, boolean accidental,
                        String diagnosis, String declaredConditions, LocalDate policyStart,
                        BigDecimal roomRentCapPerDay, int coPayPercent, int initialWaitingDays,
                        int preExistingWaitingMonths, BigDecimal remainingSumInsured) {
    }

    public record Deduction(String code, BigDecimal amount, String description) {
    }

    public record Result(BigDecimal billedTotal, BigDecimal eligibleAmount,
                         List<Deduction> deductions, List<String> reviewNotes) {

        /** Plain-text summary stored on the claim. */
        public String toNotes() {
            List<String> lines = new ArrayList<>();
            for (Deduction d : deductions) {
                lines.add(d.description());
            }
            lines.addAll(reviewNotes);
            return String.join("\n", lines);
        }
    }

    public static Result assess(Input in) {
        BigDecimal total = in.bill().total();
        List<Deduction> deductions = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        long days = Math.max(1, ChronoUnit.DAYS.between(in.admission(), in.discharge()));
        BigDecimal running = total;

        BigDecimal room = nz(in.bill().room());
        BigDecimal cap = in.roomRentCapPerDay();
        if (cap != null && cap.signum() > 0 && room.signum() > 0) {
            BigDecimal allowedRoom = cap.multiply(BigDecimal.valueOf(days));
            if (room.compareTo(allowedRoom) > 0) {
                BigDecimal associated = room.add(nz(in.bill().procedure())).add(nz(in.bill().other()));
                BigDecimal eligibleAssociated = associated.multiply(allowedRoom).divide(room, 2, RoundingMode.HALF_UP);
                BigDecimal deduction = associated.subtract(eligibleAssociated);
                deductions.add(new Deduction("ROOM_RENT_PROPORTIONATE", deduction,
                        "Room rent is above the plan limit of " + rupees(cap) + " per day, so room, procedure and other charges are reduced proportionately: -" + rupees(deduction)));
                running = running.subtract(deduction);
            }
        }

        if (in.coPayPercent() > 0) {
            BigDecimal coPay = running.multiply(BigDecimal.valueOf(in.coPayPercent()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            deductions.add(new Deduction("CO_PAY", coPay,
                    "Co-payment of " + in.coPayPercent() + "% applies: -" + rupees(coPay)));
            running = running.subtract(coPay);
        }

        BigDecimal remaining = in.remainingSumInsured().max(BigDecimal.ZERO);
        if (running.compareTo(remaining) > 0) {
            BigDecimal over = running.subtract(remaining);
            deductions.add(new Deduction("SUM_INSURED_CAP", over,
                    "Limited to the remaining sum insured of " + rupees(remaining) + ": -" + rupees(over)));
            running = remaining;
        }

        if (!in.accidental() && in.admission().isBefore(in.policyStart().plusDays(in.initialWaitingDays()))) {
            notes.add("Admission is within the initial waiting period (first " + in.initialWaitingDays()
                    + " days of the policy) and is not marked as an accident. An adjuster will review this.");
        }
        if (matchesDeclaredCondition(in.diagnosis(), in.declaredConditions())
                && in.admission().isBefore(in.policyStart().plusMonths(in.preExistingWaitingMonths()))) {
            notes.add("The diagnosis may relate to a condition declared as pre-existing, and the "
                    + in.preExistingWaitingMonths() + "-month waiting period may still apply. An adjuster will review this.");
        }

        return new Result(total, running.max(BigDecimal.ZERO), List.copyOf(deductions), List.copyOf(notes));
    }

    /** Plain-text heuristic: true if the diagnosis and any declared condition contain one another. */
    static boolean matchesDeclaredCondition(String diagnosis, String declaredConditions) {
        if (diagnosis == null || diagnosis.isBlank() || declaredConditions == null || declaredConditions.isBlank()) {
            return false;
        }
        String d = diagnosis.trim().toLowerCase(Locale.ROOT);
        for (String token : declaredConditions.toLowerCase(Locale.ROOT).split("[,;\\n]")) {
            String t = token.trim();
            if (t.length() >= 3 && d.length() >= 3 && (d.contains(t) || t.contains(d))) {
                return true;
            }
        }
        return false;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String rupees(BigDecimal v) {
        return "₹" + v.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
