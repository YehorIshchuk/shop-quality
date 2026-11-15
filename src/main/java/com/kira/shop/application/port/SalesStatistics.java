package com.kira.shop.application.port;

import com.kira.shop.domain.Order;

import java.util.Map;

/** Облік проданих одиниць товарів. */
public interface SalesStatistics {
    void record(Order order);

    void revert(Order order);

    Map<String, Integer> top(int limit);
}
