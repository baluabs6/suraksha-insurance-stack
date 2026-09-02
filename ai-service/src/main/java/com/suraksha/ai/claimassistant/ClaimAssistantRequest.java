package com.suraksha.ai.claimassistant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ClaimAssistantRequest {
    @NotBlank(message = "Describe what happened.")
    private String narrative;

    // The customer's own policies, sent by the frontend the same way the
    // chatbot/recommendation endpoints get their context — see the note in
    // ChatRequest about why this trusts client-supplied context in the demo.
    private List<PolicyOption> policies;

    @Data
    public static class PolicyOption {
        private String id;
        private String type;         // HEALTH / MOTOR / LIFE
        private String policyNumber;
        private String planName;
    }
}
