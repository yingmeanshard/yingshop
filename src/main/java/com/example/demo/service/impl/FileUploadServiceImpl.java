package com.example.demo.service.impl;

import com.example.demo.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadServiceImpl.class);
    
    // 專案目錄的資源路徑（開發環境）
    private static final String PROJECT_UPLOAD_DIR = "src/main/webapp/resources/images/products/";
    // Web 資源路徑（用於瀏覽器訪問）
    private static final String RESOURCE_PATH = "/resources/images/products/";
    
    @Autowired(required = false)
    private ServletContext servletContext;

    
    // 允許的圖片類型
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    // 允許的檔案擴展名
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );
    
    // 最大檔案大小：5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    
    // 縮圖最大寬度
    private static final int THUMBNAIL_MAX_WIDTH = 800;
    
    // 縮圖最大高度
    private static final int THUMBNAIL_MAX_HEIGHT = 800;

    @Override
    public String uploadProductImage(MultipartFile file, Long productId) throws IOException {
        log.info("========== 開始上傳圖片 ==========");
        log.info("Product ID: {}", productId);
        
        if (file == null || file.isEmpty()) {
            log.warn("檔案為空或 null，跳過上傳");
            return null;
        }

        log.info("原始檔案名稱: {}", file.getOriginalFilename());
        log.info("檔案大小: {} bytes", file.getSize());
        log.info("檔案類型: {}", file.getContentType());

        // 驗證圖片
        String validationError = validateImageFile(file);
        if (validationError != null) {
            log.error("圖片驗證失敗: {}", validationError);
            throw new IOException(validationError);
        }
        log.info("✓ 圖片驗證通過");

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = "product_" + productId + "_" + UUID.randomUUID().toString() + extension;
        log.info("生成的檔案名稱: {}", filename);

        // 處理圖片（壓縮和縮圖）
        BufferedImage image = null;
        try {
            image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IOException("無法讀取圖片文件");
            }
            log.info("✓ 圖片讀取成功，尺寸: {}x{}", image.getWidth(), image.getHeight());
        } catch (Exception e) {
            log.error("✗ 讀取圖片失敗: {}", e.getMessage(), e);
            throw new IOException("無法讀取圖片文件: " + e.getMessage(), e);
        }
        
        BufferedImage processedImage = resizeImage(image);
        log.info("✓ 圖片處理完成");
        
        // 獲取圖片格式
        String formatName = extension.substring(1).toLowerCase();
        if (formatName.equals("jpg")) {
            formatName = "jpeg";
        }
        log.info("圖片格式: {}", formatName);
        
        // 保存文件到專案目錄（確保開發環境能看到）
        // 從 ServletContext 獲取專案根目錄
        Path projectPath = null;
        
        if (servletContext != null) {
            try {
                String webappRealPath = servletContext.getRealPath("/");
                if (webappRealPath != null && !webappRealPath.isBlank()) {
                    // webappRealPath 通常是: .../target/yingshop/ 或 .../wtpwebapps/yingshop/
                    // 或者在 Eclipse 中: .../tmp0/wtpwebapps/yingshop/
                    Path webappPath = Paths.get(webappRealPath).normalize();
                    log.info("Webapp 實際路徑: {}", webappPath);
                    
                    // 從 webapp 路徑向上查找專案根目錄
                    // 例如: .../target/yingshop/ -> .../target/ -> .../ (專案根)
                    // 或者: .../wtpwebapps/yingshop/ -> .../tmp0/wtpwebapps/ -> .../tmp0/ -> ...
                    Path current = webappPath.getParent();
                    int maxDepth = 15; // 最多向上查找15層
                    
                    while (current != null && maxDepth-- > 0) {
                        // 檢查是否存在 src/main/webapp 目錄
                        Path srcMainWebapp = current.resolve("src/main/webapp");
                        if (Files.exists(srcMainWebapp) && Files.isDirectory(srcMainWebapp)) {
                            projectPath = current.resolve(PROJECT_UPLOAD_DIR).normalize();
                            log.info("找到專案根目錄: {}", current);
                            break;
                        }
                        current = current.getParent();
                    }
                }
            } catch (Exception e) {
                log.warn("無法從 ServletContext 獲取專案路徑: {}", e.getMessage());
            }
        }
        
        // 如果找不到，嘗試使用已知的專案路徑
        if (projectPath == null || !Files.exists(projectPath.getParent())) {
            // 嘗試常見的專案路徑
            String[] possiblePaths = {
                "D:\\Ying\\workspace\\yingshop",
                System.getProperty("project.root"),
                System.getProperty("catalina.base") + "\\..\\..\\workspace\\yingshop"
            };
            
            for (String basePath : possiblePaths) {
                if (basePath != null && !basePath.isEmpty()) {
                    try {
                        Path testPath = Paths.get(basePath, PROJECT_UPLOAD_DIR).normalize();
                        Path parent = testPath.getParent();
                        if (parent != null && Files.exists(parent)) {
                            projectPath = testPath;
                            log.info("使用備用專案路徑: {}", basePath);
                            break;
                        }
                    } catch (Exception e) {
                        // 忽略無效路徑
                    }
                }
            }
        }
        
        // 如果還是找不到，記錄錯誤但繼續使用部署路徑
        if (projectPath == null || !Files.exists(projectPath.getParent())) {
            log.warn("無法確定專案根目錄，將僅使用部署路徑");
            // 使用部署路徑作為專案路徑（至少能保存檔案）
            if (servletContext != null) {
                String deployRealPath = servletContext.getRealPath(RESOURCE_PATH);
                if (deployRealPath != null && !deployRealPath.isBlank()) {
                    projectPath = Paths.get(deployRealPath).normalize();
                }
            }
        }
        
        log.info("最終使用的專案路徑: {}", projectPath != null ? projectPath.toAbsolutePath() : "null");
        
        try {
            if (!Files.exists(projectPath)) {
                Files.createDirectories(projectPath);
                log.info("✓ 已建立專案目錄: {}", projectPath.toAbsolutePath());
            } else {
                log.info("✓ 專案目錄已存在: {}", projectPath.toAbsolutePath());
            }
            
            Path projectFilePath = projectPath.resolve(filename);
            log.info("完整檔案路徑: {}", projectFilePath.toAbsolutePath());
            
            // 寫入檔案
            boolean writeSuccess = ImageIO.write(processedImage, formatName, projectFilePath.toFile());
            if (!writeSuccess) {
                log.error("✗ ImageIO.write 返回 false，寫入可能失敗");
            }
            
            // 強制同步到磁碟
            System.out.flush();
            System.err.flush();
            
            // 等待一小段時間確保檔案系統更新
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待過程中被中斷");
            }
            
            // 驗證檔案是否真的寫入成功
            if (Files.exists(projectFilePath)) {
                long fileSize = Files.size(projectFilePath);
                log.info("✓✓✓ 圖片已成功儲存到專案目錄: {}", projectFilePath.toAbsolutePath());
                log.info("  - 檔案大小: {} bytes", fileSize);
                log.info("  - 檔案可讀: {}", Files.isReadable(projectFilePath));
                System.out.println("【檔案上傳成功】路徑: " + projectFilePath.toAbsolutePath());
            } else {
                log.error("✗✗✗ 圖片儲存失敗！檔案不存在: {}", projectFilePath.toAbsolutePath());
                System.err.println("【檔案上傳失敗】路徑不存在: " + projectFilePath.toAbsolutePath());
                throw new IOException("檔案寫入失敗，檔案不存在: " + projectFilePath.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("✗✗✗ 儲存檔案時發生異常: {}", e.getMessage(), e);
            System.err.println("【檔案上傳異常】: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("儲存檔案時發生錯誤: " + e.getMessage(), e);
        }

        // 同時保存到部署路徑（如果存在且不同）
        log.info("========== 檢查部署路徑 ==========");
        if (servletContext != null) {
            try {
                String deployRealPath = servletContext.getRealPath(RESOURCE_PATH);
                log.info("ServletContext 部署路徑: {}", deployRealPath);
                
                if (deployRealPath != null && !deployRealPath.isBlank()) {
                    Path deployPath = Paths.get(deployRealPath).normalize();
                    Path absoluteProjectPath = projectPath.toAbsolutePath().normalize();
                    
                    log.info("部署路徑 (絕對): {}", deployPath.toAbsolutePath());
                    log.info("專案路徑 (絕對): {}", absoluteProjectPath);
                    
                    // 如果部署路徑和專案路徑不同，才保存到部署路徑
                    if (!deployPath.equals(absoluteProjectPath)) {
                        log.info("路徑不同，將同時儲存到部署路徑");
                        if (!Files.exists(deployPath)) {
                            Files.createDirectories(deployPath);
                            log.info("✓ 已建立部署目錄: {}", deployPath.toAbsolutePath());
                        }
                        Path deployFilePath = deployPath.resolve(filename);
                        ImageIO.write(processedImage, formatName, deployFilePath.toFile());
                        log.info("圖片已同時儲存到部署目錄: {}", deployFilePath.toAbsolutePath());
                        
                        // 驗證部署路徑的檔案
                        if (Files.exists(deployFilePath)) {
                            log.info("✓ 部署路徑檔案已確認存在: {}", deployFilePath.toAbsolutePath());
                        } else {
                            log.warn("⚠ 部署路徑檔案驗證失敗");
                        }
                    } else {
                        log.info("部署路徑與專案路徑相同，跳過重複儲存");
                    }
                } else {
                    log.warn("無法取得部署路徑（servletContext.getRealPath 返回 null）");
                }
            } catch (Exception e) {
                log.warn("無法儲存到部署目錄: {}", e.getMessage(), e);
                e.printStackTrace();
            }
        } else {
            log.warn("ServletContext 為 null，跳過部署路徑儲存");
        }

        log.info("========== 圖片上傳完成 ==========");
        String contextPath = (servletContext != null) ? servletContext.getContextPath() : "";
        String imageUrl = contextPath + RESOURCE_PATH + filename;
        log.info("返回的圖片 URL: {}", imageUrl);
        
        // 回傳含 contextPath 的可訪問 URL（例如 /yingshop/resources/...）
        return imageUrl;
    }

    @Override
    public List<String> uploadProductImages(List<MultipartFile> files, Long productId) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return imageUrls;
        }
        
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String imageUrl = uploadProductImage(file, productId);
                if (imageUrl != null) {
                    imageUrls.add(imageUrl);
                }
            }
        }
        
        return imageUrls;
    }

    @Override
    public String validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "請選擇圖片文件";
        }
        
        // 檢查檔案大小
        if (file.getSize() > MAX_FILE_SIZE) {
            return "圖片文件大小不能超過 5MB";
        }
        
        // 檢查內容類型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return "不支援的圖片格式，僅支援 JPG、PNG、GIF、WEBP";
        }
        
        // 檢查檔案擴展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return "檔案名稱無效";
        }
        
        String extension = getFileExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            return "不支援的檔案擴展名，僅支援 .jpg、.jpeg、.png、.gif、.webp";
        }
        
        // 嘗試讀取圖片以驗證是否為有效圖片
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                return "無法讀取圖片，請確認文件是否為有效的圖片格式";
            }
        } catch (IOException e) {
            return "無法讀取圖片文件：" + e.getMessage();
        }
        
        return null; // 驗證通過
    }

    @Override
    public void deleteProductImage(String imagePath) throws IOException {
        if (imagePath == null || imagePath.isEmpty()) {
            return;
        }

        // 如果路径是相对路径（/resources/images/products/...），转换为文件系统路径
        if (imagePath.startsWith(RESOURCE_PATH)) {
            String filename = imagePath.substring(RESOURCE_PATH.length());
            
            // 嘗試刪除部署路徑的檔案
            if (servletContext != null) {
                try {
                    String deployRealPath = servletContext.getRealPath(RESOURCE_PATH);
                    if (deployRealPath != null && !deployRealPath.isBlank()) {
                        Path deployPath = Paths.get(deployRealPath, filename);
                        if (Files.exists(deployPath)) {
                            Files.delete(deployPath);
                            log.info("已刪除部署路徑檔案: {}", deployPath);
                        }
                    }
                } catch (Exception e) {
                    log.debug("無法刪除部署路徑檔案: {}", e.getMessage());
                }
            }
            
            // 同時嘗試刪除專案路徑的檔案
            try {
                Path projectPath = Paths.get(System.getProperty("user.dir"), PROJECT_UPLOAD_DIR, filename).normalize();
                if (Files.exists(projectPath)) {
                    Files.delete(projectPath);
                    log.info("已刪除專案路徑檔案: {}", projectPath.toAbsolutePath());
                }
            } catch (Exception e) {
                log.debug("無法刪除專案路徑檔案: {}", e.getMessage());
            }
        } else if (imagePath.startsWith("/")) {
            // 处理绝对路径的情况
            String filename = imagePath.substring(imagePath.lastIndexOf("/") + 1);
            
            // 嘗試刪除部署路徑的檔案
            if (servletContext != null) {
                try {
                    String deployRealPath = servletContext.getRealPath(RESOURCE_PATH);
                    if (deployRealPath != null && !deployRealPath.isBlank()) {
                        Path deployPath = Paths.get(deployRealPath, filename);
                        if (Files.exists(deployPath)) {
                            Files.delete(deployPath);
                            log.info("已刪除部署路徑檔案: {}", deployPath);
                        }
                    }
                } catch (Exception e) {
                    log.debug("無法刪除部署路徑檔案: {}", e.getMessage());
                }
            }
            
            // 同時嘗試刪除專案路徑的檔案
            try {
                Path projectPath = Paths.get(System.getProperty("user.dir"), PROJECT_UPLOAD_DIR, filename).normalize();
                if (Files.exists(projectPath)) {
                    Files.delete(projectPath);
                    log.info("已刪除專案路徑檔案: {}", projectPath.toAbsolutePath());
                }
            } catch (Exception e) {
                log.debug("無法刪除專案路徑檔案: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 獲取檔案擴展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg"; // 預設為jpg
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * 調整圖片大小（壓縮和縮圖）
     */
    private BufferedImage resizeImage(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // 如果圖片已經小於最大尺寸，直接返回
        if (originalWidth <= THUMBNAIL_MAX_WIDTH && originalHeight <= THUMBNAIL_MAX_HEIGHT) {
            return originalImage;
        }
        
        // 計算縮放比例
        double widthRatio = (double) THUMBNAIL_MAX_WIDTH / originalWidth;
        double heightRatio = (double) THUMBNAIL_MAX_HEIGHT / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);
        
        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);
        
        // 創建縮放後的圖片
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        
        // 設置高品質渲染
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();
        
        return resizedImage;
    }
}

