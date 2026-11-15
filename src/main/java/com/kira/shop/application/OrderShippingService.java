package com.kira.shop.application;

import com.kira.shop.application.port.Notifier;
import com.kira.shop.application.port.OrderStore;
import com.kira.shop.domain.Order;

/** Use case "Відвантажити / доставити замовлення". */
public class OrderShippingService {
    private final OrderStore orders;
    private final Notifier notifier;

    public OrderShippingService(OrderStore orders, Notifier notifier) {
        this.orders = orders;
        this.notifier = notifier;
    }

    public void ship(String orderId) {
        Order order = orders.require(orderId);
        order.markShipped();
        notifier.send(order.getCustomer().getEmail(), "Order shipped", "To: " + order.getCustomer().getAddress());
    }

    public void deliver(String orderId) {
        Order order = orders.require(orderId);
        order.markDelivered();
        notifier.send(order.getCustomer().getEmail(), "Order delivered", orderId);
    }
}
