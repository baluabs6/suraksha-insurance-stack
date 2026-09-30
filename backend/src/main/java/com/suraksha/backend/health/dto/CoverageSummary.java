package com.suraksha.backend.health.dto;

import java.math.BigDecimal;

/** used = approved/settled claims; pending = submitted/under-review claims (reserved against the sum insured). */
public record CoverageSummary(BigDecimal sumInsured, BigDecimal used, BigDecimal pending, BigDecimal remaining,
                              BigDecimal roomRentCapPerDay, int coPayPercent, int initialWaitingDays,
                              int preExistingWaitingMonths) {
}
