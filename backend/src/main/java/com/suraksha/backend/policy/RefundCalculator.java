package com.suraksha.backend.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pro-rata refund when a customer cancels mid-term: premium for the unused days, minus a cancellation fee,
 * and nothing if a claim has already been paid on the policy. The fee is configurable; confirm the actual
 * rules with your product terms and IRDAI requirements before real use.
 */
public final class RefundCalculator {

    public record Result(BigDecimal unusedPremium, BigDecimal fee, BigDecimal refund,
                         long remainingDays, long totalDays, String note) {}

    private RefundCalculator() {}

    public static Result compute(BigDecimal premium, LocalDate start, LocalDate end, LocalDate today,
                                 int feePercent, boolean claimPaid) {
        long totalDays = Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);
        LocalDate effective = today.isBefore(start) ? start.minusDays(1) : today;
        long remaining = Math.min(totalDays, Math.max(0, ChronoUnit.DAYS.between(effective, end)));

        BigDecimal unused = premium.multiply(BigDecimal.valueOf(remaining))
                .divide(BigDecimal.valueOf(totalDays), 0, RoundingMode.HALF_UP);
        BigDecimal fee = unused.multiply(BigDecimal.valueOf(feePercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        if (claimPaid) {
            return new Result(unused, BigDecimal.ZERO, BigDecimal.ZERO, remaining, totalDays,
                    "No refund is due because a claim has already been paid on this policy.");
        }
        return new Result(unused, fee, unused.subtract(fee).max(BigDecimal.ZERO), remaining, totalDays,
                "Refund is the premium for the unused " + remaining + " of " + totalDays + " days, less a "
                        + feePercent + "% cancellation fee.");
    }

    public static Result nothingPaid() {
        return new Result(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0,
                "Nothing has been paid for this policy, so there is nothing to refund.");
    }
}
