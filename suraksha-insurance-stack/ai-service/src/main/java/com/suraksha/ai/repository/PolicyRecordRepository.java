package com.suraksha.ai.repository;

import com.suraksha.ai.model.PolicyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PolicyRecordRepository extends JpaRepository<PolicyRecord, UUID> {
    List<PolicyRecord> findByUserId(UUID userId);
}
