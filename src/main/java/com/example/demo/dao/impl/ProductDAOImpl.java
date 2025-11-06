package com.example.demo.dao.impl;

import com.example.demo.dao.ProductDAO;
import com.example.demo.model.Product;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductDAOImpl implements ProductDAO {

    @Autowired
    private SessionFactory sessionFactory;

    private Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public List<Product> findAll() {
        return getCurrentSession().createQuery("from Product", Product.class).list();
    }

    @Override
    public List<Product> findListed() {
        Query<Product> query = getCurrentSession()
                .createQuery("from Product where listed = :listed", Product.class);
        query.setParameter("listed", true);
        return query.list();
    }

    @Override
    public Product findById(Long id) {
        return getCurrentSession().get(Product.class, id);
    }

    @Override
    public List<Product> findByCategory(String category) {
        Query<Product> query = getCurrentSession()
                .createQuery("from Product where category = :category", Product.class);
        query.setParameter("category", category);
        return query.list();
    }

    @Override
    public List<Product> findListedByCategory(String category) {
        Query<Product> query = getCurrentSession()
                .createQuery("from Product where category = :category and listed = :listed", Product.class);
        query.setParameter("category", category);
        query.setParameter("listed", true);
        return query.list();
    }

    @Override
    public void save(Product product) {
        getCurrentSession().saveOrUpdate(product);
    }

    @Override
    public void delete(Long id) {
        Product product = findById(id);
        if (product != null) {
            getCurrentSession().delete(product);
        }
    }
}