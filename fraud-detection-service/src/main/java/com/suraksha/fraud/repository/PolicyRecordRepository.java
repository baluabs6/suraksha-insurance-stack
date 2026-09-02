package com.suraksha.fraud.repository;

import com.suraksha.fraud.model.PolicyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PolicyRecordRepository extends JpaRepository<PolicyRecord, UUID> {
}
