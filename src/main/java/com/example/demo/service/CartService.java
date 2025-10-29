package com.example.demo.service;

import com.example.demo.model.Cart;
import com.example.demo.model.Product;

import java.math.BigDecimal;

public interface CartService {

    void addItem(Cart cart, Product product, int quantity);

    void updateItemQuantity(Cart cart, Long productId, int quantity);

    void removeItem(Cart cart, Long productId);

    BigDecimal calculateTotalPrice(Cart cart);

    int getTotalQuantity(Cart cart);
    
    void selectAddress(Cart cart, Long addressId);
}