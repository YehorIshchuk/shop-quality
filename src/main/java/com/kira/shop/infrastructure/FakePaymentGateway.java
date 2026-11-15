package com.kira.shop.infrastructure;

import com.kira.shop.application.port.PaymentGateway;
import com.kira.shop.application.port.PaymentResult;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/** Емуляція платіжного шлюзу (ліміт на одну транзакцію). */
public class FakePaymentGateway implements PaymentGateway {
    private static final BigDecimal LIMIT = new BigDecimal("50000");
    private final AtomicLong sequence = new AtomicLong(1000);

    @Override
    public PaymentResult charge(String customerId, BigDecimal amount) {
        if (amount.signum() <= 0) {
            return PaymentResult.declined("Amount must be positive");
        }
        if (amount.compareTo(LIMIT) > 0) {
            return PaymentResult.declined("Limit exceeded for " + customerId);
        }
        return PaymentResult.ok("TX-" + sequence.incrementAndGet());
    }

    @Override
    public boolean refund(String transactionId) {
        return transactionId != null && transactionId.startsWith("TX-");
    }
}
