package com.kira.shop;

import com.kira.shop.domain.Category;
import com.kira.shop.domain.Customer;
import com.kira.shop.domain.CustomerTier;
import com.kira.shop.domain.Order;
import com.kira.shop.domain.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopServicesTest {
    private ShopFixture shop;

    @BeforeEach
    void setUp() {
        shop = new ShopFixture();
    }

    @Test
    void placesOrderWithDiscountAndFreeShipping() {
        Order order = shop.placement.placeOrder(TestData.customer(CustomerTier.GOLD), Map.of("B1", 4));
        // 2000 subtotal, GOLD 5% + books bonus 2% = 140
        assertEquals(new BigDecimal("140.00"), order.getDiscount());
        assertEquals(BigDecimal.ZERO, order.getShippingCost());
        assertEquals(6, shop.stockOf("B1"));
        assertEquals(1, shop.notifications.getSent().size());
    }

    @Test
    void foreignShippingIsCharged() {
        Order order = shop.placement.placeOrder(TestData.foreignCustomer(), Map.of("B1", 1));
        assertEquals(new BigDecimal("570.00"), order.getShippingCost());
        assertEquals(BigDecimal.ZERO.setScale(2), order.getDiscount());
    }

    @Test
    void premiumCustomerGetsPartialStock() {
        Order order = shop.placement.placeOrder(TestData.customer(CustomerTier.PLATINUM), Map.of("E1", 3));
        assertEquals(1, order.totalQuantity());
        assertEquals(0, shop.stockOf("E1"));
    }

    @Test
    void regularCustomerRejectedWhenStockLow() {
        Customer regular = TestData.customer(CustomerTier.REGULAR);
        Map<String, Integer> cart = Map.of("E1", 3);
        assertThrows(IllegalStateException.class, () -> shop.placement.placeOrder(regular, cart));
    }

    @Test
    void premiumCustomerRejectedWhenOutOfStock() {
        shop.placement.placeOrder(TestData.customer(CustomerTier.GOLD), Map.of("E1", 1));
        Customer gold = TestData.customer(CustomerTier.GOLD);
        Map<String, Integer> cart = Map.of("E1", 1);
        assertThrows(IllegalStateException.class, () -> shop.placement.placeOrder(gold, cart));
    }

    @Test
    void invalidInputsRejected() {
        Customer regular = TestData.customer(CustomerTier.REGULAR);
        Map<String, Integer> unknown = Map.of("X", 1);
        Map<String, Integer> zero = Map.of("B1", 0);
        Map<String, Integer> empty = Map.of();
        assertThrows(IllegalArgumentException.class, () -> shop.placement.placeOrder(regular, unknown));
        assertThrows(IllegalArgumentException.class, () -> shop.placement.placeOrder(regular, zero));
        assertThrows(IllegalArgumentException.class, () -> shop.placement.placeOrder(regular, empty));
        assertThrows(IllegalArgumentException.class, () -> shop.placement.placeOrder(null, zero));
    }

    @Test
    void payShipDeliverAndReport() {
        Customer gold = TestData.customer(CustomerTier.GOLD);
        Order order = shop.placement.placeOrder(gold, Map.of("E1", 1));
        assertTrue(shop.payments.pay(order.getId()));
        shop.shipping.ship(order.getId());
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        shop.shipping.deliver(order.getId());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertTrue(gold.getLoyaltyPoints() > 0);
        assertEquals(Map.of("E1", 1), shop.ledger.top(5));
        assertTrue(shop.reporting.monthlyReport(YearMonth.now()).contains(order.getId()));
        String summary = shop.insights.buildCustomerSummary(gold);
        assertTrue(summary.contains("Premium purchase: Laptop"));
        assertTrue(summary.contains("Paid orders: 1"));
    }

    @Test
    void cancelPaidOrderReleasesStockAndRefunds() {
        Order order = shop.placement.placeOrder(TestData.customer(CustomerTier.SILVER), Map.of("B1", 2));
        shop.payments.pay(order.getId());
        shop.cancellation.cancel(order.getId());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(10, shop.stockOf("B1"));
        assertTrue(shop.ledger.top(5).isEmpty());
        assertTrue(shop.notifications.getSent().stream().anyMatch(m -> m.contains("Refund issued")));
        assertTrue(shop.insights.buildCustomerSummary(order.getCustomer()).contains("cancelled: 1"));
    }

    @Test
    void cancelUnpaidOrderDoesNotRefund() {
        Order order = shop.placement.placeOrder(TestData.customer(CustomerTier.SILVER), Map.of("B1", 2));
        shop.cancellation.cancel(order.getId());
        shop.payments.refund(order.getId());
        assertFalse(shop.notifications.getSent().stream().anyMatch(m -> m.contains("Refund issued")));
    }

    @Test
    void customerSummaryRecommendations() {
        Customer silver = TestData.customer(CustomerTier.SILVER);
        shop.catalogService.restock("E1", 5);
        Order order = shop.placement.placeOrder(silver, Map.of("E1", 1));
        shop.payments.pay(order.getId());
        assertTrue(shop.insights.buildCustomerSummary(silver).contains("upgrade to GOLD"));
        Order second = shop.placement.placeOrder(silver, Map.of("E1", 1));
        shop.payments.pay(second.getId());
        assertTrue(shop.insights.buildCustomerSummary(silver).contains("upgrade to PLATINUM"));
        assertTrue(shop.insights.buildCustomerSummary(TestData.customer(CustomerTier.REGULAR)).contains("keep REGULAR"));
        assertTrue(shop.insights.buildCustomerSummaryForMonth(silver, YearMonth.now().minusMonths(1)).contains("Paid orders: 0"));
    }

    @Test
    void stockQueriesAndPromo() {
        assertTrue(shop.catalogService.isAvailable("B1", 10));
        assertFalse(shop.catalogService.isAvailable("E1", 2));
        assertEquals(List.of("Laptop: 1"), shop.catalogService.lowStockReport());
        shop.promotions.sendPromo(List.of(TestData.customer(CustomerTier.REGULAR)), Category.BOOKS);
        shop.promotions.sendPromo(List.of(TestData.customer(CustomerTier.REGULAR)), Category.TOYS);
        assertEquals(1, shop.notifications.getSent().size());
        assertThrows(IllegalArgumentException.class, () -> shop.catalogService.restock("X", 1));
    }

    @Test
    void exportsOrders() throws IOException {
        shop.placement.placeOrder(TestData.customer(CustomerTier.REGULAR), Map.of("B1", 1));
        assertEquals(1, shop.reporting.exportOrderLines().size());
        Path file = Files.createTempFile("orders", ".csv");
        shop.orders.exportCsv(file);
        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).startsWith("ORD-1;"));
        Files.deleteIfExists(file);
    }

    @Test
    void failedPaymentNotifiesCustomer() {
        shop.catalogService.restock("E1", 5);
        Order order = shop.placement.placeOrder(TestData.customer(CustomerTier.REGULAR), Map.of("E1", 2));
        assertFalse(shop.payments.pay(order.getId()));
        assertEquals(OrderStatus.NEW, order.getStatus());
        assertThrows(IllegalArgumentException.class, () -> shop.payments.pay("missing"));
    }
}
