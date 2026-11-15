package com.kira.shop.application;

import com.kira.shop.application.port.OrderStore;
import com.kira.shop.domain.Category;
import com.kira.shop.domain.Customer;
import com.kira.shop.domain.CustomerTier;
import com.kira.shop.domain.Order;
import com.kira.shop.domain.OrderItem;
import com.kira.shop.domain.OrderStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Predicate;

/** Аналітика клієнта: резюме покупок та рекомендація рівня лояльності. */
public class CustomerInsightsService {
    private static final BigDecimal PREMIUM_PURCHASE = new BigDecimal("10000");
    private static final BigDecimal GOLD_THRESHOLD = new BigDecimal("20000");
    private static final BigDecimal PLATINUM_THRESHOLD = new BigDecimal("50000");

    private final OrderStore orders;

    public CustomerInsightsService(OrderStore orders) {
        this.orders = orders;
    }

    public String buildCustomerSummary(Customer customer) {
        return summarize(customer, order -> true, "");
    }

    public String buildCustomerSummaryForMonth(Customer customer, YearMonth month) {
        return summarize(customer, order -> YearMonth.from(order.getCreatedAt()).equals(month), " (" + month + ")");
    }

    private String summarize(Customer customer, Predicate<Order> filter, String label) {
        List<Order> own = orders.findAll().stream()
                .filter(order -> order.getCustomer().getId().equals(customer.getId()))
                .filter(filter)
                .toList();
        List<Order> completed = own.stream().filter(CustomerInsightsService::isCompleted).toList();
        long cancelled = own.stream().filter(order -> order.getStatus() == OrderStatus.CANCELLED).count();
        BigDecimal spent = completed.stream().map(Order::total).reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder();
        completed.stream()
                .flatMap(order -> order.getItems().stream())
                .filter(CustomerInsightsService::isPremiumPurchase)
                .forEach(item -> sb.append("Premium purchase: ").append(item.getProduct().getName()).append('\n'));
        sb.append("Customer: ").append(customer.getName()).append(label).append('\n');
        sb.append("Paid orders: ").append(completed.size()).append(", cancelled: ").append(cancelled).append('\n');
        sb.append("Spent: ").append(spent.setScale(2, RoundingMode.HALF_UP).toPlainString()).append(" UAH\n");
        sb.append("Recommendation: ").append(recommendTier(customer.getTier(), spent));
        return sb.toString();
    }

    private static boolean isCompleted(Order order) {
        return order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.NEW;
    }

    private static boolean isPremiumPurchase(OrderItem item) {
        return item.getProduct().getCategory() == Category.ELECTRONICS
                && item.lineTotal().compareTo(PREMIUM_PURCHASE) > 0;
    }

    static String recommendTier(CustomerTier current, BigDecimal spent) {
        if (spent.compareTo(PLATINUM_THRESHOLD) > 0) {
            return current == CustomerTier.PLATINUM ? "keep PLATINUM" : "upgrade to PLATINUM";
        }
        if (spent.compareTo(GOLD_THRESHOLD) > 0 && current.compareTo(CustomerTier.GOLD) < 0) {
            return "upgrade to GOLD";
        }
        return "keep " + current;
    }
}
