package com.example.demo.test;

import com.example.demo.dao.OrderDAO;
import com.example.demo.dao.ProductDAO;
import com.example.demo.model.*;
import com.example.demo.service.impl.OrderServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OrderServiceImplTest {

    @Mock
    private OrderDAO orderDAO;
    @Mock
    private ProductDAO productDAO;

    private OrderServiceImpl orderService;

    @Before
    public void setUp() {
        orderService = new OrderServiceImpl(orderDAO, productDAO);
    }

    @Test
    public void testCreateOrderInitialStatusAndTotals() {
        Cart cart = new Cart();
        cart.getItems().put(1L, new CartItem(1L, "測試商品", BigDecimal.valueOf(120), 2));

        User user = new User();
        user.setId(5L);

        Product product = new Product();
        product.setId(1L);
        product.setName("測試商品");
        product.setPrice(BigDecimal.valueOf(120));

        when(productDAO.findById(1L)).thenReturn(product);
        doAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setId(99L);
            return null;
        }).when(orderDAO).save(any(Order.class));

        Order order = orderService.createOrder(cart, user, PaymentMethod.CASH_ON_DELIVERY);

        assertNotNull(order);
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        assertEquals(PaymentMethod.CASH_ON_DELIVERY, order.getPaymentMethod());
        assertNotNull("建立訂單時計錄建立時間", order.getCreatedAt());
        assertEquals(BigDecimal.valueOf(240), order.getTotalPrice());
        assertEquals(1, order.getItems().size());
        assertEquals(Long.valueOf(99L), order.getId());

        verify(orderDAO).save(order);
        OrderItem savedItem = order.getItems().get(0);
        assertEquals(2, savedItem.getQuantity());
        assertEquals(product, savedItem.getProduct());
    }

    @Test
    public void testUpdateStatusValidTransition() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        when(orderDAO.findById(1L)).thenReturn(order);

        Order updated = orderService.updateStatus(1L, OrderStatus.PAID);

        assertEquals(OrderStatus.PAID, updated.getStatus());
        verify(orderDAO).update(order);
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdateStatusInvalidTransition() {
        Order order = new Order();
        order.setId(2L);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        when(orderDAO.findById(2L)).thenReturn(order);

        orderService.updateStatus(2L, OrderStatus.SHIPPED);
    }
}
