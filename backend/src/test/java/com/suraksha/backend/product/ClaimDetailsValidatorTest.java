package com.suraksha.backend.product;

import com.suraksha.backend.policy.PolicyType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClaimDetailsValidatorTest {

    private final ProductDefinition twoWheeler = ProductRegistry.get(PolicyType.TWO_WHEELER);
    private final ProductDefinition travel = ProductRegistry.get(PolicyType.TRAVEL);
    private final ProductDefinition cyber = ProductRegistry.get(PolicyType.CYBER);

    @Test
    void acceptsValidDetailsAndNormalisesThem() {
        var result = ClaimDetailsValidator.validate(twoWheeler, Map.of(
                "vehicleRegistrationNumber", "  KA-01-HX-4821 ",
                "incidentType", "accident",
                "thirdPartyInvolved", "false"));
        assertNull(result.error());
        assertEquals("KA-01-HX-4821", result.details().get("vehicleRegistrationNumber"));
        assertEquals("Accident", result.details().get("incidentType"));
        assertEquals(false, result.details().get("thirdPartyInvolved"));
    }

    @Test
    void rejectsMissingRequiredField() {
        var result = ClaimDetailsValidator.validate(twoWheeler, Map.of("incidentType", "Accident"));
        assertNotNull(result.error());
        assertTrue(result.error().toLowerCase().contains("vehicle registration"));
    }

    @Test
    void theftRequiresAPoliceReportButAccidentDoesNot() {
        var theft = ClaimDetailsValidator.validate(twoWheeler,
                Map.of("vehicleRegistrationNumber", "KA01AB1234", "incidentType", "Theft"));
        assertNotNull(theft.error());

        var theftWithReport = ClaimDetailsValidator.validate(twoWheeler, Map.of(
                "vehicleRegistrationNumber", "KA01AB1234", "incidentType", "Theft", "policeReportNumber", "FIR/221/2026"));
        assertNull(theftWithReport.error());

        var accident = ClaimDetailsValidator.validate(twoWheeler,
                Map.of("vehicleRegistrationNumber", "KA01AB1234", "incidentType", "Accident"));
        assertNull(accident.error());
    }

    @Test
    void booleanTriggerMakesDependentFieldRequired() {
        var missing = ClaimDetailsValidator.validate(cyber,
                Map.of("incidentType", "Identity theft", "reportedToCyberCell", true));
        assertNotNull(missing.error());

        var ok = ClaimDetailsValidator.validate(cyber, Map.of(
                "incidentType", "Identity theft", "reportedToCyberCell", true, "cyberCrimeReportNumber", "CC-99812"));
        assertNull(ok.error());
    }

    @Test
    void rejectsUnknownKeysBadSelectOptionsAndBadDates() {
        assertNotNull(ClaimDetailsValidator.validate(twoWheeler, Map.of(
                "vehicleRegistrationNumber", "KA01AB1234", "incidentType", "Accident", "isAdmin", "true")).error());
        assertNotNull(ClaimDetailsValidator.validate(twoWheeler, Map.of(
                "vehicleRegistrationNumber", "KA01AB1234", "incidentType", "Alien abduction")).error());
        assertNotNull(ClaimDetailsValidator.validate(travel, Map.of(
                "claimCategory", "Flight delay", "destination", "Singapore", "departureDate", "31/09/2026")).error());
    }

    @Test
    void rejectsOverlongText() {
        var result = ClaimDetailsValidator.validate(twoWheeler, Map.of(
                "vehicleRegistrationNumber", "x".repeat(ClaimDetailsValidator.MAX_TEXT_LENGTH + 1),
                "incidentType", "Accident"));
        assertNotNull(result.error());
    }
}
