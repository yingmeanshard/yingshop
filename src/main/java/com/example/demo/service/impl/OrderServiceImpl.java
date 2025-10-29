package com.example.demo.service.impl;

import com.example.demo.dao.OrderDAO;
import com.example.demo.dao.OrderItemDAO;
import com.example.demo.dao.ProductDAO;
import com.example.demo.model.*;
import com.example.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderDAO orderDAO;
    private final OrderItemDAO orderItemDAO;
    private final ProductDAO productDAO;

    @Autowired
    public OrderServiceImpl(OrderDAO orderDAO, OrderItemDAO orderItemDAO, ProductDAO productDAO) {
        this.orderDAO = orderDAO;
        this.orderItemDAO = orderItemDAO;
        this.productDAO = productDAO;
    }

    @Override
    public Order createOrder(Cart cart, User user) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Cart must contain at least one item to create an order");
        }
        if (user == null) {
            throw new IllegalArgumentException("User must not be null when creating an order");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItemList()) {
            Product product = productDAO.findById(cartItem.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found for id: " + cartItem.getProductId());
            }
            if (cartItem.getQuantity() <= 0) {
                continue;
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            order.addItem(orderItem);
            orderItems.add(orderItem);
            total = total.add(orderItem.getSubtotal());
        }
        order.setTotalPrice(total);

        if (orderItems.isEmpty()) {
            throw new IllegalArgumentException("Cart must contain items with quantity greater than zero");
        }

        orderDAO.save(order);
        // Persist order items explicitly to ensure they are stored in the same transaction.
        for (OrderItem orderItem : orderItems) {
            orderItemDAO.save(orderItem);
        }

        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        if (id == null) {
            return null;
        }
        return orderDAO.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> listOrdersByUser(User user) {
        if (user == null) {
            return List.of();
        }
        return orderDAO.findByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderDAO.findAll();
    }

    @Override
    public Order updateStatus(Long orderId, OrderStatus newStatus) {
        Objects.requireNonNull(newStatus, "New status must not be null");
        if (orderId == null) {
            throw new IllegalArgumentException("Order id must not be null");
        }
        Order order = orderDAO.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found for id: " + orderId);
        }
        OrderStatus currentStatus = order.getStatus();
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new IllegalStateException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }
        order.setStatus(newStatus);
        orderDAO.update(order);
        return order;
    }

    private boolean isValidTransition(OrderStatus currentStatus, OrderStatus nextStatus) {
        if (currentStatus == null) {
            return nextStatus == OrderStatus.PENDING_PAYMENT;
        }
        if (currentStatus == nextStatus) {
            return true;
        }
        switch (currentStatus) {
            case PENDING_PAYMENT:
                return nextStatus == OrderStatus.PAID;
            case PAID:
                return nextStatus == OrderStatus.SHIPPED;
            case SHIPPED:
                return nextStatus == OrderStatus.SHIPPED;
            default:
                return false;
        }
    }
}