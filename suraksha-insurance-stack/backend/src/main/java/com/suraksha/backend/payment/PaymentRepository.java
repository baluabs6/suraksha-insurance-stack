package com.suraksha.backend.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByPolicyUserIdOrderByPaidAtDesc(UUID userId);
    Optional<Payment> findByOrderId(String orderId);
}
