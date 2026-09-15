package com.suraksha.backend.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {
    List<Policy> findByUserId(UUID userId);
    List<Policy> findByAgentIdOrderByStartDateDesc(UUID agentId);
    List<Policy> findByEndDateBetweenAndStatus(LocalDate from, LocalDate to, PolicyStatus status);
}
