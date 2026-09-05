package com.suraksha.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin wrapper around the Anthropic Messages API. Every caller goes through
 * complete(systemPrompt, userMessage) and gets a plain string back — callers
 * never see whether that came from a real model call or the no-key fallback,
 * which keeps the triage/chat/recommendation code simple.
 */
@Component
@Slf4j
public class AnthropicClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${anthropic.api-key}")
    private String apiKey;

    @Value("${anthropic.model}")
    private String model;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String complete(String systemPrompt, String userMessage, String fallback) {
        if (!isConfigured()) {
            log.warn("ANTHROPIC_API_KEY not set — returning fallback response instead of calling the API.");
            return fallback;
        }
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 500);
            body.put("system", systemPrompt);
            ArrayNode messages = body.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            return send(body, fallback);
        } catch (Exception e) {
            log.error("Anthropic API call failed, using fallback response.", e);
            return fallback;
        }
    }

    /**
     * Same contract as complete(), but attaches a base64-encoded image
     * alongside the text prompt using Claude's vision input. Used by the
     * claim document/photo analysis feature — nothing else in this service
     * needs image input, so this stays a separate method rather than
     * complicating the plain-text path every other caller uses.
     */
    public String completeWithImage(String systemPrompt, String userMessage,
                                     String base64Image, String mediaType, String fallback) {
        if (!isConfigured()) {
            log.warn("ANTHROPIC_API_KEY not set — returning fallback response instead of calling the API.");
            return fallback;
        }
        if (base64Image == null || base64Image.isBlank()) {
            log.warn("No image supplied to completeWithImage — returning fallback response.");
            return fallback;
        }
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 700);
            body.put("system", systemPrompt);
            ArrayNode messages = body.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            ArrayNode contentBlocks = userMsg.putArray("content");

            ObjectNode imageBlock = contentBlocks.addObject();
            imageBlock.put("type", "image");
            ObjectNode source = imageBlock.putObject("source");
            source.put("type", "base64");
            source.put("media_type", mediaType != null && !mediaType.isBlank() ? mediaType : "image/jpeg");
            source.put("data", base64Image);

            ObjectNode textBlock = contentBlocks.addObject();
            textBlock.put("type", "text");
            textBlock.put("text", userMessage);

            return send(body, fallback);
        } catch (Exception e) {
            log.error("Anthropic API call failed, using fallback response.", e);
            return fallback;
        }
    }

    private String send(ObjectNode body, String fallback) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Anthropic API returned {}: {}", response.statusCode(), response.body());
            return fallback;
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode content = root.path("content");
        StringBuilder text = new StringBuilder();
        if (content.isArray()) {
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    text.append(block.path("text").asText());
                }
            }
        }
        return text.length() > 0 ? text.toString() : fallback;
    }
}
