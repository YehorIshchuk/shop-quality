package com.kira.shop.application.port;

import com.kira.shop.domain.Product;

/** Контракт складського обліку (низькорівневі операції із залишками). */
public interface StockService {
    int getStock(Product product);

    void add(Product product, int quantity);

    void reserve(Product product, int quantity);

    void release(Product product, int quantity);
}
