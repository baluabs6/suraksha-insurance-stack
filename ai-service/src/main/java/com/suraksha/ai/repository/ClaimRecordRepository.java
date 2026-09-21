package com.suraksha.ai.repository;

import com.suraksha.ai.model.ClaimRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimRecordRepository extends JpaRepository<ClaimRecord, UUID> {

    List<ClaimRecord> findByPolicyIdOrderByIncidentDateDesc(UUID policyId);

    List<ClaimRecord> findByRiskLevelOrderByIncidentDateDesc(String riskLevel);

    List<ClaimRecord> findTop25ByOrderByIncidentDateDesc();
}
