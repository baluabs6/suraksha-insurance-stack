package com.suraksha.backend.claims.dto;

import com.suraksha.backend.claims.ClaimStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClaimStatusUpdateRequest {
    @NotNull(message = "Select the new status for this claim.")
    private ClaimStatus status;

    @Size(max = 1000, message = "Note must be under 1000 characters.")
    private String note;
}
