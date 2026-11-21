package com.kira.shop.api;

import com.kira.shop.application.port.OrderStore;
import com.kira.shop.dto.OrderDto;
import com.kira.shop.dto.OrderMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Read-only HTTP API замовлень на вбудованому {@link HttpServer} JDK (без зовнішніх залежностей).
 * <ul>
 *   <li>{@code GET /api/health} → {@code {"status":"UP"}}</li>
 *   <li>{@code GET /api/orders} → масив замовлень</li>
 *   <li>{@code GET /api/orders/{id}} → одне замовлення або 404</li>
 * </ul>
 * Інші методи → 405.
 */
public final class ShopHttpApi {
    private static final String ORDERS = "/api/orders";
    private static final String JSON = "application/json; charset=utf-8";

    private final OrderStore orders;
    private final HttpServer server;

    public ShopHttpApi(OrderStore orders, int port) {
        this.orders = orders;
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot bind HTTP API to port " + port, e);
        }
        server.createContext("/api/health", exchange -> respond(exchange, 200, "{\"status\":\"UP\"}"));
        server.createContext("/api/orders", this::handleOrders);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void handleOrders(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            respond(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if (path.equals(ORDERS) || path.equals(ORDERS + "/")) {
            List<String> items = new ArrayList<>();
            orders.findAll().forEach(order -> items.add(toJson(OrderMapper.toDto(order))));
            respond(exchange, 200, "[" + String.join(",", items) + "]");
            return;
        }
        String id = path.substring(ORDERS.length() + 1);
        Optional<String> body = orders.findById(id).map(order -> toJson(OrderMapper.toDto(order)));
        if (body.isPresent()) {
            respond(exchange, 200, body.get());
        } else {
            respond(exchange, 404, "{\"error\":\"Order not found\",\"orderId\":\"" + escape(id) + "\"}");
        }
    }

    static String toJson(OrderDto dto) {
        return "{\"orderId\":\"" + escape(dto.getOrderId()) + "\","
                + "\"customer\":\"" + escape(dto.getCustomerName()) + "\","
                + "\"status\":\"" + dto.getStatus() + "\","
                + "\"items\":" + dto.getItemCount() + ","
                + "\"total\":\"" + dto.getTotal().toPlainString() + "\"}";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", JSON);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
