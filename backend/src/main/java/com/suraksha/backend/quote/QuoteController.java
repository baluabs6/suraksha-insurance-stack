package com.suraksha.backend.quote;

import com.suraksha.backend.policy.PolicyType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    /** The quote form for every insurance type: what to ask, and which coverage amounts are offered. */
    @GetMapping("/forms")
    public Map<PolicyType, QuoteSpec> forms() {
        return QuoteRegistry.all();
    }

    @PostMapping
    public ResponseEntity<?> quote(@Valid @RequestBody QuoteRequest req) {
        try {
            return ResponseEntity.ok(quoteService.quote(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
