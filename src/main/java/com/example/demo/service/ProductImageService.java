package com.example.demo.service;

import com.example.demo.model.ProductImage;
import java.util.List;

public interface ProductImageService {
    List<ProductImage> getImagesByProductId(Long productId);
    void saveProductImage(ProductImage productImage);
    void deleteProductImage(Long id);
    void deleteAllImagesByProductId(Long productId);
    ProductImage getProductImageById(Long id);
}

