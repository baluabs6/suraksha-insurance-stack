package com.suraksha.ai.model;

import jakarta.persistence.*;
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

    @Column(name = "user_id")
    private UUID userId;

    private String policyNumber;
    private String planName;

    @Enumerated(EnumType.STRING)
    private PolicyTypeView type;

    private BigDecimal coverageAmount;
    private BigDecimal premium;
    private LocalDate startDate;
    private LocalDate endDate;
}
