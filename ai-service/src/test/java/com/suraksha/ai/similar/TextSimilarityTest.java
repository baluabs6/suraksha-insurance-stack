package com.suraksha.ai.similar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextSimilarityTest {

    @Test
    void similarDescriptionsScoreHigherThanUnrelatedOnes() {
        var a = TextSimilarity.tokens("Rear-ended at a traffic signal, bumper and tail light damaged");
        var b = TextSimilarity.tokens("Car rear-ended at signal, bumper damaged and tail light broken");
        var c = TextSimilarity.tokens("Laptop stolen from office during lunch");
        assertTrue(TextSimilarity.jaccard(a, b) > TextSimilarity.jaccard(a, c));
        assertTrue(TextSimilarity.jaccard(a, b) > 0.3);
    }

    @Test
    void emptyOrNullTextScoresZero() {
        assertEquals(0.0, TextSimilarity.jaccard(TextSimilarity.tokens(null), TextSimilarity.tokens("anything here")));
        assertEquals(0.0, TextSimilarity.jaccard(TextSimilarity.tokens(""), TextSimilarity.tokens("")));
    }

    @Test
    void stopWordsAndShortWordsAreIgnored() {
        assertEquals(java.util.Set.of("bumper", "damaged"), TextSimilarity.tokens("The bumper was damaged in it"));
    }
}
