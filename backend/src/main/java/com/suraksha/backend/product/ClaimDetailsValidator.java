package com.suraksha.backend.product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates the type-specific "details" of a claim against the product's field definitions.
 * Unknown keys are rejected and only known keys, normalised, are returned, so arbitrary
 * client-supplied JSON is never persisted.
 */
public final class ClaimDetailsValidator {

    static final int MAX_TEXT_LENGTH = 300;

    public record Result(String error, Map<String, Object> details) {
        static Result error(String message) {
            return new Result(message, null);
        }
    }

    private ClaimDetailsValidator() {}

    public static Result validate(ProductDefinition product, Map<String, Object> raw) {
        Map<String, Object> input = raw == null ? Map.of() : raw;

        for (String key : input.keySet()) {
            boolean known = product.claimFields().stream().anyMatch(f -> f.key().equals(key));
            if (!known) {
                return Result.error("Unexpected claim detail '" + key + "' for a " + product.label() + " policy.");
            }
        }

        Map<String, Object> cleaned = new LinkedHashMap<>();
        for (ClaimField field : product.claimFields()) {
            Object value = input.get(field.key());
            boolean blank = value == null || value.toString().isBlank();

            if (blank) {
                if (isRequired(field, input)) {
                    return Result.error("Enter " + field.label().toLowerCase() + ".");
                }
                continue;
            }

            String text = value.toString().trim();
            switch (field.type()) {
                case "text" -> {
                    if (text.length() > MAX_TEXT_LENGTH) {
                        return Result.error(field.label() + " is too long (max " + MAX_TEXT_LENGTH + " characters).");
                    }
                    cleaned.put(field.key(), text);
                }
                case "date" -> {
                    try {
                        cleaned.put(field.key(), LocalDate.parse(text).toString());
                    } catch (DateTimeParseException e) {
                        return Result.error(field.label() + " must be a valid date.");
                    }
                }
                case "number" -> {
                    try {
                        BigDecimal number = new BigDecimal(text);
                        if (number.signum() < 0) {
                            return Result.error(field.label() + " can't be negative.");
                        }
                        cleaned.put(field.key(), number);
                    } catch (NumberFormatException e) {
                        return Result.error(field.label() + " must be a number.");
                    }
                }
                case "select" -> {
                    String match = field.options().stream().filter(o -> o.equalsIgnoreCase(text)).findFirst().orElse(null);
                    if (match == null) {
                        return Result.error("Choose a valid option for " + field.label().toLowerCase() + ".");
                    }
                    cleaned.put(field.key(), match);
                }
                case "boolean" -> {
                    if (!text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false")) {
                        return Result.error(field.label() + " must be yes or no.");
                    }
                    cleaned.put(field.key(), Boolean.parseBoolean(text));
                }
                default -> throw new IllegalStateException("Unknown field type " + field.type());
            }
        }

        return new Result(null, cleaned);
    }

    static boolean isRequired(ClaimField field, Map<String, Object> input) {
        if (field.required()) return true;
        if (field.requiredWhenKey() == null) return false;
        Object trigger = input.get(field.requiredWhenKey());
        return trigger != null && trigger.toString().trim().equalsIgnoreCase(field.requiredWhenValue());
    }
}
