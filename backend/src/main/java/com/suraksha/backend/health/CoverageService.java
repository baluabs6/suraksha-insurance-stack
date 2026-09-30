package com.suraksha.backend.health;

import com.suraksha.backend.claims.Claim;
import com.suraksha.backend.claims.ClaimRepository;
import com.suraksha.backend.claims.ClaimStatus;
import com.suraksha.backend.health.dto.CoverageSummary;
import com.suraksha.backend.policy.Policy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoverageService {

    static final int DEFAULT_INITIAL_WAITING_DAYS = 30;
    static final int DEFAULT_PRE_EXISTING_WAITING_MONTHS = 24;

    private final ClaimRepository claimRepository;

    public CoverageSummary summarize(Policy policy) {
        List<Claim> claims = claimRepository.findByPolicyIdAndStatusIn(policy.getId(),
                List.of(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, ClaimStatus.APPROVED, ClaimStatus.SETTLED));

        BigDecimal used = BigDecimal.ZERO;
        BigDecimal pending = BigDecimal.ZERO;
        for (Claim c : claims) {
            BigDecimal counted = c.getEligibleAmount() != null ? c.getEligibleAmount() : c.getClaimAmount();
            if (c.getStatus() == ClaimStatus.APPROVED || c.getStatus() == ClaimStatus.SETTLED) {
                used = used.add(counted);
            } else {
                pending = pending.add(counted);
            }
        }
        BigDecimal remaining = policy.getCoverageAmount().subtract(used).subtract(pending).max(BigDecimal.ZERO);

        return new CoverageSummary(policy.getCoverageAmount(), used, pending, remaining,
                policy.getRoomRentCapPerDay(),
                policy.getCoPayPercent() == null ? 0 : policy.getCoPayPercent(),
                policy.getInitialWaitingDays() == null ? DEFAULT_INITIAL_WAITING_DAYS : policy.getInitialWaitingDays(),
                policy.getPreExistingWaitingMonths() == null
                        ? DEFAULT_PRE_EXISTING_WAITING_MONTHS : policy.getPreExistingWaitingMonths());
    }
}
