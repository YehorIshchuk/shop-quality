package com.kira.shop.report;

import com.kira.shop.domain.Order;
import com.kira.shop.domain.OrderStatus;
import com.kira.shop.util.MoneyUtils;

import java.math.BigDecimal;

/** Звіт продажів: додає облік виручки та перевизначає оформлення базового звіту. */
public class SalesReport extends BaseReport {
    private BigDecimal revenue = BigDecimal.ZERO;
    private int orderCount;

    public SalesReport(String title) {
        super(title);
    }

    public void addSale(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        revenue = revenue.add(order.total());
        orderCount++;
        addRow(order.getId(), order.getCustomer().getName(), MoneyUtils.format(order.total()));
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public BigDecimal averageOrderValue() {
        if (orderCount == 0) {
            return BigDecimal.ZERO;
        }
        return MoneyUtils.round(revenue.divide(BigDecimal.valueOf(orderCount), 4, java.math.RoundingMode.HALF_UP));
    }

    @Override
    public String header() {
        return "*** SALES: " + getTitle() + " ***";
    }

    @Override
    public String formatRow(String[] cells) {
        return "- " + String.join(getSeparator(), cells);
    }

    @Override
    public String footer() {
        return "Orders: " + orderCount + ", revenue: " + MoneyUtils.format(revenue);
    }

    @Override
    public void clear() {
        super.clear();
        revenue = BigDecimal.ZERO;
        orderCount = 0;
    }
}
