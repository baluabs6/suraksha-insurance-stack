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

        return signals;
    }
}
