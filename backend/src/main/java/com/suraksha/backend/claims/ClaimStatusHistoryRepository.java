package com.suraksha.backend.claims;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimStatusHistoryRepository extends JpaRepository<ClaimStatusHistory, UUID> {
    List<ClaimStatusHistory> findByClaimIdOrderByOccurredAtAsc(UUID claimId);
}
