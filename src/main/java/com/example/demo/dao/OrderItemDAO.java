package com.example.demo.dao;

import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;

import java.util.List;

public interface OrderItemDAO {

    void save(OrderItem orderItem);

    void update(OrderItem orderItem);

    void delete(OrderItem orderItem);

    OrderItem findById(Long id);

    List<OrderItem> findByOrder(Order order);
}