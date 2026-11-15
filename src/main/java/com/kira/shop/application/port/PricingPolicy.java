package com.kira.shop.application.port;

import com.kira.shop.domain.Order;

import java.math.BigDecimal;

/** Політика ціноутворення: знижка та вартість доставки замовлення. */
public interface PricingPolicy {
    BigDecimal discountFor(Order order);

    BigDecimal shippingFor(Order order);
}
