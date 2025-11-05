package com.example.demo.service;

import com.example.demo.model.Cart;
import com.example.demo.model.DeliveryMethod;
import com.example.demo.model.DeliveryPaymentMethod;
import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.PaymentMethod;
import com.example.demo.model.User;

import java.util.List;

public interface OrderService {

    Order createOrder(Cart cart, User user, PaymentMethod paymentMethod);

    Order createOrder(Cart cart, User user, PaymentMethod paymentMethod, DeliveryMethod deliveryMethod,
                     String recipientName, String recipientPhone, String recipientEmail, String recipientAddress);

    Order createOrder(Cart cart, User user, DeliveryPaymentMethod deliveryPaymentMethod,
                     String recipientName, String recipientPhone, String recipientEmail, String recipientAddress);

    Order getOrderById(Long id);

    List<Order> listOrdersByUser(User user);

    List<Order> getAllOrders();

    Order updateStatus(Long orderId, OrderStatus newStatus);
}
