package com.suraksha.backend.health;

import com.suraksha.backend.health.HealthClaimAssessor.Bill;
import com.suraksha.backend.health.HealthClaimAssessor.Input;
import com.suraksha.backend.health.HealthClaimAssessor.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class HealthClaimAssessorTest {

    private static final LocalDate POLICY_START = LocalDate.of(2026, 1, 4);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, bd(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private static Input input(Bill bill, LocalDate admission, LocalDate discharge, boolean accidental,
                               String diagnosis, String conditions, BigDecimal roomCap, int coPay,
                               BigDecimal remaining) {
        return new Input(bill, admission, discharge, accidental, diagnosis, conditions, POLICY_START,
                roomCap, coPay, 30, 24, remaining);
    }

    @Test
    void plainClaimWithNoTermsIsPaidInFull() {
        Bill bill = new Bill(bd("6000"), bd("20000"), bd("3000"), bd("2000"), bd("1000"));
        Result r = HealthClaimAssessor.assess(input(bill, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 23),
                false, "viral fever", null, null, 0, bd("1000000")));
        assertAmount("32000", r.billedTotal());
        assertAmount("32000", r.eligibleAmount());
        assertTrue(r.deductions().isEmpty());
        assertTrue(r.reviewNotes().isEmpty());
    }

    @Test
    void roomRentAboveCapReducesRoomProcedureAndOtherProportionately_thenCoPayApplies() {
        // 3 nights, room 30000 (10000/night) against a 5000/night cap -> ratio 0.5 on room+procedure+other
        Bill bill = new Bill(bd("30000"), bd("60000"), bd("10000"), bd("5000"), bd("5000"));
        Result r = HealthClaimAssessor.assess(input(bill, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 23),
                false, "appendectomy", null, bd("5000"), 10, bd("1000000")));
        assertAmount("110000", r.billedTotal());
        // associated 95000 -> 47500 allowed, so 47500 deducted; 62500 remains; 10% co-pay = 6250
        assertAmount("56250", r.eligibleAmount());
        assertEquals(2, r.deductions().size());
        assertEquals("ROOM_RENT_PROPORTIONATE", r.deductions().get(0).code());
        assertAmount("47500", r.deductions().get(0).amount());
        assertEquals("CO_PAY", r.deductions().get(1).code());
        assertAmount("6250", r.deductions().get(1).amount());
    }

    @Test
    void roomRentWithinCapIsNotReduced() {
        Bill bill = new Bill(bd("12000"), bd("20000"), null, null, null);
        Result r = HealthClaimAssessor.assess(input(bill, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 23),
                false, "fracture", null, bd("5000"), 0, bd("1000000")));
        assertAmount("32000", r.eligibleAmount());
        assertTrue(r.deductions().isEmpty());
    }

    @Test
    void eligibleAmountIsCappedAtRemainingSumInsured() {
        Bill bill = new Bill(null, bd("50000"), null, null, null);
        Result r = HealthClaimAssessor.assess(input(bill, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 22),
                false, "surgery", null, null, 0, bd("20000")));
        assertAmount("20000", r.eligibleAmount());
        assertEquals("SUM_INSURED_CAP", r.deductions().get(0).code());
        assertAmount("30000", r.deductions().get(0).amount());
    }

    @Test
    void sameDayAdmissionCountsAsOneDayForRoomRent() {
        Bill bill = new Bill(bd("8000"), null, null, null, null);
        Result r = HealthClaimAssessor.assess(input(bill, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 20),
                false, "day care", null, bd("5000"), 0, bd("1000000")));
        // 8000 in one day vs 5000 cap -> only 5000 admissible
        assertAmount("5000", r.eligibleAmount());
    }

    @Test
    void admissionInsideInitialWaitingPeriodIsFlaggedUnlessAccident() {
        Bill bill = new Bill(null, bd("10000"), null, null, null);
        LocalDate admission = LocalDate.of(2026, 1, 20); // 16 days after policy start
        Result illness = HealthClaimAssessor.assess(input(bill, admission, admission.plusDays(2), false,
                "dengue", null, null, 0, bd("1000000")));
        assertEquals(1, illness.reviewNotes().size());
        assertTrue(illness.reviewNotes().get(0).contains("initial waiting period"));

        Result accident = HealthClaimAssessor.assess(input(bill, admission, admission.plusDays(2), true,
                "fracture", null, null, 0, bd("1000000")));
        assertTrue(accident.reviewNotes().isEmpty());
    }

    @Test
    void admissionAfterInitialWaitingPeriodIsNotFlagged() {
        Bill bill = new Bill(null, bd("10000"), null, null, null);
        LocalDate admission = LocalDate.of(2026, 3, 1);
        Result r = HealthClaimAssessor.assess(input(bill, admission, admission.plusDays(2), false,
                "dengue", null, null, 0, bd("1000000")));
        assertTrue(r.reviewNotes().isEmpty());
    }

    @Test
    void diagnosisMatchingDeclaredConditionIsFlaggedDuringPreExistingWaiting() {
        Bill bill = new Bill(null, bd("10000"), null, null, null);
        LocalDate admission = LocalDate.of(2026, 8, 20);
        Result r = HealthClaimAssessor.assess(input(bill, admission, admission.plusDays(2), false,
                "Diabetes", "Type 2 diabetes; hypertension", null, 0, bd("1000000")));
        assertEquals(1, r.reviewNotes().size());
        assertTrue(r.reviewNotes().get(0).contains("pre-existing"));

        Result unrelated = HealthClaimAssessor.assess(input(bill, admission, admission.plusDays(2), false,
                "viral fever", "Type 2 diabetes; hypertension", null, 0, bd("1000000")));
        assertTrue(unrelated.reviewNotes().isEmpty());
    }

    @Test
    void preExistingNoteDoesNotApplyOnceWaitingPeriodHasPassed() {
        Bill bill = new Bill(null, bd("10000"), null, null, null);
        LocalDate admission = POLICY_START.plusMonths(25);
        Result r = HealthClaimAssessor.assess(input(bill, admission, admission.plusDays(2), false,
                "diabetes", "diabetes", null, 0, bd("1000000")));
        assertTrue(r.reviewNotes().isEmpty());
    }

    @Test
    void tooShortConditionTokensDoNotMatchEverything() {
        assertFalse(HealthClaimAssessor.matchesDeclaredCondition("appendicitis", "bp, a"));
        assertFalse(HealthClaimAssessor.matchesDeclaredCondition(null, "diabetes"));
        assertFalse(HealthClaimAssessor.matchesDeclaredCondition("diabetes", " "));
    }
}
