package com.suraksha.backend.grievance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class GrievanceRequest {
    @NotBlank(message = "Select a category for this complaint.")
    private String category;

    @NotBlank(message = "Describe the issue.")
    private String description;

    private UUID relatedClaimId;
    private UUID relatedPolicyId;
}
