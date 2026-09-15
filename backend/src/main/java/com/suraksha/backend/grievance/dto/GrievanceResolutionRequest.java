package com.suraksha.backend.grievance.dto;

import com.suraksha.backend.grievance.GrievanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GrievanceResolutionRequest {
    @NotNull(message = "Select the new status.")
    private GrievanceStatus status;

    @NotBlank(message = "Add a note explaining the update.")
    private String note;
}
