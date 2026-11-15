package com.kira.shop.application;

import com.kira.shop.application.port.StockReservation;
import com.kira.shop.application.port.StockService;
import com.kira.shop.domain.Customer;
import com.kira.shop.domain.CustomerTier;
import com.kira.shop.domain.Order;
import com.kira.shop.domain.OrderItem;
import com.kira.shop.domain.Product;

/** Правила резервування: преміум-клієнти отримують залишок, якщо запитано більше, ніж є. */
public class StockAllocator implements StockReservation {
    private final StockService stock;

    public StockAllocator(StockService stock) {
        this.stock = stock;
    }

    @Override
    public int allocate(Customer customer, Product product, Integer requested) {
        if (requested == null || requested <= 0) {
            throw new IllegalArgumentException("Bad quantity for " + product.getId());
        }
        int available = stock.getStock(product);
        if (available >= requested) {
            return requested;
        }
        if (!isPremium(customer)) {
            throw new IllegalStateException("Not enough stock: " + product.getId());
        }
        if (available == 0) {
            throw new IllegalStateException("Out of stock: " + product.getId());
        }
        return available;
    }

    @Override
    public void reserve(Order order) {
        for (OrderItem item : order.getItems()) {
            stock.reserve(item.getProduct(), item.getQuantity());
        }
    }

    @Override
    public void release(Order order) {
        for (OrderItem item : order.getItems()) {
            stock.release(item.getProduct(), item.getQuantity());
        }
    }

    private static boolean isPremium(Customer customer) {
        return customer.getTier() == CustomerTier.GOLD || customer.getTier() == CustomerTier.PLATINUM;
    }
}
