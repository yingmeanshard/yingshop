package com.example.demo.dao.impl;

import com.example.demo.dao.OrderDAO;
import com.example.demo.model.Order;
import com.example.demo.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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
        List<Order> results = getCurrentSession()
                .createQuery(
                        "select distinct o from Order o left join fetch o.items order by o.createdAt desc, o.id desc",
                        Order.class)
                .list();
        return deduplicateById(results);
    }

    @Override
    public List<Order> findByUser(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        Query<Order> query = getCurrentSession()
                .createQuery(
                        "select distinct o from Order o left join fetch o.items "
                                + "where o.user.id = :userId order by o.createdAt desc, o.id desc",
                        Order.class);
        query.setParameter("userId", user.getId());
        List<Order> results = query.list();
        return deduplicateById(results);
    }

    private List<Order> deduplicateById(List<Order> orders) {
        if (orders.isEmpty()) {
            return orders;
        }
        List<Order> unique = new ArrayList<>(orders.size());
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        for (Order order : orders) {
            Long id = order != null ? order.getId() : null;
            if (id == null || seen.add(id)) {
                unique.add(order);
            }
        }
        return unique;
    }
}
