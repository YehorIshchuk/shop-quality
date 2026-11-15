package com.kira.shop.infrastructure;

import com.kira.shop.application.port.OrderStore;
import com.kira.shop.domain.Order;
import com.kira.shop.domain.OrderStatus;
import com.kira.shop.dto.OrderDto;
import com.kira.shop.dto.OrderMapper;
import com.kira.shop.util.CsvUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Сховище замовлень з можливістю експорту у CSV-файл. */
public class FileOrderRepository implements OrderStore {
    private static final String HEADER = "order_id;customer;status;items;total";
    private final Map<String, Order> orders = new LinkedHashMap<>();

    @Override
    public void save(Order order) {
        orders.put(order.getId(), order);
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(orders.values());
    }

    public List<Order> findByStatus(OrderStatus status) {
        List<Order> result = new ArrayList<>();
        for (Order order : orders.values()) {
            if (order.getStatus() == status) {
                result.add(order);
            }
        }
        return result;
    }

    public void exportCsv(Path file) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Order order : orders.values()) {
            OrderDto dto = OrderMapper.toDto(order);
            lines.add(CsvUtils.join(dto.getOrderId(), dto.getCustomerName(), dto.getStatus(),
                    String.valueOf(dto.getItemCount()), dto.getTotal().toPlainString()));
        }
        try {
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot export orders to " + file, e);
        }
    }

    public int count() {
        return orders.size();
    }
}
