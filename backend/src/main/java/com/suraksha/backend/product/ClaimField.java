package com.suraksha.backend.product;

import java.util.List;

/**
 * One field of a product's claim form. The frontend renders the form from these definitions and
 * {@link ClaimDetailsValidator} enforces the same rules on the server, so a new insurance type
 * needs no new form code.
 *
 * @param type              text | date | number | select | boolean
 * @param requiredWhenKey   optional: this field is also required when that other field ...
 * @param requiredWhenValue ... has this value (compared case-insensitively; booleans as "true"/"false")
 */
public record ClaimField(String key, String label, String type, boolean required, List<String> options,
                         String requiredWhenKey, String requiredWhenValue, String help) {

    public static ClaimField text(String key, String label, boolean required) {
        return new ClaimField(key, label, "text", required, List.of(), null, null, null);
    }

    public static ClaimField date(String key, String label, boolean required) {
        return new ClaimField(key, label, "date", required, List.of(), null, null, null);
    }

    public static ClaimField number(String key, String label, boolean required) {
        return new ClaimField(key, label, "number", required, List.of(), null, null, null);
    }

    public static ClaimField bool(String key, String label) {
        return new ClaimField(key, label, "boolean", false, List.of(), null, null, null);
    }

    public static ClaimField select(String key, String label, boolean required, String... options) {
        return new ClaimField(key, label, "select", required, List.of(options), null, null, null);
    }

    public ClaimField requiredWhen(String otherKey, String otherValue) {
        return new ClaimField(key, label, type, required, options, otherKey, otherValue, help);
    }

    public ClaimField withHelp(String helpText) {
        return new ClaimField(key, label, type, required, options, requiredWhenKey, requiredWhenValue, helpText);
    }
}
