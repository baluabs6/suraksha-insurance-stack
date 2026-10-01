package com.suraksha.ai.chat;

import com.suraksha.ai.client.AnthropicClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Translates an already-checked English reply. The support bot always answers in English first so the
 * "no claim decisions" guard (which reads English) runs on the real text; only then is the reply translated.
 * If translation fails the customer gets the English reply rather than nothing.
 */
@Service
@RequiredArgsConstructor
public class TranslationService {

    private static final String SYSTEM_PROMPT = """
            You are a translator for an insurance customer-support chat. Translate the user's message into %s.
            Translate faithfully and completely. Do not add, remove or soften anything, do not add advice or opinions,
            and keep numbers, rupee amounts, dates and policy numbers exactly as written. Output only the translation.
            """;

    private final AnthropicClient anthropicClient;

    public String translate(String englishText, SupportedLanguage target) {
        if (target == SupportedLanguage.EN || englishText == null || englishText.isBlank()) {
            return englishText;
        }
        return anthropicClient.complete(SYSTEM_PROMPT.formatted(target.displayName()), englishText, englishText);
    }
}
