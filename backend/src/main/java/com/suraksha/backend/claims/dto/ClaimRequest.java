package com.suraksha.backend.claims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
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

    /** Type-specific fields for non-health policies; the allowed keys come from GET /api/products. */
    private Map<String, Object> details;

    // ---- Health-policy claims only (ignored for other policy types) ----
    private UUID memberId;

    @Size(max = 200)
    private String hospitalName;

    private LocalDate admissionDate;

    private LocalDate dischargeDate;

    @Size(max = 300)
    private String diagnosis;

    @Size(max = 120)
    private String treatingDoctor;

    private Boolean accidental;

    @PositiveOrZero private BigDecimal roomCharges;
    @PositiveOrZero private BigDecimal procedureCharges;
    @PositiveOrZero private BigDecimal medicineCharges;
    @PositiveOrZero private BigDecimal diagnosticCharges;
    @PositiveOrZero private BigDecimal otherCharges;
}
