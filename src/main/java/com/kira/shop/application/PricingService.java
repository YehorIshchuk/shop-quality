package com.kira.shop.application;

import com.kira.shop.application.port.PricingPolicy;
import com.kira.shop.domain.Address;
import com.kira.shop.domain.Category;
import com.kira.shop.domain.Order;
import com.kira.shop.util.MoneyUtils;

import java.math.BigDecimal;

/** Знижки за рівнем лояльності та вартість доставки (без стану). */
public class PricingService implements PricingPolicy {
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("2000");
    private static final int BOOKS_BONUS_PERCENT = 2;
    private static final int BOOKS_BONUS_MIN_QUANTITY = 3;

    @Override
    public BigDecimal discountFor(Order order) {
        int percent = tierPercent(order) + booksBonus(order);
        return MoneyUtils.percentOf(order.subtotal(), percent);
    }

    @Override
    public BigDecimal shippingFor(Order order) {
        Address address = order.getCustomer().getAddress();
        if (address.isDomestic() && order.subtotal().compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = address.isDomestic() ? new BigDecimal("70") : new BigDecimal("450");
        BigDecimal perKg = address.isDomestic() ? new BigDecimal("10") : new BigDecimal("120");
        return MoneyUtils.round(base.add(perKg.multiply(BigDecimal.valueOf(Math.ceil(order.totalWeight())))));
    }

    private static int tierPercent(Order order) {
        switch (order.getCustomer().getTier()) {
            case SILVER:
                return 3;
            case GOLD:
                return 5;
            case PLATINUM:
                return 10;
            default:
                return 0;
        }
    }

    private static int booksBonus(Order order) {
        boolean hasBooks = order.getItems().stream()
                .anyMatch(item -> item.getProduct().getCategory() == Category.BOOKS);
        return hasBooks && order.totalQuantity() >= BOOKS_BONUS_MIN_QUANTITY ? BOOKS_BONUS_PERCENT : 0;
    }
}
