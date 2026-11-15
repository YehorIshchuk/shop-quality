package com.kira.shop.report;

import com.kira.shop.domain.Order;
import com.kira.shop.util.MoneyUtils;

import java.time.YearMonth;

/** Місячний звіт продажів — третій рівень ієрархії, перевизначає більшість батьківської логіки. */
public class MonthlySalesReport extends SalesReport {
    private final YearMonth month;

    public MonthlySalesReport(YearMonth month) {
        super("Sales for " + month);
        this.month = month;
    }

    public YearMonth getMonth() {
        return month;
    }

    @Override
    public void addSale(Order order) {
        if (YearMonth.from(order.getCreatedAt()).equals(month)) {
            super.addSale(order);
        }
    }

    @Override
    public String header() {
        return "*** MONTHLY SALES " + month + " ***";
    }

    @Override
    public String formatRow(String[] cells) {
        return month + " " + super.formatRow(cells);
    }

    @Override
    public String footer() {
        return super.footer() + ", avg: " + MoneyUtils.format(averageOrderValue());
    }

    @Override
    public String renderCsv() {
        return "month;" + month + "\n" + super.renderCsv();
    }

    @Override
    public String render() {
        if (isEmpty()) {
            return header() + "\nNo sales in " + month;
        }
        return super.render();
    }
}
