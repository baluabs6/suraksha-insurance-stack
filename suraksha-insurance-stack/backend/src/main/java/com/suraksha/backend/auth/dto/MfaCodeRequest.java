package com.suraksha.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MfaCodeRequest {
    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits.")
    private String code;
}
