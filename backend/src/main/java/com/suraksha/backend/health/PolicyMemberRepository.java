package com.suraksha.backend.health;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PolicyMemberRepository extends JpaRepository<PolicyMember, UUID> {
    List<PolicyMember> findByPolicyIdOrderByCreatedAtAsc(UUID policyId);
    Optional<PolicyMember> findByIdAndPolicyId(UUID id, UUID policyId);
    long countByPolicyId(UUID policyId);
    boolean existsByPolicyIdAndRelationship(UUID policyId, MemberRelationship relationship);
}
