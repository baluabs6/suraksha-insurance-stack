package com.suraksha.ai.client;

import com.suraksha.ai.advisor.UsageLoggingAdvisor;
import com.suraksha.ai.security.PiiRedactor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Facade over Spring AI's {@link ChatClient}. Every AI feature in this service goes through
 * here, which keeps three cross-cutting behaviours in one place:
 *
 *  - the "no API key -> canned, clearly-labelled fallback" contract the whole stack relies on,
 *  - PII redaction of anything sent as user content, and
 *  - prompts built as Message objects (not templates), so customer text containing
 *    "{" or "}" is never interpreted as a Spring AI prompt placeholder.
 *
 * The public method signatures are unchanged from the previous hand-rolled HTTP client, so the
 * existing controllers keep working; new code can use {@link #chatClient()} directly.
 */
@Component
@Slf4j
public class AnthropicClient {

    private static final int TEXT_MAX_TOKENS = 500;
    private static final int IMAGE_MAX_TOKENS = 700;

    private final ChatClient chatClient;
    private final String apiKey;

    public AnthropicClient(ChatClient.Builder builder,
                           UsageLoggingAdvisor usageLoggingAdvisor,
                           @Value("${anthropic.api-key:}") String apiKey) {
        this.chatClient = builder.defaultAdvisors(usageLoggingAdvisor).build();
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** The shared ChatClient, for features that need memory, tools or other advisors. */
    public ChatClient chatClient() {
        return chatClient;
    }

    public String complete(String systemPrompt, String userMessage, String fallback) {
        if (!isConfigured()) {
            log.warn("ANTHROPIC_API_KEY not set — returning fallback response instead of calling the model.");
            return fallback;
        }
        try {
            String text = chatClient.prompt(textPrompt(systemPrompt, userMessage)).call().content();
            return text == null || text.isBlank() ? fallback : text;
        } catch (Exception e) {
            log.error("Model call failed, using fallback response.", e);
            return fallback;
        }
    }

    public String completeWithImage(String systemPrompt, String userMessage,
                                    String base64Image, String mediaType, String fallback) {
        if (!isConfigured()) {
            log.warn("ANTHROPIC_API_KEY not set — returning fallback response instead of calling the model.");
            return fallback;
        }
        if (base64Image == null || base64Image.isBlank()) {
            log.warn("No image supplied to completeWithImage — returning fallback response.");
            return fallback;
        }
        try {
            String text = chatClient.prompt(imagePrompt(systemPrompt, userMessage, base64Image, mediaType))
                    .call().content();
            return text == null || text.isBlank() ? fallback : text;
        } catch (Exception e) {
            log.error("Model call failed, using fallback response.", e);
            return fallback;
        }
    }

    /**
     * Structured output: Spring AI appends the JSON schema for {@code type} to the prompt and
     * maps the reply onto it. Returns empty when the key is missing, the call fails, or the
     * reply doesn't fit the type — callers decide what their fallback looks like.
     */
    public <T> Optional<T> completeStructured(String systemPrompt, String userMessage, Class<T> type) {
        if (!isConfigured()) {
            log.warn("ANTHROPIC_API_KEY not set — skipping structured model call.");
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(chatClient.prompt(textPrompt(systemPrompt, userMessage)).call().entity(type));
        } catch (Exception e) {
            log.warn("Structured model call failed or returned an unparseable reply.", e);
            return Optional.empty();
        }
    }

    public <T> Optional<T> completeStructuredWithImage(String systemPrompt, String userMessage,
                                                       String base64Image, String mediaType, Class<T> type) {
        if (!isConfigured() || base64Image == null || base64Image.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(
                    chatClient.prompt(imagePrompt(systemPrompt, userMessage, base64Image, mediaType))
                            .call().entity(type));
        } catch (Exception e) {
            log.warn("Structured image call failed or returned an unparseable reply.", e);
            return Optional.empty();
        }
    }

    private static Prompt textPrompt(String systemPrompt, String userMessage) {
        return new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(PiiRedactor.redact(userMessage))),
                ChatOptions.builder().maxTokens(TEXT_MAX_TOKENS).build());
    }

    private static Prompt imagePrompt(String systemPrompt, String userMessage, String base64Image, String mediaType) {
        byte[] bytes = Base64.getMimeDecoder().decode(base64Image.trim());
        MimeType mime = MimeTypeUtils.parseMimeType(
                mediaType != null && !mediaType.isBlank() ? mediaType : "image/jpeg");
        UserMessage user = UserMessage.builder()
                .text(PiiRedactor.redact(userMessage))
                .media(List.of(new Media(mime, new ByteArrayResource(bytes))))
                .build();
        return new Prompt(
                List.of(new SystemMessage(systemPrompt), user),
                ChatOptions.builder().maxTokens(IMAGE_MAX_TOKENS).build());
    }
}
