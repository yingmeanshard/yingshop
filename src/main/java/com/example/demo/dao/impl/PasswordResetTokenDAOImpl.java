package com.example.demo.dao.impl;

import com.example.demo.dao.PasswordResetTokenDAO;
import com.example.demo.model.PasswordResetToken;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetTokenDAOImpl implements PasswordResetTokenDAO {

    private final SessionFactory sessionFactory;

    @Autowired
    public PasswordResetTokenDAOImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session currentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public PasswordResetToken findByToken(String token) {
        return currentSession().createQuery("FROM PasswordResetToken WHERE token = :token", PasswordResetToken.class)
                .setParameter("token", token)
                .uniqueResult();
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        currentSession().saveOrUpdate(token);
        return token;
    }

    @Override
    public void delete(PasswordResetToken token) {
        currentSession().delete(token);
    }
}