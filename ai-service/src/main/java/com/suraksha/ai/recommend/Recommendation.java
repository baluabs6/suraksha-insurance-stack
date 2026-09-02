package com.suraksha.ai.recommend;

import com.suraksha.ai.model.PolicyTypeView;

public record Recommendation(PolicyTypeView type, String planName, String reason) {
}
