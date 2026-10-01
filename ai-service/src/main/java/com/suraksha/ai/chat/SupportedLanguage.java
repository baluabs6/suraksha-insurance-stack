package com.suraksha.ai.chat;

import java.util.Arrays;
import java.util.Optional;

/** Languages the support chatbot can reply in. The code is what the client sends. */
public enum SupportedLanguage {
    EN("en", "English"),
    HI("hi", "Hindi"),
    TE("te", "Telugu"),
    TA("ta", "Tamil"),
    KN("kn", "Kannada"),
    ML("ml", "Malayalam"),
    MR("mr", "Marathi"),
    BN("bn", "Bengali"),
    GU("gu", "Gujarati");

    private final String code;
    private final String displayName;

    SupportedLanguage(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() { return code; }
    public String displayName() { return displayName; }

    /** Unknown or missing codes fall back to English. */
    public static SupportedLanguage fromCode(String code) {
        if (code == null) return EN;
        return Arrays.stream(values()).filter(l -> l.code.equalsIgnoreCase(code.trim())).findFirst().orElse(EN);
    }

    public static Optional<SupportedLanguage> parse(String code) {
        return code == null ? Optional.empty()
                : Arrays.stream(values()).filter(l -> l.code.equalsIgnoreCase(code.trim())).findFirst();
    }
}
