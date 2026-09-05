package com.suraksha.backend.payment;

import com.suraksha.backend.payment.gateway.RazorpayClient;
import com.suraksha.backend.policy.Policy;
import com.suraksha.backend.policy.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final PolicyRepository policyRepository;
    private final RazorpayClient razorpayClient;

    @GetMapping
    public List<Payment> myPayments(Authentication auth) {
        return paymentRepository.findByPolicyUserIdOrderByPaidAtDesc(UUID.fromString(auth.getName()));
    }

    /**
     * Starts a payment. If Razorpay isn't configured, falls back to the old
     * demo behavior (mark PAID immediately) so the stack still works without
     * a merchant account. If it is configured, creates a real Razorpay order
     * and leaves the payment PENDING — the webhook below is the only thing
     * that ever marks it PAID. Never trust a client-side "it worked" callback
     * for money; that's spoofable by anyone who controls the browser.
     */
    @PostMapping("/{policyId}/pay")
    public ResponseEntity<?> initiatePayment(@PathVariable UUID policyId, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        Policy policy = policyRepository.findById(policyId)
                .filter(p -> p.getUser().getId().equals(userId))
                .orElse(null);
        if (policy == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Policy not found for this account."));
        }

        if (!razorpayClient.isConfigured()) {
            Payment payment = Payment.builder()
                    .policy(policy).amount(policy.getPremium()).method("UPI")
                    .gatewayTxnId("MOCK-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                    .status(PaymentStatus.PAID).paidAt(Instant.now())
                    .build();
            paymentRepository.save(payment);
            return ResponseEntity.status(201).body(Map.of("mock", true, "payment", payment));
        }

        String receipt = "policy-" + policy.getPolicyNumber() + "-" + Instant.now().toEpochMilli();
        RazorpayClient.OrderResult order = razorpayClient.createOrder(policy.getPremium(), receipt);
        if (!order.success()) {
            return ResponseEntity.status(502).body(Map.of("message", "Couldn't start the payment. Please try again."));
        }

        Payment payment = Payment.builder()
                .policy(policy).amount(policy.getPremium()).method("Razorpay checkout")
                .orderId(order.orderId()).status(PaymentStatus.PENDING).paidAt(Instant.now())
                .build();
        paymentRepository.save(payment);

        return ResponseEntity.status(201).body(Map.of(
                "mock", false,
                "orderId", order.orderId(),
                "amount", policy.getPremium(),
                "currency", "INR",
                "keyId", razorpayClient.getKeyId()
        ));
    }
}
