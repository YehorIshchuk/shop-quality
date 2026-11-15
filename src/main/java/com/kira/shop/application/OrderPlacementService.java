package com.kira.shop.application;

import com.kira.shop.application.port.Notifier;
import com.kira.shop.application.port.OrderStore;
import com.kira.shop.application.port.PricingPolicy;
import com.kira.shop.application.port.ProductCatalog;
import com.kira.shop.application.port.StockReservation;
import com.kira.shop.domain.Customer;
import com.kira.shop.domain.Order;
import com.kira.shop.domain.Product;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Use case "Оформити замовлення". Усі залежності — абстракції, впроваджені через конструктор. */
public class OrderPlacementService {
    private final ProductCatalog catalog;
    private final StockReservation stock;
    private final PricingPolicy pricing;
    private final OrderStore orders;
    private final Notifier notifier;
    private final AtomicInteger sequence = new AtomicInteger(1);

    public OrderPlacementService(ProductCatalog catalog, StockReservation stock, PricingPolicy pricing,
                                 OrderStore orders, Notifier notifier) {
        this.catalog = catalog;
        this.stock = stock;
        this.pricing = pricing;
        this.orders = orders;
        this.notifier = notifier;
    }

    public Order placeOrder(Customer customer, Map<String, Integer> cart) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required");
        }
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        Order order = new Order("ORD-" + sequence.getAndIncrement(), customer, LocalDate.now());
        cart.forEach((productId, quantity) -> {
            Product product = catalog.require(productId);
            order.addItem(product, stock.allocate(customer, product, quantity));
        });
        stock.reserve(order);
        order.applyDiscount(pricing.discountFor(order));
        order.setShippingCost(pricing.shippingFor(order));
        orders.save(order);
        notifier.send(customer.getEmail(), "Order " + order.getId(), "Total: " + order.total().toPlainString() + " UAH");
        return order;
    }
}
