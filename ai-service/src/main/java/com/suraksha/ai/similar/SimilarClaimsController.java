package com.suraksha.ai.similar;

import com.suraksha.ai.model.ClaimRecord;
import com.suraksha.ai.repository.ClaimRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "Claims like this one" for adjusters: the closest past claims of the same type by description wording.
 * It is a lexical comparison and says nothing about whether a claim is genuine; it only helps an adjuster
 * find precedents. Restricted to adjusters and admins in SecurityConfig.
 */
@RestController
@RequestMapping("/api/ai/similar-claims")
@RequiredArgsConstructor
public class SimilarClaimsController {

    static final int CANDIDATES = 300;
    static final int RESULTS = 5;
    static final double MIN_SIMILARITY = 0.15;

    private final ClaimRecordRepository claimRecordRepository;

    public record SimilarClaim(UUID claimId, double similarity, String claimType, BigDecimal claimAmount,
                               LocalDate incidentDate, String status, String riskLevel) {}

    @GetMapping("/{claimId}")
    public ResponseEntity<?> similar(@PathVariable UUID claimId) {
        ClaimRecord target = claimRecordRepository.findById(claimId).orElse(null);
        if (target == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Claim not found."));
        }
        Set<String> targetTokens = TextSimilarity.tokens(target.getDescription());

        List<SimilarClaim> results = claimRecordRepository.findByClaimTypeOrderByIncidentDateDesc(target.getClaimType())
                .stream()
                .limit(CANDIDATES)
                .filter(c -> !c.getId().equals(claimId))
                .map(c -> new SimilarClaim(c.getId(), round(TextSimilarity.jaccard(targetTokens, TextSimilarity.tokens(c.getDescription()))),
                        c.getClaimType(), c.getClaimAmount(), c.getIncidentDate(),
                        c.getStatus() == null ? null : c.getStatus().name(), c.getRiskLevel()))
                .filter(s -> s.similarity() >= MIN_SIMILARITY)
                .sorted(Comparator.comparingDouble(SimilarClaim::similarity).reversed())
                .limit(RESULTS)
                .toList();
        return ResponseEntity.ok(results);
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
