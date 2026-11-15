package com.kira.shop;

import com.kira.shop.domain.Category;
import com.kira.shop.domain.Customer;
import com.kira.shop.domain.CustomerTier;
import com.kira.shop.domain.Order;
import com.kira.shop.domain.OrderStatus;
import com.kira.shop.infrastructure.ConsoleNotificationSender;
import com.kira.shop.infrastructure.FakePaymentGateway;
import com.kira.shop.infrastructure.FileOrderRepository;
import com.kira.shop.infrastructure.InventoryStore;
import com.kira.shop.infrastructure.ProductRepository;
import com.kira.shop.service.StoreManager;
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

class StoreManagerTest {
    private InventoryStore inventory;
    private FileOrderRepository orders;
    private ConsoleNotificationSender notifications;
    private StoreManager store;

    @BeforeEach
    void setUp() {
        inventory = new InventoryStore();
        orders = new FileOrderRepository();
        notifications = new ConsoleNotificationSender();
        store = new StoreManager(new ProductRepository(), inventory, orders, new FakePaymentGateway(), notifications);
        store.registerProduct(TestData.book(), 10);
        store.registerProduct(TestData.laptop(), 1);
    }

    @Test
    void placesOrderWithDiscountAndFreeShipping() {
        Order order = store.placeOrder(TestData.customer(CustomerTier.GOLD), Map.of("B1", 4));
        // 2000 subtotal, GOLD 5% + books bonus 2% = 140
        assertEquals(new BigDecimal("140.00"), order.getDiscount());
        assertEquals(BigDecimal.ZERO, order.getShippingCost());
        assertEquals(6, inventory.getStock("B1"));
        assertEquals(1, notifications.getSent().size());
    }

    @Test
    void foreignShippingIsCharged() {
        Order order = store.placeOrder(TestData.foreignCustomer(), Map.of("B1", 1));
        assertEquals(new BigDecimal("570.00"), order.getShippingCost());
        assertEquals(BigDecimal.ZERO.setScale(2), order.getDiscount());
    }

    @Test
    void goldCustomerGetsPartialStock() {
        Order order = store.placeOrder(TestData.customer(CustomerTier.PLATINUM), Map.of("E1", 3));
        assertEquals(1, order.totalQuantity());
        assertEquals(0, inventory.getStock("E1"));
    }

    @Test
    void regularCustomerRejectedWhenStockLow() {
        Customer regular = TestData.customer(CustomerTier.REGULAR);
        Map<String, Integer> cart = Map.of("E1", 3);
        assertThrows(IllegalStateException.class, () -> store.placeOrder(regular, cart));
    }

    @Test
    void invalidInputsRejected() {
        Customer regular = TestData.customer(CustomerTier.REGULAR);
        Map<String, Integer> unknown = Map.of("X", 1);
        Map<String, Integer> zero = Map.of("B1", 0);
        Map<String, Integer> empty = Map.of();
        assertThrows(IllegalArgumentException.class, () -> store.placeOrder(regular, unknown));
        assertThrows(IllegalArgumentException.class, () -> store.placeOrder(regular, zero));
        assertThrows(IllegalArgumentException.class, () -> store.placeOrder(regular, empty));
        assertThrows(IllegalArgumentException.class, () -> store.placeOrder(null, zero));
    }

    @Test
    void payShipAndReport() {
        Customer gold = TestData.customer(CustomerTier.GOLD);
        Order order = store.placeOrder(gold, Map.of("E1", 1));
        assertTrue(store.payOrder(order.getId()));
        store.shipOrder(order.getId());
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertTrue(gold.getLoyaltyPoints() > 0);
        assertEquals(Map.of("E1", 1), store.topProducts(5));
        assertTrue(store.monthlyReport(YearMonth.now()).contains(order.getId()));
        String summary = store.buildCustomerSummary(gold);
        assertTrue(summary.contains("Premium purchase: Laptop"));
        assertTrue(summary.contains("Paid orders: 1"));
    }

    @Test
    void cancelPaidOrderReleasesStock() {
        Order order = store.placeOrder(TestData.customer(CustomerTier.SILVER), Map.of("B1", 2));
        store.payOrder(order.getId());
        store.cancelOrder(order.getId());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(10, inventory.getStock("B1"));
        assertTrue(store.buildCustomerSummary(order.getCustomer()).contains("cancelled: 1"));
    }

    @Test
    void customerSummaryRecommendations() {
        Customer silver = TestData.customer(CustomerTier.SILVER);
        store.restock("E1", 5);
        Order order = store.placeOrder(silver, Map.of("E1", 1));
        store.payOrder(order.getId());
        assertTrue(store.buildCustomerSummary(silver).contains("upgrade to GOLD"));
        Order second = store.placeOrder(silver, Map.of("E1", 1));
        store.payOrder(second.getId());
        assertTrue(store.buildCustomerSummary(silver).contains("upgrade to PLATINUM"));
        assertTrue(store.buildCustomerSummary(TestData.customer(CustomerTier.REGULAR)).contains("keep REGULAR"));
    }

    @Test
    void stockQueriesAndPromo() {
        assertTrue(store.isAvailable("B1", 10));
        assertFalse(store.isAvailable("E1", 2));
        assertEquals(List.of("Laptop: 1"), store.lowStockReport());
        store.sendPromo(List.of(TestData.customer(CustomerTier.REGULAR)), Category.BOOKS);
        store.sendPromo(List.of(TestData.customer(CustomerTier.REGULAR)), Category.TOYS);
        assertEquals(1, notifications.getSent().size());
        assertThrows(IllegalArgumentException.class, () -> store.restock("X", 1));
    }

    @Test
    void exportsOrders() throws IOException {
        store.placeOrder(TestData.customer(CustomerTier.REGULAR), Map.of("B1", 1));
        assertEquals(1, store.exportOrderLines().size());
        Path file = Files.createTempFile("orders", ".csv");
        store.exportOrders(file);
        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).startsWith("ORD-1;"));
        Files.deleteIfExists(file);
    }

    @Test
    void failedPaymentNotifiesCustomer() {
        store.restock("E1", 5);
        Order order = store.placeOrder(TestData.customer(CustomerTier.REGULAR), Map.of("E1", 2));
        assertFalse(store.payOrder(order.getId()));
        assertEquals(OrderStatus.NEW, order.getStatus());
        assertThrows(IllegalArgumentException.class, () -> store.payOrder("missing"));
    }
}
