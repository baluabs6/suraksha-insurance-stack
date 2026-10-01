package com.suraksha.fraud.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fraud rules that depend on the insurance type. Pure functions of their input, so every rule can be unit-tested
 * without a database. Each finding adds to the claim's risk score; none of them decides a claim: they only
 * raise it for a human adjuster to look at.
 */
public final class TypeSpecificRules {

    public static final String INCIDENT_OUTSIDE_POLICY_PERIOD = "incident_outside_policy_period";
    public static final String THEFT_SOON_AFTER_INCEPTION = "theft_soon_after_inception";
    public static final String REPORTED_LATE = "reported_late";
    public static final String SAME_ASSET_CLAIMED_AGAIN = "same_asset_claimed_again";
    public static final String SAME_ASSET_SAME_DAY_CLAIM = "same_asset_same_day_claim";
    public static final String REPEAT_CLAIM_CATEGORY_ON_POLICY = "repeat_claim_category_on_policy";
    public static final String CYBER_INCIDENT_NOT_REPORTED = "cyber_incident_not_reported";

    static final int EARLY_THEFT_WINDOW_DAYS = 30;
    static final int LATE_REPORT_DAYS = 30;
    static final int REPEAT_CATEGORY_THRESHOLD = 2;

    /** Details key holding the identifier of the insured asset, per type. */
    public static String assetKey(String policyType) {
        if (policyType == null) return null;
        return switch (policyType) {
            case "MOTOR", "TWO_WHEELER" -> "vehicleRegistrationNumber";
            case "GADGET" -> "serialOrImei";
            case "MARINE_CARGO" -> "shipmentReference";
            default -> null;
        };
    }

    public record Input(String policyType, LocalDate policyStart, LocalDate policyEnd, LocalDate incidentDate,
                        Instant submittedAt, Map<String, Object> details,
                        long sameAssetClaims, long sameAssetSameDayClaims, long repeatCategoryOnPolicy) {}

    public record Finding(String flag, double score) {}

    private TypeSpecificRules() {}

    public static List<Finding> evaluate(Input in) {
        List<Finding> findings = new ArrayList<>();
        if (in.incidentDate() == null) return findings;
        Map<String, Object> details = in.details() == null ? Map.of() : in.details();
        boolean health = "HEALTH".equals(in.policyType());

        if ((in.policyStart() != null && in.incidentDate().isBefore(in.policyStart()))
                || (in.policyEnd() != null && in.incidentDate().isAfter(in.policyEnd()))) {
            findings.add(new Finding(INCIDENT_OUTSIDE_POLICY_PERIOD, 0.50));
        } else if (isTheftLike(details) && in.policyStart() != null
                && ChronoUnit.DAYS.between(in.policyStart(), in.incidentDate()) <= EARLY_THEFT_WINDOW_DAYS) {
            findings.add(new Finding(THEFT_SOON_AFTER_INCEPTION, 0.25));
        }

        if (!health && in.submittedAt() != null) {
            long daysToReport = ChronoUnit.DAYS.between(in.incidentDate(), in.submittedAt().atZone(ZoneOffset.UTC).toLocalDate());
            if (daysToReport > LATE_REPORT_DAYS) {
                findings.add(new Finding(REPORTED_LATE, 0.10));
            }
        }

        if (in.sameAssetSameDayClaims() > 0) {
            findings.add(new Finding(SAME_ASSET_SAME_DAY_CLAIM, 0.45));
        } else if (in.sameAssetClaims() > 0) {
            findings.add(new Finding(SAME_ASSET_CLAIMED_AGAIN, 0.25));
        }

        if (in.repeatCategoryOnPolicy() >= REPEAT_CATEGORY_THRESHOLD) {
            findings.add(new Finding(REPEAT_CLAIM_CATEGORY_ON_POLICY, 0.20));
        }

        if ("CYBER".equals(in.policyType()) && isReportableCyberIncident(details)
                && !Boolean.TRUE.equals(details.get("reportedToCyberCell"))) {
            findings.add(new Finding(CYBER_INCIDENT_NOT_REPORTED, 0.20));
        }
        return findings;
    }

    /** What happened, taken from whichever detail field the product uses for it. */
    public static String category(Map<String, Object> details) {
        if (details == null) return null;
        for (String key : List.of("claimCategory", "incidentType", "damageType", "lossType", "injuryType")) {
            Object v = details.get(key);
            if (v != null && !v.toString().isBlank()) return v.toString();
        }
        return null;
    }

    static boolean isTheftLike(Map<String, Object> details) {
        String c = category(details);
        if (c == null) return false;
        String lower = c.toLowerCase(Locale.ROOT);
        return lower.contains("theft") || lower.contains("burglary") || lower.contains("pilferage");
    }

    static boolean isReportableCyberIncident(Map<String, Object> details) {
        String c = category(details);
        if (c == null) return false;
        String lower = c.toLowerCase(Locale.ROOT);
        return lower.contains("fraud") || lower.contains("identity theft") || lower.contains("ransomware");
    }

    /** Upper-case letters and digits only, so "KA-01 hx 4821" and "ka01hx4821" compare equal. */
    public static String normalizeIdentifier(String raw) {
        return raw == null ? "" : raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }
}
