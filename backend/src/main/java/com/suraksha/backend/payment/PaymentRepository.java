package com.suraksha.backend.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByPolicyUserIdOrderByPaidAtDesc(UUID userId);
    Optional<Payment> findByOrderId(String orderId);

    @Query("select p.policy.id from Payment p where p.id = :paymentId")
    UUID findPolicyIdByPaymentId(@Param("paymentId") UUID paymentId);
}
