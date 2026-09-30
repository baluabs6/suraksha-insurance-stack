package com.suraksha.ai.chat;

import com.suraksha.ai.security.PromptGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class ChatController {

    private final SupportChatService supportChatService;

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest req, Authentication authentication) {
        String fallback = "I can't reach the assistant service right now "
                + "(ANTHROPIC_API_KEY not configured on ai-service). "
                + "For anything urgent, please use the claims section of the app or contact support directly.";

        String rawReply = supportChatService
                .reply(authentication.getName(), req.getConversationId(), req.getMessage(), req.getPolicySummaries())
                .orElse(fallback);

        String safeFallback = "I can't answer that the way it was phrased — for anything about a specific "
                + "claim's outcome, please check the claims section of the app or contact support.";
        return new ChatResponse(PromptGuard.enforceNoDecisionLanguage(rawReply, safeFallback));
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
}
