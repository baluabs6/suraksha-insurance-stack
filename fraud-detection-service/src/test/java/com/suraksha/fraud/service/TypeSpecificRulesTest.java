package com.suraksha.fraud.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TypeSpecificRulesTest {

    private static final LocalDate START = LocalDate.of(2026, 3, 1);
    private static final LocalDate END = LocalDate.of(2027, 2, 28);

    private static Set<String> flags(String type, LocalDate incident, Instant submitted, Map<String, Object> details,
                                     long sameAsset, long sameDay, long repeat) {
        List<TypeSpecificRules.Finding> findings = TypeSpecificRules.evaluate(new TypeSpecificRules.Input(
                type, START, END, incident, submitted, details, sameAsset, sameDay, repeat));
        return findings.stream().map(TypeSpecificRules.Finding::flag).collect(Collectors.toSet());
    }

    private static Instant at(LocalDate d) {
        return d.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
    }

    @Test
    void cleanClaimRaisesNothing() {
        LocalDate incident = LocalDate.of(2026, 8, 10);
        assertTrue(flags("TWO_WHEELER", incident, at(incident.plusDays(2)),
                Map.of("incidentType", "Accident"), 0, 0, 0).isEmpty());
    }

    @Test
    void incidentOutsidePolicyPeriodIsFlagged() {
        LocalDate before = START.minusDays(3);
        assertTrue(flags("TRAVEL", before, at(before.plusDays(1)), Map.of(), 0, 0, 0)
                .contains(TypeSpecificRules.INCIDENT_OUTSIDE_POLICY_PERIOD));
        LocalDate after = END.plusDays(1);
        assertTrue(flags("HOME", after, at(after), Map.of(), 0, 0, 0)
                .contains(TypeSpecificRules.INCIDENT_OUTSIDE_POLICY_PERIOD));
    }

    @Test
    void theftShortlyAfterInceptionIsFlaggedButLaterTheftIsNot() {
        LocalDate early = START.plusDays(10);
        assertTrue(flags("GADGET", early, at(early), Map.of("damageType", "Theft"), 0, 0, 0)
                .contains(TypeSpecificRules.THEFT_SOON_AFTER_INCEPTION));
        LocalDate later = START.plusDays(90);
        assertFalse(flags("GADGET", later, at(later), Map.of("damageType", "Theft"), 0, 0, 0)
                .contains(TypeSpecificRules.THEFT_SOON_AFTER_INCEPTION));
    }

    @Test
    void lateReportingAppliesToNonHealthOnly() {
        LocalDate incident = LocalDate.of(2026, 5, 1);
        Instant submitted = at(incident.plusDays(45));
        assertTrue(flags("MOTOR", incident, submitted, Map.of(), 0, 0, 0).contains(TypeSpecificRules.REPORTED_LATE));
        assertFalse(flags("HEALTH", incident, submitted, Map.of(), 0, 0, 0).contains(TypeSpecificRules.REPORTED_LATE));
    }

    @Test
    void sameDayAssetDuplicateOutranksPlainDuplicate() {
        LocalDate incident = LocalDate.of(2026, 6, 1);
        Set<String> both = flags("MOTOR", incident, at(incident), Map.of(), 2, 1, 0);
        assertTrue(both.contains(TypeSpecificRules.SAME_ASSET_SAME_DAY_CLAIM));
        assertFalse(both.contains(TypeSpecificRules.SAME_ASSET_CLAIMED_AGAIN));
        assertTrue(flags("MOTOR", incident, at(incident), Map.of(), 1, 0, 0).contains(TypeSpecificRules.SAME_ASSET_CLAIMED_AGAIN));
    }

    @Test
    void repeatedCategoryNeedsAtLeastTwoEarlierClaims() {
        LocalDate incident = LocalDate.of(2026, 6, 1);
        assertFalse(flags("TRAVEL", incident, at(incident), Map.of("claimCategory", "Flight delay"), 0, 0, 1)
                .contains(TypeSpecificRules.REPEAT_CLAIM_CATEGORY_ON_POLICY));
        assertTrue(flags("TRAVEL", incident, at(incident), Map.of("claimCategory", "Flight delay"), 0, 0, 2)
                .contains(TypeSpecificRules.REPEAT_CLAIM_CATEGORY_ON_POLICY));
    }

    @Test
    void cyberFraudNotReportedIsFlaggedUnlessReported() {
        LocalDate incident = LocalDate.of(2026, 6, 1);
        assertTrue(flags("CYBER", incident, at(incident), Map.of("incidentType", "Online or UPI fraud"), 0, 0, 0)
                .contains(TypeSpecificRules.CYBER_INCIDENT_NOT_REPORTED));
        assertFalse(flags("CYBER", incident, at(incident),
                Map.of("incidentType", "Online or UPI fraud", "reportedToCyberCell", true), 0, 0, 0)
                .contains(TypeSpecificRules.CYBER_INCIDENT_NOT_REPORTED));
        assertFalse(flags("CYBER", incident, at(incident), Map.of("incidentType", "Online harassment"), 0, 0, 0)
                .contains(TypeSpecificRules.CYBER_INCIDENT_NOT_REPORTED));
    }

    @Test
    void identifiersAreNormalised() {
        assertEquals("KA01HX4821", TypeSpecificRules.normalizeIdentifier(" ka-01 hx/4821 "));
        assertEquals("", TypeSpecificRules.normalizeIdentifier(null));
        assertEquals("vehicleRegistrationNumber", TypeSpecificRules.assetKey("MOTOR"));
        assertNull(TypeSpecificRules.assetKey("HEALTH"));
    }
}
