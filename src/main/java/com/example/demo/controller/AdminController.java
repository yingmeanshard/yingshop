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
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ProductService productService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private ProductImageService productImageService;

    @GetMapping
    public String adminDashboard(Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        return "admin-dashboard";
    }

    @GetMapping("/products")
    public String adminProducts(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortOrder", defaultValue = "asc") String sortOrder,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            Model model,
            Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"))) {
            return "redirect:/";
        }
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);

        List<Product> allProducts = productService.getAllProducts();
        List<String> categories = allProducts.stream()
                .map(Product::getCategory)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        // 計算每個分類的商品數量
        java.util.Map<String, Long> categoryCounts = allProducts.stream()
                .filter(p -> p.getCategory() != null && !p.getCategory().isBlank())
                .collect(Collectors.groupingBy(Product::getCategory, Collectors.counting()));

        List<Product> products;
        if (category == null || category.isBlank()) {
            products = allProducts;
        } else {
            products = productService.getProductsByCategory(category);
        }

        // 搜尋功能
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            products = products.stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(searchLower)) ||
                            (p.getDescription() != null && p.getDescription().toLowerCase().contains(searchLower)) ||
                            (p.getCategory() != null && p.getCategory().toLowerCase().contains(searchLower)))
                    .collect(Collectors.toList());
        }

        // 排序功能
        Comparator<Product> comparator;
        switch (sortBy.toLowerCase()) {
            case "name":
                comparator = Comparator.comparing(p -> p.getName() != null ? p.getName() : "", String.CASE_INSENSITIVE_ORDER);
                break;
            case "price":
                comparator = Comparator.comparing(Product::getPrice, Comparator.nullsLast(java.math.BigDecimal::compareTo));
                break;
            case "stock":
                comparator = Comparator.comparing(p -> p.getStock() != null ? p.getStock() : 0, Comparator.nullsLast(Integer::compareTo));
                break;
            case "category":
                comparator = Comparator.comparing(p -> p.getCategory() != null ? p.getCategory() : "", String.CASE_INSENSITIVE_ORDER);
                break;
            default:
                comparator = Comparator.comparing(Product::getId, Comparator.nullsLast(Long::compareTo));
        }
        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }
        products.sort(comparator);

        // 分頁功能
        int totalProducts = products.size();
        int totalPages = (int) Math.ceil((double) totalProducts / pageSize);
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalProducts);
        List<Product> paginatedProducts = products.subList(startIndex, endIndex);

        model.addAttribute("products", paginatedProducts);
        model.addAttribute("allProducts", allProducts);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryCounts", categoryCounts);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("search", search);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortOrder", sortOrder);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalProducts", totalProducts);
        return "admin-products";
    }

    @GetMapping("/products/create")
    public String showCreateForm(Model model, Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        List<Product> allProducts = productService.getAllProducts();
        List<String> categories = allProducts.stream()
                .map(Product::getCategory)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        model.addAttribute("product", new Product());
        model.addAttribute("formTitle", "新增商品");
        model.addAttribute("isEdit", false);
        model.addAttribute("categories", categories);
        return "admin-product-form";
    }

    @PostMapping("/products/create")
    public String createProduct(@ModelAttribute Product product,
                                 @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                 @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                                 @RequestParam(value = "coverImageIndex", required = false) Integer coverImageIndex,
                                 RedirectAttributes redirectAttributes,
                                 Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        
        try {
            // 先保存商品以获取ID
            productService.saveProduct(product);
            
            // 處理多圖片上傳（優先）
            if (imageFiles != null && !imageFiles.isEmpty()) {
                List<String> imageUrls = fileUploadService.uploadProductImages(imageFiles, product.getId());
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
                }
            } else if (imageFile != null && !imageFile.isEmpty()) {
                // 單圖片上傳（向後兼容）
                String imagePath = fileUploadService.uploadProductImage(imageFile, product.getId());
                if (imagePath != null) {
                    product.setImageUrl(imagePath);
                    productService.saveProduct(product);
                    
                    // 同時創建ProductImage記錄
                    ProductImage productImage = new ProductImage();
                    productImage.setProduct(product);
                    productImage.setImageUrl(imagePath);
                    productImage.setIsCover(true);
                    productImage.setDisplayOrder(0);
                    productImageService.saveProductImage(productImage);
                }
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "商品已建立。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "建立商品時發生錯誤：" + e.getMessage());
        }
        
        return "redirect:/admin/products";
    }

    @GetMapping("/products/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        Product product = productService.getProductById(id);
        if (product == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的商品。");
            return "redirect:/admin/products";
        }
        List<Product> allProducts = productService.getAllProducts();
        List<String> categories = allProducts.stream()
                .map(Product::getCategory)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        
        // 載入商品圖片
        List<ProductImage> images = productImageService.getImagesByProductId(id);
        model.addAttribute("product", product);
        model.addAttribute("images", images);
        model.addAttribute("formTitle", "編輯商品");
        model.addAttribute("isEdit", true);
        model.addAttribute("categories", categories);
        return "admin-product-form";
    }

    @PostMapping("/products/edit/{id}")
    public String updateProduct(@PathVariable Long id,
                                @ModelAttribute Product product,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                                @RequestParam(value = "coverImageIndex", required = false) Integer coverImageIndex,
                                @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
                                RedirectAttributes redirectAttributes,
                                Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        
        try {
            Product existingProduct = productService.getProductById(id);
            if (existingProduct == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的商品。");
                return "redirect:/admin/products";
            }
            
            product.setId(id);
            
            // 刪除指定的圖片
            if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
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
                List<String> imageUrls = fileUploadService.uploadProductImages(imageFiles, id);
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
            } else if (imageFile != null && !imageFile.isEmpty()) {
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
                }
            } else {
                // 如果没有上传新图片，保留原有图片URL
                product.setImageUrl(existingProduct.getImageUrl());
            }
            
            productService.saveProduct(product);
            redirectAttributes.addFlashAttribute("successMessage", "商品已更新。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "更新商品時發生錯誤：" + e.getMessage());
        }
        
        return "redirect:/admin/products";
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        productService.deleteProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "商品已刪除。");
        return "redirect:/admin/products";
    }

    @PostMapping("/categories/update")
    public String updateCategory(
            @RequestParam("oldCategory") String oldCategory,
            @RequestParam("newCategory") String newCategory,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        if (oldCategory == null || oldCategory.isBlank() || newCategory == null || newCategory.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "分類名稱不能為空。");
            return "redirect:/admin/products";
        }
        List<Product> products = productService.getProductsByCategory(oldCategory);
        for (Product product : products) {
            product.setCategory(newCategory);
            productService.saveProduct(product);
        }
        redirectAttributes.addFlashAttribute("successMessage", "分類「" + oldCategory + "」已更新為「" + newCategory + "」，共 " + products.size() + " 個商品。");
        return "redirect:/admin/products";
    }

    @PostMapping("/categories/delete")
    public String deleteCategory(
            @RequestParam("category") String category,
            @RequestParam(value = "moveToCategory", required = false) String moveToCategory,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        if (category == null || category.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "分類名稱不能為空。");
            return "redirect:/admin/products";
        }
        List<Product> products = productService.getProductsByCategory(category);
        if (moveToCategory != null && !moveToCategory.isBlank()) {
            for (Product product : products) {
                product.setCategory(moveToCategory);
                productService.saveProduct(product);
            }
            redirectAttributes.addFlashAttribute("successMessage", "分類「" + category + "」已刪除，共 " + products.size() + " 個商品已移至「" + moveToCategory + "」。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "刪除分類前，請先將該分類下的商品移至其他分類。");
            return "redirect:/admin/products";
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/staff")
    public String staffDashboard(Model model, Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);
        return "admin-staff-dashboard";
    }

    @GetMapping("/products/stock")
    public String stockManagement(Model model, Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);
        return "admin-stock-management";
    }

    @PostMapping("/products/stock/batch")
    public String batchUpdateStock(
            @RequestParam("productIds") List<Long> productIds,
            @RequestParam("stocks") List<Integer> stocks,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        if (productIds == null || stocks == null || productIds.size() != stocks.size()) {
            redirectAttributes.addFlashAttribute("errorMessage", "更新失敗：資料不完整。");
            return "redirect:/admin/products/stock";
        }
        int updatedCount = 0;
        for (int i = 0; i < productIds.size(); i++) {
            Product product = productService.getProductById(productIds.get(i));
            if (product != null) {
                product.setStock(stocks.get(i));
                productService.saveProduct(product);
                updatedCount++;
            }
        }
        redirectAttributes.addFlashAttribute("successMessage", "已成功更新 " + updatedCount + " 個商品的庫存。");
        return "redirect:/admin/products/stock";
    }

    @PostMapping("/products/{id}/stock")
    public String updateProductStock(
            @PathVariable Long id,
            @RequestParam("stock") Integer stock,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        Product product = productService.getProductById(id);
        if (product == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的商品。");
            return "redirect:/admin/products/stock";
        }
        product.setStock(stock);
        productService.saveProduct(product);
        redirectAttributes.addFlashAttribute("successMessage", "商品「" + product.getName() + "」的庫存已更新為 " + stock + "。");
        return "redirect:/admin/products/stock";
    }

    @PostMapping("/products/{id}/toggle-listed")
    public String toggleListed(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        Product product = productService.getProductById(id);
        if (product == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的商品。");
            return "redirect:/admin/products";
        }
        product.setListed(!product.getListed());
        productService.saveProduct(product);
        redirectAttributes.addFlashAttribute("successMessage", "商品「" + product.getName() + "」已" + (product.getListed() ? "上架" : "下架") + "。");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/stock/batch-list")
    public String batchUpdateStockList(
            @RequestParam("productIds") List<Long> productIds,
            @RequestParam("stocks") List<Integer> stocks,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"))) {
            return "redirect:/";
        }
        if (productIds == null || stocks == null || productIds.size() != stocks.size()) {
            redirectAttributes.addFlashAttribute("errorMessage", "更新失敗：資料不完整。");
            return "redirect:/admin/products";
        }
        int updatedCount = 0;
        for (int i = 0; i < productIds.size(); i++) {
            Product product = productService.getProductById(productIds.get(i));
            if (product != null) {
                product.setStock(stocks.get(i));
                productService.saveProduct(product);
                updatedCount++;
            }
        }
        redirectAttributes.addFlashAttribute("successMessage", "已成功更新 " + updatedCount + " 個商品的庫存。");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/batch-update")
    public String batchUpdateProducts(
            @RequestParam("productIds") List<Long> productIds,
            @RequestParam(value = "names", required = false) List<String> names,
            @RequestParam(value = "categories", required = false) List<String> categories,
            @RequestParam(value = "prices", required = false) List<java.math.BigDecimal> prices,
            @RequestParam(value = "stocks", required = false) List<Integer> stocksAll,
            @RequestParam(value = "listeds", required = false) List<Boolean> listeds,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        if (productIds == null || productIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "更新失敗：沒有收到商品資料。");
            return "redirect:/admin/products";
        }

        int updatedCount = 0;
        for (int i = 0; i < productIds.size(); i++) {
            Long id = productIds.get(i);
            Product product = productService.getProductById(id);
            if (product == null) {
                continue;
            }
            if (names != null && i < names.size() && names.get(i) != null) {
                product.setName(names.get(i).trim());
            }
            if (categories != null && i < categories.size()) {
                String cat = categories.get(i);
                product.setCategory(cat == null ? null : cat.trim());
            }
            if (prices != null && i < prices.size() && prices.get(i) != null) {
                product.setPrice(prices.get(i));
            }
            if (stocksAll != null && i < stocksAll.size() && stocksAll.get(i) != null) {
                product.setStock(stocksAll.get(i));
            }
            if (listeds != null && i < listeds.size() && listeds.get(i) != null) {
                product.setListed(listeds.get(i));
            }
            productService.saveProduct(product);
            updatedCount++;
        }
        redirectAttributes.addFlashAttribute("successMessage", "已成功更新 " + updatedCount + " 個商品。");
        return "redirect:/admin/products";
    }
}
