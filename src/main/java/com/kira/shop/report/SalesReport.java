package com.kira.shop.report;

import com.kira.shop.domain.Order;
import com.kira.shop.domain.OrderStatus;
import com.kira.shop.util.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.UnaryOperator;

/** Звіт продажів. Не успадковує ReportTable, а містить його (композиція + DI через конструктор). */
public class SalesReport {
    private final String title;
    private final ReportTable table;
    private BigDecimal revenue = BigDecimal.ZERO;
    private int orderCount;

    public SalesReport(String title, ReportTable table) {
        this.title = title;
        this.table = table;
    }

    public void addSale(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        revenue = revenue.add(order.total());
        orderCount++;
        table.addRow(List.of(order.getId(), order.getCustomer().getName(), MoneyUtils.format(order.total())));
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public boolean isEmpty() {
        return table.isEmpty();
    }

    public BigDecimal averageOrderValue() {
        if (orderCount == 0) {
            return BigDecimal.ZERO;
        }
        return MoneyUtils.round(revenue.divide(BigDecimal.valueOf(orderCount), 4, RoundingMode.HALF_UP));
    }

    public String header() {
        return "*** SALES: " + title + " ***";
    }

    public String footer() {
        return "Orders: " + orderCount + ", revenue: " + MoneyUtils.format(revenue);
    }

    public String render() {
        return renderWith(header(), footer(), row -> row);
    }

    /** Точка розширення для звітів-обгорток: власні заголовок/підсумок і префікс рядка. */
    public String renderWith(String customHeader, String customFooter, UnaryOperator<String> rowPrefix) {
        return table.render(customHeader, customFooter, row -> rowPrefix.apply("- " + row));
    }

    public String renderCsv() {
        return table.renderCsv();
    }

    public void clear() {
        table.clear();
        revenue = BigDecimal.ZERO;
        orderCount = 0;
    }
}
