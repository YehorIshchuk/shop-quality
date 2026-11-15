package com.kira.shop.application.port;

import java.math.BigDecimal;

/** Контракт зовнішнього платіжного шлюзу. */
public interface PaymentGateway {
    PaymentResult charge(String customerId, BigDecimal amount);

    boolean refund(String transactionId);
}
