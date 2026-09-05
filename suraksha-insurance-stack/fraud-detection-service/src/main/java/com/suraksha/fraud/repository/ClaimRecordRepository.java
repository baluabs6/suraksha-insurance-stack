package com.suraksha.fraud.repository;

import com.suraksha.fraud.model.ClaimRecord;
import com.suraksha.fraud.model.ClaimStatusView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimRecordRepository extends JpaRepository<ClaimRecord, UUID> {
    List<ClaimRecord> findByPolicyIdAndStatusNot(UUID policyId, ClaimStatusView excludedStatus);
}
