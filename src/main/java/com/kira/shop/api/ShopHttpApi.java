package com.kira.shop.api;

import com.kira.shop.application.port.OrderStore;
import com.kira.shop.dto.OrderDto;
import com.kira.shop.dto.OrderMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Read-only HTTP API замовлень на вбудованому {@link HttpServer} JDK (без зовнішніх залежностей).
 * <ul>
 *   <li>{@code GET /api/health} → {@code {"status":"UP"}}</li>
 *   <li>{@code GET /api/orders} → масив замовлень</li>
 *   <li>{@code GET /api/orders/{id}} → одне замовлення або 404</li>
 * </ul>
 * Контракт — {@code docs/api/openapi.yaml}: інші методи → 405 із заголовком {@code Allow: GET},
 * будь-яка неочікувана помилка → 500; тіло помилок — спільний об'єкт {@code {"error": ...}}.
 * Відповіді містять {@code Access-Control-Allow-Origin: *} для «Try it out» у Swagger UI бази знань.
 */
public final class ShopHttpApi {
    private static final String ORDERS = "/api/orders";
    private static final String HEALTH = "/api/health";
    private static final String JSON = "application/json; charset=utf-8";
    private static final Logger LOG = Logger.getLogger(ShopHttpApi.class.getName());

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private final OrderStore orders;
    private final HttpServer server;

    public ShopHttpApi(OrderStore orders, int port) {
        this.orders = orders;
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot bind HTTP API to port " + port, e);
        }
        server.createContext(HEALTH, readOnly(ShopHttpApi::handleHealth));
        server.createContext(ORDERS, readOnly(this::handleOrders));
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

    /** Спільна обгортка маршрутів: лише GET (інакше 405 + Allow), неочікувана помилка → 500 з {@code Error}. */
    private static HttpHandler readOnly(Handler handler) {
        return exchange -> {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().set("Allow", "GET");
                    respond(exchange, 405, error("Method not allowed"));
                    return;
                }
                handler.handle(exchange);
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, "Unhandled HTTP API error", e);
                respond(exchange, 500, error("Internal server error"));
            }
        };
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        if (HEALTH.equals(exchange.getRequestURI().getPath())) {
            respond(exchange, 200, "{\"status\":\"UP\"}");
        } else {
            respond(exchange, 404, error("Not found"));
        }
    }

    private void handleOrders(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!path.equals(ORDERS) && !path.startsWith(ORDERS + "/")) {
            respond(exchange, 404, error("Not found"));
            return;
        }
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

    private static String error(String message) {
        return "{\"error\":\"" + escape(message) + "\"}";
    }

    /** Екранування рядка за RFC 8259: лапки, зворотна коса риска і всі керуючі символи U+0000…U+001F. */
    public static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    appendPlainOrUnicode(out, c);
            }
        }
        return out.toString();
    }

    private static void appendPlainOrUnicode(StringBuilder out, char c) {
        if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
        } else {
            out.append(c);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", JSON);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
