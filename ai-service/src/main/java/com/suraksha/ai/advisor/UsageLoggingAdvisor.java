package com.suraksha.ai.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Outermost advisor on every ChatClient call: logs model, token usage and latency in one
 * line so cost and slow calls are visible per request. It only reads the response — it
 * never alters what the model said, so it can't affect claim-decision guardrails.
 */
@Component
@Slf4j
public class UsageLoggingAdvisor implements CallAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long start = System.nanoTime();
        ChatClientResponse response = chain.nextCall(request);
        long ms = (System.nanoTime() - start) / 1_000_000;

        ChatResponse chatResponse = response.chatResponse();
        Object model = "unknown";
        Object promptTokens = "n/a";
        Object completionTokens = "n/a";
        if (chatResponse != null && chatResponse.getMetadata() != null) {
            model = chatResponse.getMetadata().getModel();
            Usage usage = chatResponse.getMetadata().getUsage();
            if (usage != null) {
                promptTokens = usage.getPromptTokens();
                completionTokens = usage.getCompletionTokens();
            }
        }
        log.info("ai-call model={} promptTokens={} completionTokens={} latencyMs={}",
                model, promptTokens, completionTokens, ms);
        return response;
    }

    @Override
    public String getName() {
        return "UsageLoggingAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
