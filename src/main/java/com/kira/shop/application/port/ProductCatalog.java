package com.kira.shop.application.port;

import com.kira.shop.domain.Category;
import com.kira.shop.domain.Product;

import java.util.List;
import java.util.Optional;

/** Контракт каталогу товарів. */
public interface ProductCatalog {
    void save(Product product);

    Optional<Product> findById(String id);

    List<Product> findAll();

    List<Product> findByCategory(Category category);

    default Product require(String id) {
        return findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown product " + id));
    }
}
