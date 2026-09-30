package com.suraksha.ai.recommend;

import com.suraksha.ai.model.PolicyTypeView;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PlanCatalogTest {

    @Test
    void everyPolicyTypeHasAPlan() {
        for (PolicyTypeView type : PolicyTypeView.values()) {
            assertTrue(PlanCatalog.forType(type).isPresent(), "No plan for " + type);
        }
        assertEquals(PolicyTypeView.values().length, PlanCatalog.ALL.size());
    }

    @Test
    void recommendationsAreCappedAndNeverIncludeOwnedTypes() {
        Set<PolicyTypeView> owned = EnumSet.of(PolicyTypeView.HEALTH, PolicyTypeView.MOTOR);
        List<PolicyTypeView> ranked = RecommendationController.rankUnowned(owned);
        assertTrue(ranked.size() <= RecommendationController.MAX_RECOMMENDATIONS);
        assertTrue(ranked.stream().noneMatch(owned::contains));
    }

    @Test
    void relatedCoverIsOfferedFirst() {
        List<PolicyTypeView> ranked = RecommendationController.rankUnowned(EnumSet.of(PolicyTypeView.TWO_WHEELER));
        assertEquals(PolicyTypeView.PERSONAL_ACCIDENT, ranked.get(0));
    }

    @Test
    void ownerOfEverythingGetsNoRecommendations() {
        assertTrue(RecommendationController.rankUnowned(EnumSet.allOf(PolicyTypeView.class)).isEmpty());
    }
}
