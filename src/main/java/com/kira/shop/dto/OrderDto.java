package com.kira.shop.dto;

import java.math.BigDecimal;

/** Плоске представлення замовлення для експорту та API. */
public final class OrderDto {
    private final String orderId;
    private final String customerName;
    private final String status;
    private final int itemCount;
    private final BigDecimal total;

    public OrderDto(String orderId, String customerName, String status, int itemCount, BigDecimal total) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.status = status;
        this.itemCount = itemCount;
        this.total = total;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getStatus() {
        return status;
    }

    public int getItemCount() {
        return itemCount;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
