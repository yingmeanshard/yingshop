package com.example.demo.service.impl;

import com.example.demo.model.Cart;
import com.example.demo.model.CartItem;
import com.example.demo.model.Product;
import com.example.demo.service.CartService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    @Override
    public void addItem(Cart cart, Product product, int quantity) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart must not be null");
        }
        if (product == null) {
            throw new IllegalArgumentException("Product must not be null");
        }

        int safeQuantity = quantity <= 0 ? 1 : quantity;
        Map<Long, CartItem> items = cart.getItems();
        CartItem existingItem = items.get(product.getId());
        if (existingItem != null) {
            existingItem.increaseQuantity(safeQuantity);
        } else {
            CartItem cartItem = new CartItem(product.getId(), product.getName(), product.getPrice(), safeQuantity);
            items.put(product.getId(), cartItem);
        }
        cart.recalculateTotalPrice();
    }

    @Override
    public void updateItemQuantity(Cart cart, Long productId, int quantity) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart must not be null");
        }
        if (productId == null) {
            throw new IllegalArgumentException("Product id must not be null");
        }
        Map<Long, CartItem> items = cart.getItems();
        CartItem existingItem = items.get(productId);
        if (existingItem == null) {
            return;
        }
        if (quantity <= 0) {
            items.remove(productId);
        } else {
            existingItem.setQuantity(quantity);
        }
        cart.recalculateTotalPrice();
    }

    @Override
    public void removeItem(Cart cart, Long productId) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart must not be null");
        }
        if (productId == null) {
            throw new IllegalArgumentException("Product id must not be null");
        }
        cart.getItems().remove(productId);
        cart.recalculateTotalPrice();
    }

    @Override
    public BigDecimal calculateTotalPrice(Cart cart) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart must not be null");
        }
        cart.recalculateTotalPrice();
        return cart.getTotalPrice();
    }

    @Override
    public int getTotalQuantity(Cart cart) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart must not be null");
        }
        return cart.getTotalQuantity();
    }
    
    @Override
    public void selectAddress(Cart cart, Long addressId) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart must not be null");
        }
        cart.setSelectedAddressId(addressId);
    }
}