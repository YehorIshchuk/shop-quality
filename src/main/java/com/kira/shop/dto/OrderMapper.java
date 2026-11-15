package com.kira.shop.dto;

import com.kira.shop.domain.Order;
import com.kira.shop.util.MoneyUtils;

/** Перетворення доменних замовлень у DTO. */
public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderDto toDto(Order order) {
        return new OrderDto(
                order.getId(),
                order.getCustomer().getName(),
                order.getStatus().name(),
                order.totalQuantity(),
                MoneyUtils.round(order.total()));
    }
}
