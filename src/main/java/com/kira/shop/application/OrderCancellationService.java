package com.kira.shop.application;

import com.kira.shop.application.port.Notifier;
import com.kira.shop.application.port.OrderStore;
import com.kira.shop.application.port.PaymentProcessor;
import com.kira.shop.application.port.StockReservation;
import com.kira.shop.domain.Order;
import com.kira.shop.domain.OrderStatus;

/** Use case "Скасувати замовлення": повернення складу та коштів. */
public class OrderCancellationService {
    private final OrderStore orders;
    private final StockReservation stock;
    private final PaymentProcessor payments;
    private final Notifier notifier;

    public OrderCancellationService(OrderStore orders, StockReservation stock, PaymentProcessor payments,
                                    Notifier notifier) {
        this.orders = orders;
        this.stock = stock;
        this.payments = payments;
        this.notifier = notifier;
    }

    public void cancel(String orderId) {
        Order order = orders.require(orderId);
        boolean wasPaid = order.getStatus() == OrderStatus.PAID;
        order.cancel();
        stock.release(order);
        if (wasPaid) {
            payments.refund(orderId);
        }
        notifier.send(order.getCustomer().getEmail(), "Order cancelled", orderId);
    }
}
