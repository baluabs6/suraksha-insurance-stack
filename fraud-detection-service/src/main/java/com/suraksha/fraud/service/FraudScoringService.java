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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FraudScoringService {

    private final PolicyRecordRepository policyRecordRepository;
    private final ClaimRecordRepository claimRecordRepository;

    @Value("${fraud.amount-to-coverage-threshold}")
    private double amountToCoverageThreshold;

    @Value("${fraud.open-claims-threshold}")
    private int openClaimsThreshold;

    private List<TypeSpecificRules.Finding> typeSpecificFindings(ClaimSubmittedEvent event, ClaimRecord claim, PolicyRecord policy) {
        String policyType = policy == null ? null : policy.getType();
        Map<String, Object> details = claim.getDetails();

        long sameAsset = 0;
        long sameAssetSameDay = 0;
        String assetKey = TypeSpecificRules.assetKey(policyType);
        if (assetKey != null && details != null && details.get(assetKey) != null) {
            String value = TypeSpecificRules.normalizeIdentifier(details.get(assetKey).toString());
            if (!value.isEmpty()) {
                sameAsset = claimRecordRepository.countOtherClaimsWithAsset(event.claimId(), assetKey, value);
                if (claim.getIncidentDate() != null) {
                    sameAssetSameDay = claimRecordRepository.countOtherClaimsWithAssetOnDate(
                            event.claimId(), assetKey, value, claim.getIncidentDate());
                }
            }
        }

        long repeatCategory = 0;
        String category = TypeSpecificRules.category(details);
        if (category != null) {
            repeatCategory = claimRecordRepository.countOtherClaimsOnPolicyWithCategory(event.policyId(), event.claimId(), category);
        }

        return TypeSpecificRules.evaluate(new TypeSpecificRules.Input(
                policyType,
                policy == null ? null : policy.getStartDate(),
                policy == null ? null : policy.getEndDate(),
                claim.getIncidentDate(), claim.getSubmittedAt(), details,
                sameAsset, sameAssetSameDay, repeatCategory));
    }

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

        if (event.claimAmount().remainder(BigDecimal.valueOf(1000)).compareTo(BigDecimal.ZERO) == 0) {
            score += 0.05;
        }

        ClaimRecord claim = claimRecordRepository.findById(event.claimId()).orElse(null);
        if (claim != null) {
            for (TypeSpecificRules.Finding finding : typeSpecificFindings(event, claim, policy)) {
                flags.add(finding.flag());
                score += finding.score();
            }
        }

        score = Math.min(score, 1.0);

        String level = score >= 0.6 ? "HIGH" : score >= 0.3 ? "MEDIUM" : "LOW";
        return new ScoringResult(score, level, flags);
    }
}
