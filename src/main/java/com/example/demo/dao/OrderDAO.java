package com.example.demo.dao;

import com.example.demo.model.Order;
import com.example.demo.model.User;

import java.util.List;

public interface OrderDAO {

    void save(Order order);

    void update(Order order);

    void delete(Order order);

    Order findById(Long id);

    List<Order> findAll();

    List<Order> findByUser(User user);
}