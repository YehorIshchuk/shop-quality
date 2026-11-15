package com.kira.shop;

import com.kira.shop.domain.CustomerTier;
import com.kira.shop.domain.Order;
import com.kira.shop.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    private Order newOrder() {
        return new Order("O1", TestData.customer(CustomerTier.REGULAR), LocalDate.of(2026, 11, 1));
    }

    @Test
    void totalIncludesDiscountAndShipping() {
        Order order = newOrder();
        order.addItem(TestData.book(), 2);
        order.applyDiscount(new BigDecimal("100"));
        order.setShippingCost(new BigDecimal("70"));
        assertEquals(new BigDecimal("1000"), order.subtotal());
        assertEquals(new BigDecimal("970"), order.total());
        assertEquals(2, order.totalQuantity());
        assertEquals(1.0, order.totalWeight(), 0.0001);
    }

    @Test
    void lifecycleTransitions() {
        Order order = newOrder();
        order.addItem(TestData.book(), 1);
        order.markPaid();
        order.markShipped();
        order.markDelivered();
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertThrows(IllegalStateException.class, order::cancel);
    }

    @Test
    void cannotModifyPaidOrder() {
        Order order = newOrder();
        order.addItem(TestData.book(), 1);
        order.markPaid();
        assertThrows(IllegalStateException.class, () -> order.addItem(TestData.book(), 1));
    }

    @Test
    void discountCannotExceedSubtotal() {
        Order order = newOrder();
        order.addItem(TestData.book(), 1);
        assertThrows(IllegalArgumentException.class, () -> order.applyDiscount(new BigDecimal("501")));
        assertTrue(order.getItems().size() == 1);
    }
}
