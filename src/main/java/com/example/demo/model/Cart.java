package com.example.demo.model;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Cart {

    private final Map<Long, CartItem> items = new LinkedHashMap<>();
    private BigDecimal totalPrice = BigDecimal.ZERO;

    public Map<Long, CartItem> getItems() {
        return items;
    }

    public Collection<CartItem> getItemList() {
        return items.values();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public int getTotalQuantity() {
        return items.values().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public void clear() {
        items.clear();
        totalPrice = BigDecimal.ZERO;
    }

    public void recalculateTotalPrice() {
        totalPrice = items.values().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}