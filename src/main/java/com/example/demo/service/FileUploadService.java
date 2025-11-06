package com.example.demo.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileUploadService {
    String uploadProductImage(MultipartFile file, Long productId) throws IOException;
    void deleteProductImage(String imagePath) throws IOException;
    
    /**
     * 上傳多張商品圖片
     * @param files 圖片文件列表
     * @param productId 商品ID
     * @return 圖片URL列表
     * @throws IOException 上傳失敗時拋出
     */
    List<String> uploadProductImages(List<MultipartFile> files, Long productId) throws IOException;
    
    /**
     * 驗證圖片文件
     * @param file 圖片文件
     * @return 驗證錯誤訊息，null表示驗證通過
     */
    String validateImageFile(MultipartFile file);
}

