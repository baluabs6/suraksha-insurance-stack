package com.suraksha.backend.documents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, UUID> {
    List<PolicyDocument> findByUserIdOrderByUploadedAtDesc(UUID userId);
    List<PolicyDocument> findByClaimIdOrderByUploadedAtDesc(UUID claimId);
    List<PolicyDocument> findByPolicyIdOrderByUploadedAtDesc(UUID policyId);
}
