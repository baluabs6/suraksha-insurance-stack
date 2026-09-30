package com.suraksha.ai.urgency;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Target type for Spring AI structured output. */
public record UrgencyAssessment(
        @JsonPropertyDescription("Exactly one of LOW, MEDIUM, HIGH, CRITICAL")
        String urgencyLevel,
        @JsonPropertyDescription("One short factual sentence citing what in the text drove the level")
        String reason
) {
}
