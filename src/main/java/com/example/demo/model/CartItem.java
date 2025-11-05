package com.example.demo.model;

import java.math.BigDecimal;
import java.util.Objects;

public class CartItem {

    private final Long productId;
    private final String name;
    private final BigDecimal unitPrice;
    private int quantity;
    private BigDecimal subtotal;
    private boolean selected = true; // 預設選中

    public CartItem(Long productId, String name, BigDecimal unitPrice, int quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("Product id must not be null");
        }
        this.productId = productId;
        this.name = name;
        this.unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        setQuantity(quantity);
    }

    public Long getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(quantity, 0);
        recalculateSubtotal();
    }

    public void increaseQuantity(int amount) {
        setQuantity(this.quantity + amount);
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    private void recalculateSubtotal() {
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(this.quantity));
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CartItem cartItem = (CartItem) o;
        return Objects.equals(productId, cartItem.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }
}