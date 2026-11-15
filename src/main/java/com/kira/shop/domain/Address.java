package com.kira.shop.domain;

import java.util.Objects;

/** Адреса доставки (value object). */
public final class Address {
    private static final String HOME_COUNTRY = "Ukraine";

    private final String street;
    private final String city;
    private final String zip;
    private final String country;

    public Address(String street, String city, String zip, String country) {
        this.street = Objects.requireNonNull(street);
        this.city = Objects.requireNonNull(city);
        this.zip = Objects.requireNonNull(zip);
        this.country = Objects.requireNonNull(country);
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getZip() {
        return zip;
    }

    public String getCountry() {
        return country;
    }

    public boolean isDomestic() {
        return HOME_COUNTRY.equalsIgnoreCase(country);
    }

    @Override
    public String toString() {
        return street + ", " + city + " " + zip + ", " + country;
    }
}
