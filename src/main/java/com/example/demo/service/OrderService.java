package com.example.demo.service;

import com.example.demo.model.Cart;
import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.PaymentMethod;
import com.example.demo.model.User;

import java.util.List;

public interface OrderService {

    Order createOrder(Cart cart, User user, PaymentMethod paymentMethod);

    Order getOrderById(Long id);

    List<Order> listOrdersByUser(User user);

    List<Order> getAllOrders();

    Order updateStatus(Long orderId, OrderStatus newStatus);
}
