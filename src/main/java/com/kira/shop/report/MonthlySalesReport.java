package com.kira.shop.report;

import com.kira.shop.domain.Order;
import com.kira.shop.util.MoneyUtils;

import java.time.YearMonth;

/** Місячний звіт: обгортка над SalesReport (композиція), фільтрує продажі за місяцем. */
public class MonthlySalesReport {
    private final YearMonth month;
    private final SalesReport sales;

    public MonthlySalesReport(YearMonth month, SalesReport sales) {
        this.month = month;
        this.sales = sales;
    }

    /** Фабрика зі стандартним складом компонентів. */
    public static MonthlySalesReport of(YearMonth month) {
        return new MonthlySalesReport(month, new SalesReport("Sales for " + month, new ReportTable(" | ")));
    }

    public YearMonth getMonth() {
        return month;
    }

    public int getOrderCount() {
        return sales.getOrderCount();
    }

    public void addSale(Order order) {
        if (YearMonth.from(order.getCreatedAt()).equals(month)) {
            sales.addSale(order);
        }
    }

    public String header() {
        return "*** MONTHLY SALES " + month + " ***";
    }

    public String footer() {
        return sales.footer() + ", avg: " + MoneyUtils.format(sales.averageOrderValue());
    }

    public String render() {
        if (sales.isEmpty()) {
            return header() + "\nNo sales in " + month;
        }
        return sales.renderWith(header(), footer(), row -> month + " " + row);
    }

    public String renderCsv() {
        return "month;" + month + "\n" + sales.renderCsv();
    }
}
