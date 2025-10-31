package com.example.demo.dao.impl;

import com.example.demo.dao.OrderDAO;
import com.example.demo.model.Order;
import com.example.demo.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderDAOImpl implements OrderDAO {

    private final SessionFactory sessionFactory;

    @Autowired
    public OrderDAOImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public void save(Order order) {
        getCurrentSession().save(order);
    }

    @Override
    public void update(Order order) {
        getCurrentSession().update(order);
    }

    @Override
    public void delete(Order order) {
        getCurrentSession().delete(order);
    }

    @Override
    public Order findById(Long id) {
        return getCurrentSession().get(Order.class, id);
    }

    @Override
    public List<Order> findAll() {
        return getCurrentSession()
                .createQuery("from Order order by createdAt desc", Order.class)
                .list();
    }

    @Override
    public List<Order> findByUser(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        Query<Order> query = getCurrentSession()
                .createQuery("from Order where user.id = :userId order by createdAt desc", Order.class);
        query.setParameter("userId", user.getId());
        return query.list();
    }
}
