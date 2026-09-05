package com.suraksha.ai.adjuster;

public record AdjusterQueryResponse(String answer, int claimsConsidered, boolean usedFallback) {
}
