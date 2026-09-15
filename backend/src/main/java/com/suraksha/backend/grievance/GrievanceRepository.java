package com.suraksha.backend.grievance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GrievanceRepository extends JpaRepository<Grievance, UUID> {
    List<Grievance> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Grievance> findAllByOrderByCreatedAtDesc();
    List<Grievance> findByStatusOrderByCreatedAtDesc(GrievanceStatus status);
}
