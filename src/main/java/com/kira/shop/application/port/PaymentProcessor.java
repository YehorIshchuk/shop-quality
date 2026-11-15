package com.kira.shop.application.port;

/** Оплата та повернення коштів за замовленням. */
public interface PaymentProcessor {
    boolean pay(String orderId);

    void refund(String orderId);
}
