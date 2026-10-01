package com.suraksha.ai.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suraksha.ai.security.AiRateLimiter;
import com.suraksha.ai.security.PromptGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class ChatController {

    static final String UNAVAILABLE = "I can't reach the assistant service right now "
            + "(ANTHROPIC_API_KEY not configured on ai-service). "
            + "For anything urgent, please use the claims section of the app or contact support directly.";

    static final String SAFE_FALLBACK = "I can't answer that the way it was phrased — for anything about a specific "
            + "claim's outcome, please check the claims section of the app or contact support.";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SupportChatService supportChatService;
    private final TranslationService translationService;
    private final AiRateLimiter rateLimiter;

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest req, Authentication authentication) {
        requireWithinLimits(authentication);

        String english = supportChatService
                .reply(authentication.getName(), req.getConversationId(), req.getMessage(), req.getPolicySummaries())
                .orElse(UNAVAILABLE);

        // The guard reads English, so it runs before translation, never after.
        String checked = PromptGuard.enforceNoDecisionLanguage(english, SAFE_FALLBACK);
        return new ChatResponse(translationService.translate(checked, SupportedLanguage.fromCode(req.getLanguage())));
    }

    /**
     * Server-sent events (each event's data is a JSON string, so spaces and newlines in the text survive intact): "token" events carry text as it is generated. When the stream ends a final "done" event
     * carries the complete, guard-checked text; clients replace what they showed with it. If the guard rejects the
     * reply, the "done" text is the safe fallback, so a streamed answer can never keep decision language.
     * English only: other languages use the non-streaming endpoint, which translates after the check.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@Valid @RequestBody ChatRequest req, Authentication authentication) {
        requireWithinLimits(authentication);
        if (SupportedLanguage.fromCode(req.getLanguage()) != SupportedLanguage.EN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Streaming is only available in English.");
        }

        StringBuilder full = new StringBuilder();
        Flux<ServerSentEvent<String>> tokens = supportChatService
                .stream(authentication.getName(), req.getConversationId(), req.getMessage(), req.getPolicySummaries())
                .doOnNext(full::append)
                .map(chunk -> ServerSentEvent.<String>builder().event("token").data(json(chunk)).build())
                .onErrorResume(e -> Flux.empty());

        Mono<ServerSentEvent<String>> done = Mono.fromSupplier(() -> {
            String text = full.length() == 0 ? UNAVAILABLE : full.toString();
            return ServerSentEvent.<String>builder().event("done")
                    .data(json(PromptGuard.enforceNoDecisionLanguage(text, SAFE_FALLBACK))).build();
        });
        return tokens.concatWith(done);
    }

    /** "New chat": forgets the stored history for this user's conversation. */
    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@PathVariable String conversationId, Authentication authentication) {
        if (!SupportChatService.isValidConversationId(conversationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid conversation id.");
        }
        supportChatService.clearConversation(authentication.getName(), conversationId);
    }

    private static String json(String text) {
        try {
            return JSON.writeValueAsString(text);
        } catch (JsonProcessingException e) {
            return "\"\"";
        }
    }

    private void requireWithinLimits(Authentication authentication) {
        if (!rateLimiter.tryAcquire(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "You've reached the limit for assistant messages. Please try again in a little while.");
        }
    }
}
