package com.kira.shop;

import com.kira.shop.domain.CustomerTier;
import com.kira.shop.domain.Order;
import com.kira.shop.report.MonthlySalesReport;
import com.kira.shop.report.ReportTable;
import com.kira.shop.report.SalesReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportTest {

    private Order paidOrder(String id, LocalDate date) {
        Order order = new Order(id, TestData.customer(CustomerTier.SILVER), date);
        order.addItem(TestData.book(), 2);
        order.markPaid();
        return order;
    }

    @Test
    void salesReportCountsRevenue() {
        SalesReport report = new SalesReport("All", new ReportTable(" | "));
        report.addSale(paidOrder("O1", LocalDate.of(2026, 10, 3)));
        report.addSale(paidOrder("O2", LocalDate.of(2026, 11, 3)));
        assertEquals(2, report.getOrderCount());
        assertEquals(new BigDecimal("2000"), report.getRevenue());
        assertEquals(new BigDecimal("1000.00"), report.averageOrderValue());
        assertTrue(report.render().contains("revenue: 2000.00 UAH"));
    }

    @Test
    void monthlyReportFiltersByMonth() {
        MonthlySalesReport report = MonthlySalesReport.of(YearMonth.of(2026, 11));
        report.addSale(paidOrder("O1", LocalDate.of(2026, 10, 3)));
        report.addSale(paidOrder("O2", LocalDate.of(2026, 11, 3)));
        assertEquals(1, report.getOrderCount());
        assertTrue(report.render().startsWith("*** MONTHLY SALES 2026-11 ***"));
        assertTrue(report.renderCsv().startsWith("month;2026-11"));
    }

    @Test
    void emptyMonthlyReport() {
        MonthlySalesReport report = MonthlySalesReport.of(YearMonth.of(2026, 1));
        assertTrue(report.render().contains("No sales in 2026-01"));
    }

    @Test
    void rowsAreDecoratedByConcreteReport() {
        SalesReport sales = new SalesReport("All", new ReportTable(" | "));
        sales.addSale(paidOrder("O1", LocalDate.of(2026, 11, 3)));
        assertTrue(sales.render().contains("\n- O1 | Client SILVER | 1000.00 UAH\n"));

        MonthlySalesReport monthly = MonthlySalesReport.of(YearMonth.of(2026, 11));
        monthly.addSale(paidOrder("O2", LocalDate.of(2026, 11, 4)));
        assertTrue(monthly.render().contains("\n2026-11 - O2 | Client SILVER | 1000.00 UAH\n"));
    }

    @Test
    void reportTableIsIndependentComponent() {
        ReportTable table = new ReportTable(";");
        table.addRow(java.util.List.of("a", "b"));
        assertEquals(1, table.rowCount());
        assertEquals("H\n[a;b]\nF", table.render("H", "F", row -> "[" + row + "]"));
        assertEquals("a;b\n", table.renderCsv());
        table.clear();
        assertTrue(table.isEmpty());
    }
}
