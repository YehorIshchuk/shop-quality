package com.kira.shop.application.port;

import com.kira.shop.domain.Customer;
import com.kira.shop.domain.Order;
import com.kira.shop.domain.Product;

/** Бізнес-правила резервування складу під замовлення. */
public interface StockReservation {
    int allocate(Customer customer, Product product, Integer requested);

    void reserve(Order order);

    void release(Order order);
}
