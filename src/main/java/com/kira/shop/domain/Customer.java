package com.kira.shop.domain;

import java.util.Objects;

/** Клієнт магазину. */
public class Customer {
    private final String id;
    private final String name;
    private final String email;
    private final Address address;
    private CustomerTier tier;
    private int loyaltyPoints;

    public Customer(String id, String name, String email, Address address, CustomerTier tier) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.email = Objects.requireNonNull(email);
        this.address = Objects.requireNonNull(address);
        this.tier = Objects.requireNonNull(tier);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Address getAddress() {
        return address;
    }

    public CustomerTier getTier() {
        return tier;
    }

    public void setTier(CustomerTier tier) {
        this.tier = Objects.requireNonNull(tier);
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void addLoyaltyPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Points must be positive");
        }
        loyaltyPoints += points;
    }
}
