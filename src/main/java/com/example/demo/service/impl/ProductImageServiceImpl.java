package com.example.demo.service.impl;

import com.example.demo.dao.ProductImageDAO;
import com.example.demo.model.ProductImage;
import com.example.demo.service.ProductImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductImageServiceImpl implements ProductImageService {

    @Autowired
    private ProductImageDAO productImageDAO;

    @Override
    public List<ProductImage> getImagesByProductId(Long productId) {
        return productImageDAO.findByProductId(productId);
    }

    @Override
    public void saveProductImage(ProductImage productImage) {
        productImageDAO.save(productImage);
    }

    @Override
    public void deleteProductImage(Long id) {
        productImageDAO.delete(id);
    }

    @Override
    public void deleteAllImagesByProductId(Long productId) {
        productImageDAO.deleteByProductId(productId);
    }

    @Override
    public ProductImage getProductImageById(Long id) {
        return productImageDAO.findById(id);
    }
}

