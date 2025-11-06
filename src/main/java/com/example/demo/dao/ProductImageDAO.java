package com.example.demo.dao;

import com.example.demo.model.ProductImage;
import java.util.List;

public interface ProductImageDAO {
    List<ProductImage> findByProductId(Long productId);
    void save(ProductImage productImage);
    void delete(Long id);
    void deleteByProductId(Long productId);
    ProductImage findById(Long id);
}

