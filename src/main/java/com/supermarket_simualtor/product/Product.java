package com.supermarket_simualtor.product;

import com.supermarket_simualtor.customer.Customer;
import lombok.Data;
import lombok.val;
import org.jetbrains.annotations.NotNull;

@Data
public class Product implements Comparable<Product>, ProductDiscounts, ProductPermissions {
    private final long id;

    private final String name;

    private final boolean weighted;

    private final ProductPermissions permissions;

    private final ProductDiscounts discounts;

    private double weight;

    public Product(long id) {
        this.id = id;
        name = null;
        weight = 0.0;
        weighted = false;
        permissions = null;
        discounts = null;
    }

    public Product(long id, String name, double weight, ProductPermissions permissions, ProductDiscounts discounts) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        weighted = true;
        this.permissions = permissions;
        this.discounts = discounts;
    }

    public Product(long id, String name, ProductPermissions permissions, ProductDiscounts discounts) {
        this.id = id;
        this.name = name;
        weight = 0.0;
        weighted = false;
        this.permissions = permissions;
        this.discounts = discounts;
    }

    public synchronized Product take(double weight) throws NonWeightedTakeException {
        if (!weighted) {
            throw new NonWeightedTakeException("Cannot take some of " + name);
        }
        assert this.weight - weight > 0;
        val next = this.weight - weight;
        this.weight = weight;
        return new Product(id, name, next, permissions, discounts);
    }

    @Override
    public synchronized int compareTo(@NotNull Product other) {
        return Integer.compare((int) id, (int) other.id);
    }

    @Override
    public boolean allowedForChild(Customer customer) {
        assert permissions != null;
        return permissions.allowedForChild(customer);
    }

    @Override
    public double discountForRetired(Customer customer) {
        assert discounts != null;
        return discounts.discountForRetired(customer);
    }

    @Override
    public double applyBonuses() {
        assert discounts != null;
        return discounts.applyBonuses();
    }
}
