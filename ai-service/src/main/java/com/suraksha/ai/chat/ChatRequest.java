package com.suraksha.ai.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    @NotBlank(message = "Enter a message.")
    private String message;

    // The frontend already has the logged-in user's policies/claims loaded
    // (fetched from the authenticated backend), so it passes a short summary
    // here rather than this service needing its own auth/user lookup.
    // In production, put this endpoint behind the API gateway like the
    // others, validate the JWT there, and look policies up server-side
    // instead of trusting client-supplied context.
    private List<String> policySummaries;
}
