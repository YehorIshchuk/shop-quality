package com.kira.shop.infrastructure;

import java.util.HashMap;
import java.util.Map;

/**
 * Облік складських залишків.
 * Ключ — ідентифікатор товару, значення — кількість одиниць на складі.
 */
public class InventoryStore {
    private final Map<String, Integer> stock = new HashMap<>();

    public int getStock(String productId) {
        return stock.getOrDefault(productId, 0);
    }

    public void add(String productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        stock.merge(productId, quantity, Integer::sum);
    }

    public void reserve(String productId, int quantity) {
        int available = getStock(productId);
        if (available < quantity) {
            throw new IllegalStateException("Not enough stock for " + productId);
        }
        stock.put(productId, available - quantity);
    }

    public void release(String productId, int quantity) {
        stock.merge(productId, quantity, Integer::sum);
    }
}
