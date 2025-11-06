package com.example.demo.dao;

import com.example.demo.model.Product;

import java.util.List;

public interface ProductDAO {

    List<Product> findAll();

    List<Product> findListed();

    Product findById(Long id);

    List<Product> findByCategory(String category);

    List<Product> findListedByCategory(String category);

    void save(Product product);

    void delete(Long id);
}