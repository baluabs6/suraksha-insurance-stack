package com.suraksha.ai.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    @NotBlank(message = "Enter a message.")
    private String message;

    /** Optional, and only kept for older clients: the server now looks policies up itself via tools. */
    private List<String> policySummaries;

    /** Client-generated id that groups messages into one conversation (memory is per user + id). */
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$", message = "conversationId may only contain letters, digits, '-' and '_' (max 64).")
    private String conversationId;
}
