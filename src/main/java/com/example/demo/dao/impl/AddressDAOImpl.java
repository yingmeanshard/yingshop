package com.example.demo.dao.impl;

import com.example.demo.dao.AddressDAO;
import com.example.demo.model.Address;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AddressDAOImpl implements AddressDAO {

    private final SessionFactory sessionFactory;

    @Autowired
    public AddressDAOImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Session currentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public Address findById(Long id) {
        return currentSession().get(Address.class, id);
    }

    @Override
    public List<Address> findByUserId(Long userId) {
        return currentSession().createQuery("FROM Address WHERE user.id = :userId", Address.class)
                .setParameter("userId", userId)
                .list();
    }

    @Override
    public Address save(Address address) {
        currentSession().saveOrUpdate(address);
        return address;
    }

    @Override
    public void delete(Address address) {
        currentSession().delete(address);
    }
}