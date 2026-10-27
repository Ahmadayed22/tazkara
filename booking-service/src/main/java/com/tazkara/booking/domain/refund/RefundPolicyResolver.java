package com.tazkara.booking.domain.refund;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;


@Component
public class RefundPolicyResolver {

    private final List<RefundPolicy> policies;

    public RefundPolicyResolver(List<RefundPolicy> policies) {
        this.policies = List.copyOf(policies);
    }

    public RefundPolicy resolve(Duration timeBeforeStart) {
        return policies.stream()
                .filter(policy -> policy.appliesTo(timeBeforeStart))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No refund policy covers " + timeBeforeStart));
    }
}
