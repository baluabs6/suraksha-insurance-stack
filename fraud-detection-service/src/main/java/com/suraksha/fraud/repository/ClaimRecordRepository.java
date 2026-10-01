package com.suraksha.fraud.repository;

import com.suraksha.fraud.model.ClaimRecord;
import com.suraksha.fraud.model.ClaimStatusView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ClaimRecordRepository extends JpaRepository<ClaimRecord, UUID> {
    List<ClaimRecord> findByPolicyIdAndStatusNot(UUID policyId, ClaimStatusView excludedStatus);

    /**
     * Other claims (any policy) carrying the same asset identifier in their details, for example the same vehicle
     * registration, IMEI or shipment reference. Compared ignoring case, spaces and punctuation; pass the value
     * already normalised with {@code TypeSpecificRules.normalizeIdentifier}.
     */
    @Query(value = "SELECT COUNT(*) FROM claims WHERE id <> :claimId AND "
            + "UPPER(REGEXP_REPLACE(COALESCE(details ->> CAST(:key AS text), ''), '[^A-Za-z0-9]', '', 'g')) = :value",
            nativeQuery = true)
    long countOtherClaimsWithAsset(@Param("claimId") UUID claimId, @Param("key") String key, @Param("value") String value);

    /** Same as above, restricted to claims with the same incident date. */
    @Query(value = "SELECT COUNT(*) FROM claims WHERE id <> :claimId AND incident_date = :incidentDate AND "
            + "UPPER(REGEXP_REPLACE(COALESCE(details ->> CAST(:key AS text), ''), '[^A-Za-z0-9]', '', 'g')) = :value",
            nativeQuery = true)
    long countOtherClaimsWithAssetOnDate(@Param("claimId") UUID claimId, @Param("key") String key,
                                         @Param("value") String value, @Param("incidentDate") LocalDate incidentDate);

    /** Other claims on the same policy with the same "what happened" category (delay, theft, illness...). */
    @Query(value = "SELECT COUNT(*) FROM claims WHERE policy_id = :policyId AND id <> :claimId AND "
            + "COALESCE(details ->> 'claimCategory', details ->> 'incidentType', details ->> 'damageType', "
            + "details ->> 'lossType', details ->> 'injuryType') = :category",
            nativeQuery = true)
    long countOtherClaimsOnPolicyWithCategory(@Param("policyId") UUID policyId, @Param("claimId") UUID claimId,
                                              @Param("category") String category);
}
