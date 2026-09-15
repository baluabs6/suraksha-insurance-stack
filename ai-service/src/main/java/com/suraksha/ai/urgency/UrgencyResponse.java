package com.suraksha.ai.urgency;

public record UrgencyResponse(String urgencyLevel, String reason, boolean usedFallback) {
}
