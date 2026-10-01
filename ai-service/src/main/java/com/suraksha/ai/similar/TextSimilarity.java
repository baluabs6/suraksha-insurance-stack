package com.suraksha.ai.similar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Word-overlap (Jaccard) similarity between two descriptions. Lexical, not semantic: no embeddings involved. */
public final class TextSimilarity {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "of", "to", "in", "on", "at", "for", "with", "was", "were", "is", "are",
            "my", "our", "i", "we", "it", "its", "this", "that", "from", "by", "as", "be", "been", "had", "has", "have",
            "when", "then", "after", "before", "into", "very", "also", "than", "but", "not", "no", "so");

    private TextSimilarity() {}

    public static Set<String> tokens(String text) {
        if (text == null) return Set.of();
        Set<String> out = new HashSet<>();
        Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(t -> t.length() > 2 && !STOP_WORDS.contains(t))
                .forEach(out::add);
        return out;
    }

    public static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }
}
