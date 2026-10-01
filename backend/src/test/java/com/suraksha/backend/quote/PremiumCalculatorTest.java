package com.suraksha.backend.quote;

import com.suraksha.backend.policy.PolicyType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PremiumCalculatorTest {

    private static PremiumCalculator.Quote health(int age, int members) {
        return PremiumCalculator.calculate(PolicyType.HEALTH, BigDecimal.valueOf(500_000),
                Map.of("eldestMemberAge", BigDecimal.valueOf(age), "membersCount", BigDecimal.valueOf(members)));
    }

    @Test
    void breakdownAlwaysAddsUpToThePremium() {
        var q = health(34, 3);
        BigDecimal sum = q.lines().stream().map(PremiumCalculator.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(q.premium(), sum);
        assertEquals(new BigDecimal("12600"), q.premium());
    }

    @Test
    void olderAgeAndMoreMembersCostMore() {
        assertTrue(health(60, 1).premium().compareTo(health(30, 1).premium()) > 0);
        assertTrue(health(30, 4).premium().compareTo(health(30, 1).premium()) > 0);
    }

    @Test
    void smokersPayMoreForTermCover() {
        Map<String, Object> nonSmoker = Map.of("age", BigDecimal.valueOf(35), "smoker", false, "termYears", "20");
        Map<String, Object> smoker = Map.of("age", BigDecimal.valueOf(35), "smoker", true, "termYears", "20");
        BigDecimal cover = BigDecimal.valueOf(10_000_000);
        assertTrue(PremiumCalculator.calculate(PolicyType.LIFE, cover, smoker).premium()
                .compareTo(PremiumCalculator.calculate(PolicyType.LIFE, cover, nonSmoker).premium()) > 0);
    }

    @Test
    void outOfRangeInputsAreRejectedWithAReadableMessage() {
        var e = assertThrows(IllegalArgumentException.class, () -> health(90, 1));
        assertTrue(e.getMessage().contains("75"));
        assertThrows(IllegalArgumentException.class, () -> health(30, 9));
        assertThrows(IllegalArgumentException.class, () -> PremiumCalculator.calculate(PolicyType.LIFE, BigDecimal.valueOf(5_000_000),
                Map.of("age", BigDecimal.valueOf(62), "smoker", false, "termYears", "30")));
    }

    @Test
    void travelHasAMinimumPremiumAndScalesWithTripLength() {
        var shortTrip = PremiumCalculator.calculate(PolicyType.TRAVEL, BigDecimal.valueOf(250_000), Map.of(
                "destinationRegion", "Domestic", "tripDays", BigDecimal.valueOf(1), "travellers", BigDecimal.valueOf(1)));
        assertEquals(new BigDecimal("149"), shortTrip.premium());
        var longTrip = PremiumCalculator.calculate(PolicyType.TRAVEL, BigDecimal.valueOf(250_000), Map.of(
                "destinationRegion", "Domestic", "tripDays", BigDecimal.valueOf(30), "travellers", BigDecimal.valueOf(1)));
        assertTrue(longTrip.premium().compareTo(shortTrip.premium()) > 0);
    }

    @Test
    void everyPremiumIsAPositiveWholeRupeeAmount() {
        var q = PremiumCalculator.calculate(PolicyType.TWO_WHEELER, BigDecimal.valueOf(80_000), Map.of(
                "vehicleValue", BigDecimal.valueOf(80_000), "vehicleAgeYears", BigDecimal.valueOf(2), "engineCc", "76 to 150 cc"));
        assertTrue(q.premium().signum() > 0);
        assertEquals(0, q.premium().scale());
    }
}
