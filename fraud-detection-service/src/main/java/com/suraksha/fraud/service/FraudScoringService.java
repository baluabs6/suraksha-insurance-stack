package com.suraksha.fraud.service;

import com.suraksha.fraud.events.ClaimSubmittedEvent;
import com.suraksha.fraud.model.ClaimRecord;
import com.suraksha.fraud.model.ClaimStatusView;
import com.suraksha.fraud.model.PolicyRecord;
import com.suraksha.fraud.repository.ClaimRecordRepository;
import com.suraksha.fraud.repository.PolicyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based risk scoring, deliberately simple and explainable — this is the
 * kind of thing a real fraud model would eventually replace (XGBoost/scikit-
 * learn trained on historical claims), but the same shape applies: pull
 * signals, produce a 0–1 score, classify into a risk band, and always keep
 * a list of *why* it was flagged, since insurers need to justify a manual
 * review decision, not just show a number.
 */
@Service
@RequiredArgsConstructor
public class FraudScoringService {

    private final PolicyRecordRepository policyRecordRepository;
    private final ClaimRecordRepository claimRecordRepository;

    @Value("${fraud.amount-to-coverage-threshold}")
    private double amountToCoverageThreshold;

    @Value("${fraud.open-claims-threshold}")
    private int openClaimsThreshold;

    public record ScoringResult(double riskScore, String riskLevel, List<String> flags) {
    }

    public ScoringResult score(ClaimSubmittedEvent event) {
        List<String> flags = new ArrayList<>();
        double score = 0.0;

        PolicyRecord policy = policyRecordRepository.findById(event.policyId()).orElse(null);
        if (policy != null && policy.getCoverageAmount() != null
                && policy.getCoverageAmount().compareTo(BigDecimal.ZERO) > 0) {
            double ratio = event.claimAmount().doubleValue() / policy.getCoverageAmount().doubleValue();
            if (ratio >= amountToCoverageThreshold) {
                flags.add("amount_high_relative_to_coverage");
                score += 0.45;
            }
        }

        long openClaimsOnPolicy = claimRecordRepository
                .findByPolicyIdAndStatusNot(event.policyId(), ClaimStatusView.SETTLED)
                .stream()
                .filter(c -> !c.getId().equals(event.claimId()))
                .count();
        if (openClaimsOnPolicy >= openClaimsThreshold) {
            flags.add("multiple_open_claims_on_policy");
            score += 0.35;
        }

        // A round-number claim amount is a very weak heuristic signal on its own,
        // but combined with the others it nudges the score — a real model would
        // learn a proper weighting instead of these hand-picked increments.
        if (event.claimAmount().remainder(BigDecimal.valueOf(1000)).compareTo(BigDecimal.ZERO) == 0) {
            score += 0.05;
        }

        score = Math.min(score, 1.0);

        String level = score >= 0.6 ? "HIGH" : score >= 0.3 ? "MEDIUM" : "LOW";
        return new ScoringResult(score, level, flags);
    }
}
