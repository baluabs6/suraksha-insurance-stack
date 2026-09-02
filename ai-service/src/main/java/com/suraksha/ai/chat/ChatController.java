package com.suraksha.ai.chat;

import com.suraksha.ai.client.AnthropicClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final String SYSTEM_PROMPT = """
            You are Suraksha's customer support assistant for an Indian insurance
            platform covering health, motor, and life policies. Answer questions
            about how policies, claims, and payments generally work. Use the
            customer's policy summary below when it's relevant to their question.

            Rules:
            - Never confirm or deny whether a specific claim will be approved —
              that's a human adjuster's decision, not yours.
            - Never quote a premium, payout amount, or timeline you weren't given.
            - If the question needs a human (a specific claim dispute, a complaint,
              anything about their exact case), tell them to contact support or use
              the claims section of the app instead of guessing.
            - Keep answers to 3-4 sentences.
            """;

    private final AnthropicClient anthropicClient;

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest req) {
        List<String> policies = req.getPolicySummaries() == null ? List.of() : req.getPolicySummaries();
        String context = policies.isEmpty()
                ? "The customer has no policies on file yet."
                : "The customer's policies: " + String.join("; ", policies);

        String userMessage = context + "\n\nCustomer question: " + req.getMessage();

        String fallback = "I can't reach the assistant service right now "
                + "(ANTHROPIC_API_KEY not configured on ai-service). "
                + "For anything urgent, please use the claims section of the app or contact support directly.";

        String reply = anthropicClient.complete(SYSTEM_PROMPT, userMessage, fallback);
        return new ChatResponse(reply);
    }
}
