package com.suraksha.ai.security;

import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Masks identifiers that must never leave the platform in a prompt: card numbers,
 * Aadhaar, PAN, e-mail addresses and Indian mobile numbers.
 *
 * The backend encrypts PAN/Aadhaar at rest (EncryptedStringConverter); this closes the
 * other half of the gap so a customer pasting them into a chat message or claim
 * narrative doesn't ship them to a third-party model (or into stored chat memory).
 *
 * Deliberately conservative and regex-based: it fails closed (may mask an unrelated
 * 12-digit number) rather than open. Order matters — longer patterns run first.
 */
public final class PiiRedactor {

    private static final Pattern CARD = Pattern.compile("(?<![\\d])\\d(?:[ -]?\\d){12,18}(?![\\d])");
    private static final Pattern AADHAAR = Pattern.compile("(?<![\\d-])\\d{4}[ -]?\\d{4}[ -]?\\d{4}(?![\\d-])");
    private static final Pattern PAN = Pattern.compile("(?i)\\b[A-Z]{5}\\d{4}[A-Z]\\b");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(?<![\\d])(?:\\+91[ -]?|0)?[6-9]\\d{4}[ -]?\\d{5}(?![\\d])");

    private PiiRedactor() {}

    public static String redact(String text) {
        if (text == null || text.isEmpty()) return text;
        String out = CARD.matcher(text).replaceAll((MatchResult m) ->
                luhnValid(m.group()) ? "[REDACTED_CARD]" : "[REDACTED_NUMBER]"); // long digit runs fail closed
        out = AADHAAR.matcher(out).replaceAll("[REDACTED_AADHAAR]");
        out = PAN.matcher(out).replaceAll("[REDACTED_PAN]");
        out = EMAIL.matcher(out).replaceAll("[REDACTED_EMAIL]");
        out = PHONE.matcher(out).replaceAll("[REDACTED_PHONE]");
        return out;
    }

    static boolean luhnValid(String candidate) {
        int sum = 0;
        boolean alternate = false;
        int digits = 0;
        for (int i = candidate.length() - 1; i >= 0; i--) {
            char c = candidate.charAt(i);
            if (c < '0' || c > '9') continue;
            int n = c - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
            digits++;
        }
        return digits >= 13 && digits <= 19 && sum % 10 == 0;
    }
}
