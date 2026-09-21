package com.suraksha.ai.claimassistant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ClaimAssistantRequest {
    @NotBlank(message = "Describe what happened.")
    private String narrative;

    private List<PolicyOption> policies;

    @Data
    public static class PolicyOption {
        private String id;
        private String type;
        private String policyNumber;
        private String planName;
    }
}
