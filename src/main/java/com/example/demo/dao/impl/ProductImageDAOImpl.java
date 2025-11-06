package com.example.demo.dao.impl;

import com.example.demo.dao.ProductImageDAO;
import com.example.demo.model.ProductImage;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductImageDAOImpl implements ProductImageDAO {

    @Autowired
    private SessionFactory sessionFactory;

    private Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public List<ProductImage> findByProductId(Long productId) {
        Query<ProductImage> query = getCurrentSession()
                .createQuery("from ProductImage where product.id = :productId order by displayOrder asc, id asc", ProductImage.class);
        query.setParameter("productId", productId);
        return query.list();
    }

    @Override
    public void save(ProductImage productImage) {
        getCurrentSession().saveOrUpdate(productImage);
    }

    @Override
    public void delete(Long id) {
        ProductImage productImage = getCurrentSession().get(ProductImage.class, id);
        if (productImage != null) {
            getCurrentSession().delete(productImage);
        }
    }

    @Override
    public void deleteByProductId(Long productId) {
        Query<?> query = getCurrentSession()
                .createQuery("delete from ProductImage where product.id = :productId");
        query.setParameter("productId", productId);
        query.executeUpdate();
    }

    @Override
    public ProductImage findById(Long id) {
        return getCurrentSession().get(ProductImage.class, id);
    }
}

