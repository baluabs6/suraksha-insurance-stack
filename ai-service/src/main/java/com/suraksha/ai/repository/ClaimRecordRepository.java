package com.suraksha.ai.repository;

import com.suraksha.ai.model.ClaimRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimRecordRepository extends JpaRepository<ClaimRecord, UUID> {

    // Used by the adjuster Q&A feature to scope retrieval to a specific
    // policy instead of handing the model every claim in the table.
    List<ClaimRecord> findByPolicyIdOrderByIncidentDateDesc(UUID policyId);

    List<ClaimRecord> findByRiskLevelOrderByIncidentDateDesc(String riskLevel);

    List<ClaimRecord> findTop25ByOrderByIncidentDateDesc();
}
