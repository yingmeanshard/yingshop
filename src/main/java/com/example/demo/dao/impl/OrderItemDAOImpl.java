package com.example.demo.dao.impl;

import com.example.demo.dao.OrderItemDAO;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderItemDAOImpl implements OrderItemDAO {

    private final SessionFactory sessionFactory;

    @Autowired
    public OrderItemDAOImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public void save(OrderItem orderItem) {
        getCurrentSession().save(orderItem);
    }

    @Override
    public void update(OrderItem orderItem) {
        getCurrentSession().update(orderItem);
    }

    @Override
    public void delete(OrderItem orderItem) {
        getCurrentSession().delete(orderItem);
    }

    @Override
    public OrderItem findById(Long id) {
        return getCurrentSession().get(OrderItem.class, id);
    }

    @Override
    public List<OrderItem> findByOrder(Order order) {
        Query<OrderItem> query = getCurrentSession()
                .createQuery("from OrderItem where order = :order", OrderItem.class);
        query.setParameter("order", order);
        return query.list();
    }
}