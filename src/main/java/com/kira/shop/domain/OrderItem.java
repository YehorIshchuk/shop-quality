package com.kira.shop.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Позиція замовлення. */
public class OrderItem {
    private final Product product;
    private final int quantity;

    public OrderItem(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.product = Objects.requireNonNull(product);
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal lineTotal() {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public double lineWeight() {
        return product.getWeightKg() * quantity;
    }
}
