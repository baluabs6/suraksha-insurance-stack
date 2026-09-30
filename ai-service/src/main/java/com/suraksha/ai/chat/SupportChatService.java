package com.suraksha.ai.chat;

import com.suraksha.ai.client.AnthropicClient;
import com.suraksha.ai.security.PiiRedactor;
import com.suraksha.ai.security.PromptGuard;
import com.suraksha.ai.tools.SupportTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Support chatbot built on Spring AI: conversation memory (JDBC-backed, so it is shared across
 * ai-service replicas) plus read-only tools for the customer's own policies and claims.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupportChatService {

    static final Pattern VALID_CONVERSATION_ID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final String DEFAULT_CONVERSATION = "default";
    private static final int MAX_TOKENS = 600;

    static final String SYSTEM_PROMPT = PromptGuard.ANTI_INJECTION_PREAMBLE + """

            You are Suraksha's customer support assistant for an Indian insurance
            platform covering health, motor, and life policies. Answer questions
            about how policies, claims, and payments generally work.

            You can look up the signed-in customer's own policies and claims with
            your tools. Use them whenever the question is about the customer's
            own cover or claims, and treat tool results as more reliable than any
            summary text in the conversation. You can only see this customer's
            data; if they ask about anyone else's, say you can't help with that.

            Rules:
            - You may report a claim's currently recorded status exactly as the
              tool returns it. Never predict whether a claim will be approved,
              rejected, or paid, and never say how a claim "should" be decided —
              that's a human adjuster's decision, not yours.
            - Never quote a premium, payout amount, or timeline you weren't given
              by the customer or by a tool result.
            - If the question needs a human (a specific claim dispute, a complaint,
              anything you can't resolve from the tools), tell them to contact
              support or use the claims section of the app instead of guessing.
            - Keep answers to 3-4 sentences.
            """;

    private final AnthropicClient anthropicClient;
    private final ChatMemory chatMemory;
    private final SupportTools supportTools;

    /** Empty when the key isn't configured or the call failed; the controller supplies the fallback text. */
    public Optional<String> reply(String userId, String conversationId, String message, List<String> policySummaries) {
        if (!anthropicClient.isConfigured()) {
            return Optional.empty();
        }
        String memoryKey = memoryKey(userId, conversationId);

        StringBuilder user = new StringBuilder();
        if (policySummaries != null && !policySummaries.isEmpty()) {
            user.append("Policy summary shown in the app (may be out of date): ")
                    .append(PromptGuard.wrapUntrusted(String.join("; ", policySummaries)))
                    .append("\n\n");
        }
        user.append("Customer question: ").append(PromptGuard.wrapUntrusted(message));

        // Redact before the message is sent *and* before the memory advisor persists it.
        Prompt prompt = new Prompt(
                List.of(new SystemMessage(SYSTEM_PROMPT), new UserMessage(PiiRedactor.redact(user.toString()))),
                ChatOptions.builder().maxTokens(MAX_TOKENS).build());

        try {
            String text = anthropicClient.chatClient().prompt(prompt)
                    .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, memoryKey))
                    .tools(supportTools)
                    .toolContext(Map.of(SupportTools.USER_ID_KEY, userId))
                    .call()
                    .content();
            return Optional.ofNullable(text).filter(t -> !t.isBlank());
        } catch (Exception e) {
            log.error("Support chat call failed.", e);
            return Optional.empty();
        }
    }

    public void clearConversation(String userId, String conversationId) {
        chatMemory.clear(memoryKey(userId, conversationId));
    }

    public static boolean isValidConversationId(String id) {
        return id == null || VALID_CONVERSATION_ID.matcher(id).matches();
    }

    /** Memory is namespaced by the authenticated user so one customer can never read another's history. */
    static String memoryKey(String userId, String conversationId) {
        String id = conversationId == null || conversationId.isBlank() ? DEFAULT_CONVERSATION : conversationId;
        if (!VALID_CONVERSATION_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid conversation id");
        }
        return userId + ":" + id;
    }
}
