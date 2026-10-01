package com.suraksha.backend.quote;

import com.suraksha.backend.policy.PolicyType;
import com.suraksha.backend.product.ClaimDetailsValidator;
import com.suraksha.backend.product.ProductRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Turns a quote request into a validated, priced quote. Used for both "get a quote" and "buy". */
@Service
public class QuoteService {

    static final int MAX_DAYS_AHEAD = 60;

    public record QuoteResult(PolicyType type, String planName, BigDecimal coverageAmount, BigDecimal premium,
                              String premiumBasis, List<PremiumCalculator.Line> breakdown,
                              LocalDate startDate, LocalDate endDate, Map<String, Object> attributes) {}

    /** @throws IllegalArgumentException with a customer-readable message when the request can't be quoted */
    public QuoteResult quote(QuoteRequest req) {
        return quote(req, LocalDate.now());
    }

    QuoteResult quote(QuoteRequest req, LocalDate today) {
        PolicyType type = req.getType();
        QuoteSpec spec = QuoteRegistry.get(type);
        var product = ProductRegistry.get(type);

        var checked = ClaimDetailsValidator.validate(product.label(), spec.fields(), req.getInputs());
        if (checked.error() != null) throw new IllegalArgumentException(checked.error());
        Map<String, Object> inputs = checked.details();

        BigDecimal coverage = coverageFor(spec, req, inputs);

        LocalDate start = req.getStartDate() == null ? today : req.getStartDate();
        if (start.isBefore(today) || start.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new IllegalArgumentException("The start date must be within the next " + MAX_DAYS_AHEAD + " days.");
        }

        PremiumCalculator.Quote priced = PremiumCalculator.calculate(type, coverage, inputs);

        Map<String, Object> attributes = new LinkedHashMap<>();
        for (String key : spec.attributeKeys()) {
            if (inputs.containsKey(key)) attributes.put(key, inputs.get(key));
        }

        return new QuoteResult(type, product.planName(), coverage, priced.premium(), spec.premiumBasis(),
                priced.lines(), start, endDate(type, start, inputs), attributes);
    }

    private static BigDecimal coverageFor(QuoteSpec spec, QuoteRequest req, Map<String, Object> inputs) {
        if (spec.coverageFromField() != null) {
            return (BigDecimal) inputs.get(spec.coverageFromField());
        }
        BigDecimal chosen = req.getCoverageAmount();
        boolean allowed = chosen != null && spec.coverageOptions().stream()
                .anyMatch(o -> BigDecimal.valueOf(o).compareTo(chosen) == 0);
        if (!allowed) throw new IllegalArgumentException("Choose one of the available " + spec.coverageLabel().toLowerCase() + " options.");
        return chosen;
    }

    static LocalDate endDate(PolicyType type, LocalDate start, Map<String, Object> inputs) {
        return switch (type) {
            case LIFE -> start.plusYears(Integer.parseInt(inputs.get("termYears").toString())).minusDays(1);
            case TRAVEL -> start.plusDays(((BigDecimal) inputs.get("tripDays")).longValue() - 1);
            case MARINE_CARGO -> start.plusDays(29);
            default -> start.plusYears(1).minusDays(1);
        };
    }
}
