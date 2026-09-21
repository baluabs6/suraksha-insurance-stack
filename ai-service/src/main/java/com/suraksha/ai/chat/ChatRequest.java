package com.suraksha.ai.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    @NotBlank(message = "Enter a message.")
    private String message;

    private List<String> policySummaries;
}
