package com.kira.shop.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Замовлення — агрегат доменної моделі. */
public class Order {
    private final String id;
    private final Customer customer;
    private final LocalDate createdAt;
    private final List<OrderItem> items = new ArrayList<>();
    private OrderStatus status = OrderStatus.NEW;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal shippingCost = BigDecimal.ZERO;

    public Order(String id, Customer customer, LocalDate createdAt) {
        this.id = Objects.requireNonNull(id);
        this.customer = Objects.requireNonNull(customer);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public String getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void addItem(Product product, int quantity) {
        requireStatus(OrderStatus.NEW);
        items.add(new OrderItem(product, quantity));
    }

    public void applyDiscount(BigDecimal value) {
        requireStatus(OrderStatus.NEW);
        if (value.signum() < 0 || value.compareTo(subtotal()) > 0) {
            throw new IllegalArgumentException("Invalid discount");
        }
        this.discount = value;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        requireStatus(OrderStatus.NEW);
        this.shippingCost = Objects.requireNonNull(shippingCost);
    }

    public BigDecimal subtotal() {
        return items.stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal total() {
        return subtotal().subtract(discount).add(shippingCost);
    }

    public double totalWeight() {
        return items.stream().mapToDouble(OrderItem::lineWeight).sum();
    }

    public int totalQuantity() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void markPaid() {
        requireStatus(OrderStatus.NEW);
        status = OrderStatus.PAID;
    }

    public void markShipped() {
        requireStatus(OrderStatus.PAID);
        status = OrderStatus.SHIPPED;
    }

    public void markDelivered() {
        requireStatus(OrderStatus.SHIPPED);
        status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Shipped order cannot be cancelled");
        }
        status = OrderStatus.CANCELLED;
    }

    private void requireStatus(OrderStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Expected status " + expected + " but was " + status);
        }
    }
}
