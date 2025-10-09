package com.example.demo.dao.impl;

import com.example.demo.dao.UserDAO;
import com.example.demo.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserDAOImpl implements UserDAO {

    @Autowired
    private SessionFactory sessionFactory;

    private Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public List<User> findAll() {
        return getCurrentSession().createQuery("FROM User", User.class).list();
    }

    @Override
    public User findById(Long id) {
        return getCurrentSession().get(User.class, id);
    }

    @Override
    public void save(User user) {
        getCurrentSession().saveOrUpdate(user);
    }

    @Override
    public void delete(Long id) {
        User user = getCurrentSession().get(User.class, id);
        if (user != null) {
            getCurrentSession().delete(user);
        }
    }
}
