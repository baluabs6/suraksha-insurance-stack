package com.suraksha.ai.recommend;

import com.suraksha.ai.model.PolicyTypeView;
import lombok.Data;

import java.util.List;

@Data
public class RecommendationRequest {
    // Policy types the customer already holds — sent by the frontend from
    // policies it already loaded post-login, same pattern as the chatbot.
    private List<PolicyTypeView> ownedTypes;
}
