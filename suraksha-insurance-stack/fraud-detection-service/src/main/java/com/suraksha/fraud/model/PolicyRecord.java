package com.suraksha.fraud.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "policies")
@Data
public class PolicyRecord {
    @Id
    private UUID id;
    private BigDecimal coverageAmount;
}
