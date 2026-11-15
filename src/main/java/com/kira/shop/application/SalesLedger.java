package com.kira.shop.application;

import com.kira.shop.application.port.SalesStatistics;
import com.kira.shop.domain.Order;
import com.kira.shop.domain.OrderItem;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** In-memory облік проданих одиниць по товарах. */
public class SalesLedger implements SalesStatistics {
    private final Map<String, Integer> soldByProduct = new HashMap<>();

    @Override
    public void record(Order order) {
        for (OrderItem item : order.getItems()) {
            soldByProduct.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
        }
    }

    @Override
    public void revert(Order order) {
        for (OrderItem item : order.getItems()) {
            soldByProduct.computeIfPresent(item.getProduct().getId(),
                    (id, sold) -> sold > item.getQuantity() ? sold - item.getQuantity() : null);
        }
    }

    @Override
    public Map<String, Integer> top(int limit) {
        Map<String, Integer> result = new LinkedHashMap<>();
        soldByProduct.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }
}
