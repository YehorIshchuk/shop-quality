package com.kira.shop.infrastructure;

import com.kira.shop.domain.Category;
import com.kira.shop.domain.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory сховище каталогу товарів. */
public class ProductRepository {
    private final Map<String, Product> products = new LinkedHashMap<>();

    public void save(Product product) {
        products.put(product.getId(), product);
    }

    public Optional<Product> findById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    public List<Product> findByCategory(Category category) {
        List<Product> result = new ArrayList<>();
        for (Product product : products.values()) {
            if (product.getCategory() == category) {
                result.add(product);
            }
        }
        return result;
    }

    public int count() {
        return products.size();
    }
}
