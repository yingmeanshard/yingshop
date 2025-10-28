package com.example.demo.test;

import com.example.demo.model.Cart;
import com.example.demo.model.Product;
import com.example.demo.service.CartService;
import com.example.demo.service.impl.CartServiceImpl;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class CartServiceTest {

    private CartService cartService;

    @Before
    public void setUp() {
        cartService = new CartServiceImpl();
    }

    @Test
    public void testAddUpdateAndRemoveItems() {
        Cart cart = new Cart();
        Product productA = createProduct(1L, "商品A", new BigDecimal("100.00"));
        Product productB = createProduct(2L, "商品B", new BigDecimal("50.00"));

        cartService.addItem(cart, productA, 2);
        cartService.addItem(cart, productB, 3);
        cartService.addItem(cart, productA, 1);

        assertEquals(3, cart.getItems().get(productA.getId()).getQuantity());
        assertEquals(new BigDecimal("300.00"), cart.getItems().get(productA.getId()).getSubtotal());
        assertEquals(4, cartService.getTotalQuantity(cart));

        cartService.updateItemQuantity(cart, productB.getId(), 5);
        assertEquals(5, cart.getItems().get(productB.getId()).getQuantity());

        cartService.updateItemQuantity(cart, productA.getId(), 0);
        assertFalse(cart.getItems().containsKey(productA.getId()));

        cartService.removeItem(cart, productB.getId());
        assertTrue(cart.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, cartService.calculateTotalPrice(cart));
    }

    private Product createProduct(Long id, String name, BigDecimal price) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(price);
        return product;
    }
}