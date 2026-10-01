package com.suraksha.backend.claims;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {
    List<Claim> findByUserIdOrderBySubmittedAtDesc(UUID userId);
    List<Claim> findByPolicyIdAndStatusIn(UUID policyId, Collection<ClaimStatus> statuses);
    boolean existsByMemberId(UUID memberId);
    List<Claim> findByStatusIn(Collection<ClaimStatus> statuses);
}
