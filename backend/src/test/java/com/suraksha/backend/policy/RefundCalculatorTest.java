package com.suraksha.backend.policy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RefundCalculatorTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 31);
    private static final BigDecimal PREMIUM = new BigDecimal("3650");

    @Test
    void midTermCancellationRefundsUnusedDaysLessTheFee() {
        var r = RefundCalculator.compute(PREMIUM, START, END, LocalDate.of(2026, 7, 2), 10, false);
        assertEquals(182, r.remainingDays());
        assertEquals(new BigDecimal("1820"), r.unusedPremium());
        assertEquals(new BigDecimal("182"), r.fee());
        assertEquals(new BigDecimal("1638"), r.refund());
    }

    @Test
    void cancellingBeforeTheStartDateRefundsTheWholeTermLessTheFee() {
        var r = RefundCalculator.compute(PREMIUM, START, END, START.minusDays(5), 10, false);
        assertEquals(365, r.remainingDays());
        assertEquals(new BigDecimal("3285"), r.refund());
    }

    @Test
    void noRefundOnceTheTermHasEndedOrAClaimWasPaid() {
        assertEquals(0, RefundCalculator.compute(PREMIUM, START, END, END.plusDays(3), 10, false).refund().signum());
        var paid = RefundCalculator.compute(PREMIUM, START, END, LocalDate.of(2026, 3, 1), 10, true);
        assertEquals(0, paid.refund().signum());
        assertTrue(paid.note().toLowerCase().contains("claim"));
    }

    @Test
    void refundIsNeverNegative() {
        assertTrue(RefundCalculator.compute(PREMIUM, START, END, END, 100, false).refund().signum() >= 0);
    }
}
