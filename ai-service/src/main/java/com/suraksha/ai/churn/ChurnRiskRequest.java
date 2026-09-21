package com.suraksha.ai.churn;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ChurnRiskRequest {
    @NotBlank(message = "policyId is required.")
    private String policyId;

    private String planName;
    private String type;
    private BigDecimal premium;
    private LocalDate startDate;
    private LocalDate endDate;

    private int totalClaimsFiled;
    private int highRiskClaims;
    private int mediumRiskClaims;
    private int rejectedClaims;
    private int openGrievances;
}
