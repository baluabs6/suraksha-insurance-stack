package com.suraksha.fraud.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "policies")
@Data
public class PolicyRecord {
    @Id
    private UUID id;
    private BigDecimal coverageAmount;

    /** Insurance type as the backend's enum name (HEALTH, MOTOR, TRAVEL...). Kept as text so new types never break reads. */
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
}
