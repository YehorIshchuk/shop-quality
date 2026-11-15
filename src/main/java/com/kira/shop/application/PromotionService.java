package com.kira.shop.application;

import com.kira.shop.application.port.Notifier;
import com.kira.shop.application.port.ProductCatalog;
import com.kira.shop.domain.Category;
import com.kira.shop.domain.Customer;
import com.kira.shop.domain.Product;

import java.util.List;

/** Маркетингові розсилки про товари категорії. */
public class PromotionService {
    private final ProductCatalog catalog;
    private final Notifier notifier;

    public PromotionService(ProductCatalog catalog, Notifier notifier) {
        this.catalog = catalog;
        this.notifier = notifier;
    }

    public void sendPromo(List<Customer> customers, Category category) {
        List<Product> products = catalog.findByCategory(category);
        if (products.isEmpty()) {
            return;
        }
        customers.forEach(customer -> notifier.send(customer.getEmail(), "New in " + category,
                products.size() + " products waiting for you"));
    }
}
