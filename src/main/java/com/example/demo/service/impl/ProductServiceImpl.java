package com.example.demo.service.impl;

import com.example.demo.dao.ProductDAO;
import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductDAO productDAO;

    @Override
    public List<Product> getAllProducts() {
        return productDAO.findAll();
    }

    @Override
    public Product getProductById(Long id) {
        return productDAO.findById(id);
    }

    @Override
    public List<Product> getProductsByCategory(String category) {
        if (category == null || category.isBlank()) {
            return productDAO.findAll();
        }
        return productDAO.findByCategory(category);
    }

    @Override
    public void saveProduct(Product product) {
        productDAO.save(product);
    }

    @Override
    public void deleteProduct(Long id) {
        productDAO.delete(id);
    }
}