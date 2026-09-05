package com.suraksha.backend.claims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ClaimRequest {
    @NotNull(message = "Select which policy this claim is against.")
    private UUID policyId;

    @NotNull(message = "Enter the amount you're claiming.")
    @Positive(message = "Claim amount must be greater than zero.")
    private BigDecimal claimAmount;

    @NotNull(message = "Enter when the incident happened.")
    private LocalDate incidentDate;

    @NotBlank(message = "Give a short description of what happened.")
    private String description;
}
