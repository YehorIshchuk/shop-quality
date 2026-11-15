package com.kira.shop.application;

import com.kira.shop.application.port.Notifier;
import com.kira.shop.application.port.OrderStore;
import com.kira.shop.application.port.PaymentGateway;
import com.kira.shop.application.port.PaymentProcessor;
import com.kira.shop.application.port.PaymentResult;
import com.kira.shop.application.port.SalesStatistics;
import com.kira.shop.domain.Order;

import java.util.HashMap;
import java.util.Map;

/** Use case "Оплатити / повернути кошти": шлюз, транзакції, статистика продажів, бонуси. */
public class OrderPaymentService implements PaymentProcessor {
    private final OrderStore orders;
    private final PaymentGateway gateway;
    private final SalesStatistics statistics;
    private final Notifier notifier;
    private final Map<String, String> transactions = new HashMap<>();

    public OrderPaymentService(OrderStore orders, PaymentGateway gateway, SalesStatistics statistics, Notifier notifier) {
        this.orders = orders;
        this.gateway = gateway;
        this.statistics = statistics;
        this.notifier = notifier;
    }

    @Override
    public boolean pay(String orderId) {
        Order order = orders.require(orderId);
        PaymentResult result = gateway.charge(order.getCustomer().getId(), order.total());
        if (!result.isSuccess()) {
            notifier.send(order.getCustomer().getEmail(), "Payment failed", result.getMessage());
            return false;
        }
        order.markPaid();
        transactions.put(orderId, result.getTransactionId());
        statistics.record(order);
        order.getCustomer().addLoyaltyPoints(order.total().intValue() / 100);
        notifier.send(order.getCustomer().getEmail(), "Payment received", result.getTransactionId());
        return true;
    }

    @Override
    public void refund(String orderId) {
        Order order = orders.require(orderId);
        String transactionId = transactions.remove(orderId);
        if (transactionId == null || !gateway.refund(transactionId)) {
            return;
        }
        statistics.revert(order);
        notifier.send(order.getCustomer().getEmail(), "Refund issued", transactionId);
    }
}
