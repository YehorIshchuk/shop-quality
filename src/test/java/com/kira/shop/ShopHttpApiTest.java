package com.kira.shop;

import com.kira.shop.api.ShopHttpApi;
import com.kira.shop.domain.CustomerTier;
import com.kira.shop.domain.Order;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopHttpApiTest {
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> call(ShopHttpApi api, String method, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + api.port() + path))
                .method(method, HttpRequest.BodyPublishers.noBody()).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void servesOrdersHealthAndErrors() throws Exception {
        ShopFixture shop = new ShopFixture();
        Order order = shop.placement.placeOrder(TestData.customer(CustomerTier.GOLD), Map.of("B1", 2));
        ShopHttpApi api = new ShopHttpApi(shop.orders, 0);
        api.start();
        try {
            assertEquals("{\"status\":\"UP\"}", call(api, "GET", "/api/health").body());

            HttpResponse<String> list = call(api, "GET", "/api/orders");
            assertEquals(200, list.statusCode());
            assertTrue(list.body().startsWith("[{\"orderId\":\"" + order.getId() + "\""));

            HttpResponse<String> one = call(api, "GET", "/api/orders/" + order.getId());
            assertEquals(200, one.statusCode());
            assertTrue(one.body().contains("\"status\":\"NEW\""));
            assertTrue(one.body().contains("\"items\":2"));

            assertEquals(404, call(api, "GET", "/api/orders/NOPE").statusCode());
            assertEquals(405, call(api, "POST", "/api/orders").statusCode());
        } finally {
            api.stop();
        }
    }
}
