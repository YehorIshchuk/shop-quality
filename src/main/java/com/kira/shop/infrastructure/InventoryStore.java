package com.kira.shop.infrastructure;

import com.kira.shop.application.port.StockService;
import com.kira.shop.domain.Product;

import java.util.HashMap;
import java.util.Map;

/**
 * Облік складських залишків.
 * Ключ — товар, значення — кількість одиниць на складі.
 */
public class InventoryStore implements StockService {
    private final Map<Product, Integer> stock = new HashMap<>();

    @Override
    public int getStock(Product product) {
        return stock.getOrDefault(product, 0);
    }

    @Override
    public void add(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        stock.merge(product, quantity, Integer::sum);
    }

    @Override
    public void reserve(Product product, int quantity) {
        int available = getStock(product);
        if (available < quantity) {
            throw new IllegalStateException("Not enough stock for " + product.getId());
        }
        stock.put(product, available - quantity);
    }

    @Override
    public void release(Product product, int quantity) {
        stock.merge(product, quantity, Integer::sum);
    }
}
