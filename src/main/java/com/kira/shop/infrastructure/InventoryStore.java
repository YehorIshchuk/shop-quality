package com.kira.shop.infrastructure;

import com.kira.shop.domain.Product;

import java.util.HashMap;
import java.util.Map;

/**
 * Облік складських залишків.
 * Ключ — товар, значення — кількість одиниць на складі.
 */
public class InventoryStore {
    private final Map<Product, Integer> stock = new HashMap<>();

    public int getStock(Product product) {
        return stock.getOrDefault(product, 0);
    }

    public void add(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        stock.merge(product, quantity, Integer::sum);
    }

    public void reserve(Product product, int quantity) {
        int available = getStock(product);
        if (available < quantity) {
            throw new IllegalStateException("Not enough stock for " + product.getId());
        }
        stock.put(product, available - quantity);
    }

    public void release(Product product, int quantity) {
        stock.merge(product, quantity, Integer::sum);
    }
}
