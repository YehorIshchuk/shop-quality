package com.kira.shop.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Товар каталогу. */
public class Product {
    private final String id;
    private final String name;
    private final Category category;
    private final BigDecimal price;
    private final double weightKg;

    public Product(String id, String name, Category category, BigDecimal price, double weightKg) {
        if (price.signum() < 0) {
            throw new IllegalArgumentException("Price must not be negative");
        }
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.category = Objects.requireNonNull(category);
        this.price = price;
        this.weightKg = weightKg;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public double getWeightKg() {
        return weightKg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product)) {
            return false;
        }
        return id.equals(((Product) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
