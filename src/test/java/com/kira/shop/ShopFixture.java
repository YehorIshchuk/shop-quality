package com.kira.shop;

import com.kira.shop.application.CatalogService;
import com.kira.shop.application.CustomerInsightsService;
import com.kira.shop.application.OrderCancellationService;
import com.kira.shop.application.OrderPaymentService;
import com.kira.shop.application.OrderPlacementService;
import com.kira.shop.application.OrderShippingService;
import com.kira.shop.application.PricingService;
import com.kira.shop.application.PromotionService;
import com.kira.shop.application.SalesLedger;
import com.kira.shop.application.SalesReportingService;
import com.kira.shop.application.StockAllocator;
import com.kira.shop.infrastructure.ConsoleNotificationSender;
import com.kira.shop.infrastructure.FakePaymentGateway;
import com.kira.shop.infrastructure.FileOrderRepository;
import com.kira.shop.infrastructure.InventoryStore;
import com.kira.shop.infrastructure.ProductRepository;

/** Тестовий composition root: зв'язує реальні адаптери з сервісами через інтерфейси. */
final class ShopFixture {
    final ProductRepository catalog = new ProductRepository();
    final InventoryStore inventory = new InventoryStore();
    final FileOrderRepository orders = new FileOrderRepository();
    final ConsoleNotificationSender notifications = new ConsoleNotificationSender();
    final SalesLedger ledger = new SalesLedger();
    final StockAllocator allocator = new StockAllocator(inventory);

    final CatalogService catalogService = new CatalogService(catalog, inventory);
    final OrderPlacementService placement =
            new OrderPlacementService(catalog, allocator, new PricingService(), orders, notifications);
    final OrderPaymentService payments =
            new OrderPaymentService(orders, new FakePaymentGateway(), ledger, notifications);
    final OrderShippingService shipping = new OrderShippingService(orders, notifications);
    final OrderCancellationService cancellation =
            new OrderCancellationService(orders, allocator, payments, notifications);
    final PromotionService promotions = new PromotionService(catalog, notifications);
    final CustomerInsightsService insights = new CustomerInsightsService(orders);
    final SalesReportingService reporting = new SalesReportingService(orders);

    ShopFixture() {
        catalogService.registerProduct(TestData.book(), 10);
        catalogService.registerProduct(TestData.laptop(), 1);
    }

    int stockOf(String productId) {
        return inventory.getStock(catalog.require(productId));
    }
}
