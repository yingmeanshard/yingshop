package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.model.ProductImage;
import com.example.demo.service.FileUploadService;
import com.example.demo.service.ProductImageService;
import com.example.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private ProductImageService productImageService;

    @GetMapping
    public String listProducts(@RequestParam(value = "category", required = false) String category,
                               Model model,
                               Authentication authentication) {
        // 检查是否是管理员或员工
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
        
        // 管理员可以看到所有商品，普通用户只能看到已上架的商品
        List<Product> allProducts = isAdmin ? productService.getAllProducts() : productService.getListedProducts();
        
        List<String> categories = allProducts.stream()
                .map(Product::getCategory)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        List<Product> products;
        if (category == null || category.isBlank()) {
            products = allProducts;
        } else {
            products = isAdmin ? productService.getProductsByCategory(category) : productService.getListedProductsByCategory(category);
        }

        products.sort(Comparator.comparing(Product::getId, Comparator.nullsLast(Long::compareTo)));

        // 為每個商品載入圖片資訊（用於 getCoverImageUrl）
        for (Product product : products) {
            List<ProductImage> images = productImageService.getImagesByProductId(product.getId());
            product.setImages(images);
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category);
        return "products";
    }

    @GetMapping("/detail/{id}")
    public String showProductDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        Product product = productService.getProductById(id);
        if (product == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的商品。");
            return "redirect:/products";
        }
        
        // 检查是否是管理员
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
        
        // 如果不是管理员，且商品已下架，则不允许访问
        if (!isAdmin && (product.getListed() == null || !product.getListed())) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的商品。");
            return "redirect:/products";
        }
        
        // 載入商品圖片
        List<ProductImage> images = productImageService.getImagesByProductId(id);
        model.addAttribute("product", product);
        model.addAttribute("images", images);
        return "product-detail";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("formTitle", "新增商品");
        model.addAttribute("isEdit", false);
        return "product-form";
    }

    @PostMapping("/create")
    public String createProduct(@ModelAttribute Product product,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                                @RequestParam(value = "coverImageIndex", required = false) Integer coverImageIndex,
                                RedirectAttributes redirectAttributes) {
        System.out.println("========== 開始建立商品 ==========");
        System.out.println("Product ID: " + product.getId());
        System.out.println("imageFiles: " + (imageFiles != null ? imageFiles.size() + " files" : "null"));
        System.out.println("imageFile: " + (imageFile != null && !imageFile.isEmpty() ? imageFile.getOriginalFilename() : "null/empty"));
        
        try {
            // 先保存商品以获取ID
            productService.saveProduct(product);
            System.out.println("商品已保存，ID: " + product.getId());
            
            // 處理多圖片上傳（優先）
            if (imageFiles != null && !imageFiles.isEmpty()) {
                System.out.println("處理多圖片上傳，共 " + imageFiles.size() + " 個檔案");
                // 過濾掉空檔案
                List<MultipartFile> validFiles = imageFiles.stream()
                    .filter(file -> file != null && !file.isEmpty())
                    .collect(Collectors.toList());
                
                System.out.println("有效檔案數量: " + validFiles.size());
                
                if (!validFiles.isEmpty()) {
                    List<String> imageUrls = fileUploadService.uploadProductImages(validFiles, product.getId());
                    System.out.println("上傳完成，獲得 " + imageUrls.size() + " 個圖片 URL");
                    
                    int order = 0;
                    for (String imageUrl : imageUrls) {
                        ProductImage productImage = new ProductImage();
                        productImage.setProduct(product);
                        productImage.setImageUrl(imageUrl);
                        productImage.setDisplayOrder(order++);
                        productImage.setIsCover(order == 1 && (coverImageIndex == null || coverImageIndex == 0));
                        productImageService.saveProductImage(productImage);
                    }
                    // 設置第一張圖片為封面（如果沒有指定）
                    if (!imageUrls.isEmpty() && (coverImageIndex == null || coverImageIndex == 0)) {
                        product.setImageUrl(imageUrls.get(0));
                        productService.saveProduct(product);
                        System.out.println("設置封面圖片: " + imageUrls.get(0));
                    }
                } else {
                    System.out.println("警告：沒有有效的圖片檔案");
                }
            } else if (imageFile != null && !imageFile.isEmpty()) {
                System.out.println("處理單圖片上傳: " + imageFile.getOriginalFilename());
                // 單圖片上傳（向後兼容）
                String imagePath = fileUploadService.uploadProductImage(imageFile, product.getId());
                if (imagePath != null) {
                    System.out.println("單圖片上傳成功: " + imagePath);
                    product.setImageUrl(imagePath);
                    productService.saveProduct(product);
                    
                    // 同時創建ProductImage記錄
                    ProductImage productImage = new ProductImage();
                    productImage.setProduct(product);
                    productImage.setImageUrl(imagePath);
                    productImage.setIsCover(true);
                    productImage.setDisplayOrder(0);
                    productImageService.saveProductImage(productImage);
                } else {
                    System.out.println("警告：單圖片上傳返回 null");
                }
            } else {
                System.out.println("警告：沒有選擇任何圖片檔案");
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "商品已建立。");
            System.out.println("========== 商品建立完成 ==========");
        } catch (Exception e) {
            System.err.println("✗✗✗ 建立商品時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "建立商品時發生錯誤：" + e.getMessage());
        }
        
        return "redirect:/products";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Product product = productService.getProductById(id);
        if (product == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的商品。");
            return "redirect:/products";
        }
        
        // 載入商品圖片
        List<ProductImage> images = productImageService.getImagesByProductId(id);
        model.addAttribute("product", product);
        model.addAttribute("images", images);
        model.addAttribute("formTitle", "編輯商品");
        model.addAttribute("isEdit", true);
        return "product-form";
    }

    @PostMapping("/edit/{id}")
    public String updateProduct(@PathVariable Long id,
                                @ModelAttribute Product product,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                                @RequestParam(value = "coverImageIndex", required = false) Integer coverImageIndex,
                                @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
                                RedirectAttributes redirectAttributes) {
        System.out.println("========== 開始編輯商品 ==========");
        System.out.println("Product ID: " + id);
        System.out.println("imageFiles: " + (imageFiles != null ? imageFiles.size() + " files" : "null"));
        System.out.println("imageFile: " + (imageFile != null && !imageFile.isEmpty() ? imageFile.getOriginalFilename() : "null/empty"));
        
        try {
            Product existingProduct = productService.getProductById(id);
            if (existingProduct == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的商品。");
                return "redirect:/products";
            }
            
            product.setId(id);
            
            // 刪除指定的圖片
            if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
                System.out.println("刪除圖片 IDs: " + deleteImageIds);
                for (Long imageId : deleteImageIds) {
                    ProductImage img = productImageService.getProductImageById(imageId);
                    if (img != null) {
                        try {
                            fileUploadService.deleteProductImage(img.getImageUrl());
                        } catch (Exception e) {
                            // 忽略刪除錯誤
                        }
                        productImageService.deleteProductImage(imageId);
                    }
                }
            }
            
            // 處理多圖片上傳（優先）
            if (imageFiles != null && !imageFiles.isEmpty()) {
                System.out.println("處理多圖片上傳，共 " + imageFiles.size() + " 個檔案");
                // 過濾掉空檔案
                List<MultipartFile> validFiles = imageFiles.stream()
                    .filter(file -> file != null && !file.isEmpty())
                    .collect(Collectors.toList());
                
                System.out.println("有效檔案數量: " + validFiles.size());
                
                if (!validFiles.isEmpty()) {
                    List<String> imageUrls = fileUploadService.uploadProductImages(validFiles, id);
                    System.out.println("上傳完成，獲得 " + imageUrls.size() + " 個圖片 URL");
                List<ProductImage> existingImages = productImageService.getImagesByProductId(id);
                int maxOrder = existingImages.stream()
                        .mapToInt(img -> img.getDisplayOrder() != null ? img.getDisplayOrder() : 0)
                        .max()
                        .orElse(-1);
                
                int order = maxOrder + 1;
                for (String imageUrl : imageUrls) {
                    ProductImage productImage = new ProductImage();
                    productImage.setProduct(existingProduct);
                    productImage.setImageUrl(imageUrl);
                    productImage.setDisplayOrder(order++);
                    productImage.setIsCover(false);
                    productImageService.saveProductImage(productImage);
                }
                
                // 設置封面圖片
                if (coverImageIndex != null && coverImageIndex >= 0) {
                    List<ProductImage> allImages = productImageService.getImagesByProductId(id);
                    for (int i = 0; i < allImages.size(); i++) {
                        ProductImage img = allImages.get(i);
                        img.setIsCover(i == coverImageIndex);
                        productImageService.saveProductImage(img);
                    }
                    if (coverImageIndex < allImages.size()) {
                        product.setImageUrl(allImages.get(coverImageIndex).getImageUrl());
                    }
                } else {
                    // 如果沒有指定封面，使用第一張圖片
                    List<ProductImage> allImages = productImageService.getImagesByProductId(id);
                    if (!allImages.isEmpty()) {
                        ProductImage firstImage = allImages.get(0);
                        firstImage.setIsCover(true);
                        productImageService.saveProductImage(firstImage);
                        product.setImageUrl(firstImage.getImageUrl());
                    }
                }
                } else {
                    System.out.println("警告：沒有有效的圖片檔案");
                }
            } else if (imageFile != null && !imageFile.isEmpty()) {
                System.out.println("處理單圖片上傳: " + imageFile.getOriginalFilename());
                // 單圖片上傳（向後兼容）
                // 刪除舊圖片（如果存在）
                if (existingProduct.getImageUrl() != null && existingProduct.getImageUrl().startsWith("/resources/images/products/")) {
                    try {
                        fileUploadService.deleteProductImage(existingProduct.getImageUrl());
                    } catch (Exception e) {
                        // 忽略删除旧图片的错误
                    }
                }
                
                // 上传新图片
                String imagePath = fileUploadService.uploadProductImage(imageFile, id);
                if (imagePath != null) {
                    System.out.println("單圖片上傳成功: " + imagePath);
                    product.setImageUrl(imagePath);
                    
                    // 更新或創建ProductImage記錄
                    List<ProductImage> existingImages = productImageService.getImagesByProductId(id);
                    if (!existingImages.isEmpty()) {
                        ProductImage firstImage = existingImages.get(0);
                        firstImage.setImageUrl(imagePath);
                        firstImage.setIsCover(true);
                        productImageService.saveProductImage(firstImage);
                    } else {
                        ProductImage productImage = new ProductImage();
                        productImage.setProduct(existingProduct);
                        productImage.setImageUrl(imagePath);
                        productImage.setIsCover(true);
                        productImage.setDisplayOrder(0);
                        productImageService.saveProductImage(productImage);
                    }
                } else {
                    System.out.println("警告：單圖片上傳返回 null");
                }
            } else {
                System.out.println("警告：沒有選擇任何圖片檔案");
                // 如果没有上传新图片，保留原有图片URL
                product.setImageUrl(existingProduct.getImageUrl());
            }
            
            productService.saveProduct(product);
            redirectAttributes.addFlashAttribute("successMessage", "商品已更新。");
            System.out.println("========== 商品更新完成 ==========");
        } catch (Exception e) {
            System.err.println("✗✗✗ 更新商品時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "更新商品時發生錯誤：" + e.getMessage());
        }
        
        return "redirect:/products";
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.deleteProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "商品已刪除。");
        return "redirect:/products";
    }
}