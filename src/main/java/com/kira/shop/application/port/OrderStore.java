package com.kira.shop.application.port;

import com.kira.shop.domain.Order;

import java.util.List;
import java.util.Optional;

/** Контракт сховища замовлень. */
public interface OrderStore {
    void save(Order order);

    Optional<Order> findById(String id);

    List<Order> findAll();

    default Order require(String id) {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown order " + id));
    }
}
