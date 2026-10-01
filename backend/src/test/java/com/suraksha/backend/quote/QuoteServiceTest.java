package com.suraksha.backend.quote;

import com.suraksha.backend.policy.PolicyType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QuoteServiceTest {

    private final QuoteService service = new QuoteService();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    private static QuoteRequest request(PolicyType type, BigDecimal coverage, Map<String, Object> inputs) {
        QuoteRequest r = new QuoteRequest();
        r.setType(type);
        r.setCoverageAmount(coverage);
        r.setInputs(inputs);
        return r;
    }

    @Test
    void everyTypeHasAQuoteForm() {
        for (PolicyType type : PolicyType.values()) {
            QuoteSpec spec = QuoteRegistry.get(type);
            assertFalse(spec.fields().isEmpty(), type + " needs quote fields");
            assertTrue(spec.coverageFromField() != null || !spec.coverageOptions().isEmpty(), type + " needs a coverage source");
            if (spec.coverageFromField() != null) {
                assertTrue(spec.fields().stream().anyMatch(f -> f.key().equals(spec.coverageFromField())),
                        type + " coverage field is missing from its form");
            }
        }
    }

    @Test
    void quotesAreComputedOnTheServerAndOnlyAttributeKeysAreKept() {
        var result = service.quote(request(PolicyType.TWO_WHEELER, null, Map.of(
                "vehicleValue", 80000, "vehicleAgeYears", 2, "engineCc", "76 to 150 cc",
                "vehicleRegistrationNumber", "KA-01-HX-4821", "vehicleModel", "Honda Activa")), TODAY);

        assertEquals(new BigDecimal("80000"), result.coverageAmount());
        assertEquals(TODAY, result.startDate());
        assertEquals(TODAY.plusYears(1).minusDays(1), result.endDate());
        assertEquals(Map.of("vehicleRegistrationNumber", "KA-01-HX-4821", "vehicleModel", "Honda Activa"), result.attributes());
        assertFalse(result.attributes().containsKey("vehicleValue"));
    }

    @Test
    void lifeAndTravelTermsComeFromTheAnswers() {
        var life = service.quote(request(PolicyType.LIFE, BigDecimal.valueOf(10_000_000),
                Map.of("age", 30, "smoker", false, "termYears", "20")), TODAY);
        assertEquals(TODAY.plusYears(20).minusDays(1), life.endDate());
        assertTrue(life.attributes().isEmpty(), "age and smoker status must not be stored on the policy");

        var travel = service.quote(request(PolicyType.TRAVEL, BigDecimal.valueOf(500_000), Map.of(
                "destination", "Singapore", "destinationRegion", "Asia", "tripDays", 10, "travellers", 2)), TODAY);
        assertEquals(TODAY.plusDays(9), travel.endDate());
    }

    @Test
    void coverageMustBeOneOfTheOfferedOptions() {
        var e = assertThrows(IllegalArgumentException.class, () -> service.quote(request(PolicyType.HOME, BigDecimal.valueOf(123_456),
                Map.of("propertyType", "Apartment", "propertyAddress", "Flat 1", "constructionAgeYears", 5)), TODAY));
        assertTrue(e.getMessage().toLowerCase().contains("sum insured"));
    }

    @Test
    void startDateMustBeWithinTheNextSixtyDays() {
        QuoteRequest r = request(PolicyType.CYBER, BigDecimal.valueOf(100_000), Map.of("riskProfile", "Basic (browsing, messaging)"));
        r.setStartDate(TODAY.minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> service.quote(r, TODAY));
        r.setStartDate(TODAY.plusDays(61));
        assertThrows(IllegalArgumentException.class, () -> service.quote(r, TODAY));
        r.setStartDate(TODAY.plusDays(30));
        assertEquals(TODAY.plusDays(30), service.quote(r, TODAY).startDate());
    }

    @Test
    void unknownOrMissingAnswersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.quote(request(PolicyType.CYBER, BigDecimal.valueOf(100_000),
                Map.of("riskProfile", "Basic (browsing, messaging)", "discount", "100")), TODAY));
        assertThrows(IllegalArgumentException.class, () -> service.quote(request(PolicyType.CYBER, BigDecimal.valueOf(100_000), Map.of()), TODAY));
    }
}
