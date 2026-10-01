package com.suraksha.ai.fraudexplain;

import com.suraksha.ai.model.ClaimRecord;
import com.suraksha.ai.model.ClaimStatusView;
import com.suraksha.ai.model.PolicyRecord;
import com.suraksha.ai.repository.ClaimRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FraudSignalExplainer {

    private final ClaimRecordRepository claimRecordRepository;

    @Value("${fraud.amount-to-coverage-threshold}")
    private double amountToCoverageThreshold;

    @Value("${fraud.open-claims-threshold}")
    private int openClaimsThreshold;

    public record Signal(String code, String plainLanguage) {
    }

    public List<Signal> explainSignals(ClaimRecord claim, PolicyRecord policy) {
        List<Signal> signals = new ArrayList<>();

        if (policy != null && policy.getCoverageAmount() != null
                && policy.getCoverageAmount().compareTo(BigDecimal.ZERO) > 0) {
            double ratio = claim.getClaimAmount().doubleValue() / policy.getCoverageAmount().doubleValue();
            if (ratio >= amountToCoverageThreshold) {
                signals.add(new Signal("amount_high_relative_to_coverage",
                        "The claim amount is %.0f%% of the policy's total coverage.".formatted(ratio * 100)));
            }
        }

        long openClaimsOnPolicy = claimRecordRepository.findByPolicyIdOrderByIncidentDateDesc(claim.getPolicyId())
                .stream()
                .filter(c -> !c.getId().equals(claim.getId()))
                .filter(c -> c.getStatus() != ClaimStatusView.SETTLED)
                .count();
        if (openClaimsOnPolicy >= openClaimsThreshold) {
            signals.add(new Signal("multiple_open_claims_on_policy",
                    "There are %d other open (non-settled) claim(s) on this same policy.".formatted(openClaimsOnPolicy)));
        }

        if (claim.getClaimAmount().remainder(BigDecimal.valueOf(1000)).compareTo(BigDecimal.ZERO) == 0) {
            signals.add(new Signal("round_number_amount",
                    "The claim amount is an exact round number, a weak signal on its own."));
        }

        // Type-specific rules are evaluated by fraud-detection-service, which stores which ones fired on the claim.
        if (claim.getRiskFlags() != null && !claim.getRiskFlags().isBlank()) {
            for (String code : claim.getRiskFlags().split(",")) {
                String description = TYPE_SPECIFIC_FLAGS.get(code.trim());
                if (description != null) {
                    signals.add(new Signal(code.trim(), description));
                }
            }
        }

        return signals;
    }

    private static final Map<String, String> TYPE_SPECIFIC_FLAGS = Map.of(
            "incident_outside_policy_period", "The incident date falls outside the policy's start and end dates.",
            "theft_soon_after_inception", "A theft or burglary was reported within 30 days of the policy starting.",
            "reported_late", "The claim was submitted more than 30 days after the incident.",
            "same_asset_claimed_again", "The same insured item (vehicle, device or shipment) appears on another claim.",
            "same_asset_same_day_claim", "The same insured item appears on another claim with the same incident date.",
            "repeat_claim_category_on_policy", "This is at least the third claim of the same kind on this policy.",
            "cyber_incident_not_reported", "A cyber incident was claimed, but it was not reported to the cyber crime portal or police.");
}
