package com.kira.shop.application;

import com.kira.shop.application.port.OrderStore;
import com.kira.shop.domain.OrderStatus;
import com.kira.shop.dto.OrderMapper;
import com.kira.shop.report.MonthlySalesReport;
import com.kira.shop.util.CsvUtils;

import java.time.YearMonth;
import java.util.List;

/** Звітність по продажах: місячний звіт та експорт рядків замовлень. */
public class SalesReportingService {
    private final OrderStore orders;

    public SalesReportingService(OrderStore orders) {
        this.orders = orders;
    }

    public String monthlyReport(YearMonth month) {
        MonthlySalesReport report = new MonthlySalesReport(month);
        orders.findAll().stream().filter(order -> order.getStatus() != OrderStatus.NEW).forEach(report::addSale);
        return report.render();
    }

    public List<String> exportOrderLines() {
        return orders.findAll().stream()
                .map(OrderMapper::toDto)
                .map(dto -> CsvUtils.join(dto.getOrderId(), dto.getCustomerName(), dto.getStatus()))
                .toList();
    }
}
