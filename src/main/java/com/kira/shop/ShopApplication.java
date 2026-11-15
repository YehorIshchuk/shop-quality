package com.kira.shop;

import com.kira.shop.application.CatalogService;
import com.kira.shop.application.CustomerInsightsService;
import com.kira.shop.application.OrderPaymentService;
import com.kira.shop.application.OrderPlacementService;
import com.kira.shop.application.PricingService;
import com.kira.shop.application.SalesLedger;
import com.kira.shop.application.SalesReportingService;
import com.kira.shop.application.StockAllocator;
import com.kira.shop.domain.Address;
import com.kira.shop.domain.Category;
import com.kira.shop.domain.Customer;
import com.kira.shop.domain.CustomerTier;
import com.kira.shop.domain.Order;
import com.kira.shop.domain.Product;
import com.kira.shop.infrastructure.ConsoleNotificationSender;
import com.kira.shop.infrastructure.FakePaymentGateway;
import com.kira.shop.infrastructure.FileOrderRepository;
import com.kira.shop.infrastructure.InventoryStore;
import com.kira.shop.infrastructure.ProductRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Демонстраційна точка входу та Composition Root: тільки тут конкретні реалізації
 * зв'язуються з інтерфейсами (Dependency Injection через конструктори).
 */
public final class ShopApplication {
    private static final Logger LOG = Logger.getLogger(ShopApplication.class.getName());

    private ShopApplication() {
    }

    public static void main(String[] args) {
        ProductRepository catalog = new ProductRepository();
        InventoryStore inventory = new InventoryStore();
        FileOrderRepository orders = new FileOrderRepository();
        ConsoleNotificationSender notifier = new ConsoleNotificationSender();
        StockAllocator allocator = new StockAllocator(inventory);

        CatalogService catalogService = new CatalogService(catalog, inventory);
        OrderPlacementService placement = new OrderPlacementService(catalog, allocator, new PricingService(), orders, notifier);
        OrderPaymentService payments = new OrderPaymentService(orders, new FakePaymentGateway(), new SalesLedger(), notifier);

        catalogService.registerProduct(new Product("B1", "Clean Code", Category.BOOKS, new BigDecimal("650"), 0.7), 10);
        catalogService.registerProduct(new Product("E1", "Laptop", Category.ELECTRONICS, new BigDecimal("32000"), 2.1), 2);

        Customer kira = new Customer("C1", "Kira", "kira@example.com",
                new Address("Khreshchatyk 1", "Kyiv", "01001", "Ukraine"), CustomerTier.GOLD);

        Order order = placement.placeOrder(kira, Map.of("B1", 3, "E1", 1));
        payments.pay(order.getId());
        LOG.info(new SalesReportingService(orders).monthlyReport(YearMonth.now()));
        LOG.info(new CustomerInsightsService(orders).buildCustomerSummary(kira));
    }
}
