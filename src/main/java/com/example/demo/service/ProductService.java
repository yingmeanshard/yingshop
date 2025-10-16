package com.example.demo.service;

import com.example.demo.model.Product;

import java.util.List;

public interface ProductService {

    List<Product> getAllProducts();

    Product getProductById(Long id);

    List<Product> getProductsByCategory(String category);

    void saveProduct(Product product);

    void deleteProduct(Long id);
}