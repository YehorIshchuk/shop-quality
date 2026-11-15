package com.kira.shop;

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
import com.kira.shop.service.StoreManager;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;
import java.util.logging.Logger;

/** Демонстраційна точка входу. */
public final class ShopApplication {
    private static final Logger LOG = Logger.getLogger(ShopApplication.class.getName());

    private ShopApplication() {
    }

    public static void main(String[] args) {
        StoreManager store = new StoreManager(new ProductRepository(), new InventoryStore(),
                new FileOrderRepository(), new FakePaymentGateway(), new ConsoleNotificationSender());

        store.registerProduct(new Product("B1", "Clean Code", Category.BOOKS, new BigDecimal("650"), 0.7), 10);
        store.registerProduct(new Product("E1", "Laptop", Category.ELECTRONICS, new BigDecimal("32000"), 2.1), 2);

        Customer kira = new Customer("C1", "Kira", "kira@example.com",
                new Address("Khreshchatyk 1", "Kyiv", "01001", "Ukraine"), CustomerTier.GOLD);

        Order order = store.placeOrder(kira, Map.of("B1", 3, "E1", 1));
        store.payOrder(order.getId());
        LOG.info(store.monthlyReport(YearMonth.now()));
        LOG.info(store.buildCustomerSummary(kira));
    }
}
