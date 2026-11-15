package com.kira.shop.application;

import com.kira.shop.application.port.ProductCatalog;
import com.kira.shop.application.port.StockService;
import com.kira.shop.domain.Product;

import java.util.List;

/** Каталог і складські залишки: реєстрація, поповнення, наявність. */
public class CatalogService {
    private static final int LOW_STOCK_LEVEL = 3;

    private final ProductCatalog catalog;
    private final StockService stock;

    public CatalogService(ProductCatalog catalog, StockService stock) {
        this.catalog = catalog;
        this.stock = stock;
    }

    public void registerProduct(Product product, int initialStock) {
        catalog.save(product);
        if (initialStock > 0) {
            stock.add(product, initialStock);
        }
    }

    public void restock(String productId, int quantity) {
        stock.add(catalog.require(productId), quantity);
    }

    public boolean isAvailable(String productId, int quantity) {
        return stock.getStock(catalog.require(productId)) >= quantity;
    }

    public List<String> lowStockReport() {
        return catalog.findAll().stream()
                .filter(product -> stock.getStock(product) <= LOW_STOCK_LEVEL)
                .map(product -> product.getName() + ": " + stock.getStock(product))
                .toList();
    }
}
