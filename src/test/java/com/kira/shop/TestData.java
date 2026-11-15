package com.kira.shop;

import com.kira.shop.domain.Address;
import com.kira.shop.domain.Category;
import com.kira.shop.domain.Customer;
import com.kira.shop.domain.CustomerTier;
import com.kira.shop.domain.Product;

import java.math.BigDecimal;

public final class TestData {
    public static final Address KYIV = new Address("Khreshchatyk 1", "Kyiv", "01001", "Ukraine");
    public static final Address BERLIN = new Address("Unter den Linden 5", "Berlin", "10117", "Germany");

    private TestData() {
    }

    public static Product book() {
        return new Product("B1", "Clean Code", Category.BOOKS, new BigDecimal("500"), 0.5);
    }

    public static Product laptop() {
        return new Product("E1", "Laptop", Category.ELECTRONICS, new BigDecimal("30000"), 2.0);
    }

    public static Customer customer(CustomerTier tier) {
        return new Customer("C-" + tier, "Client " + tier, tier.name().toLowerCase() + "@example.com", KYIV, tier);
    }

    public static Customer foreignCustomer() {
        return new Customer("C-F", "Hans", "hans@example.com", BERLIN, CustomerTier.REGULAR);
    }
}
