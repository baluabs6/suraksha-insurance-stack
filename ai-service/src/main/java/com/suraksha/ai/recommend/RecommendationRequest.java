package com.suraksha.ai.recommend;

import com.suraksha.ai.model.PolicyTypeView;
import lombok.Data;

import java.util.List;

@Data
public class RecommendationRequest {
    private List<PolicyTypeView> ownedTypes;
}
