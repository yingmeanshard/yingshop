package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ProductService productService;

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
    public String createProduct(@ModelAttribute Product product, RedirectAttributes redirectAttributes, Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        productService.saveProduct(product);
        redirectAttributes.addFlashAttribute("successMessage", "商品已建立。");
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
        model.addAttribute("product", product);
        model.addAttribute("formTitle", "編輯商品");
        model.addAttribute("isEdit", true);
        model.addAttribute("categories", categories);
        return "admin-product-form";
    }

    @PostMapping("/products/edit/{id}")
    public String updateProduct(@PathVariable Long id, @ModelAttribute Product product, RedirectAttributes redirectAttributes, Authentication authentication) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/";
        }
        product.setId(id);
        productService.saveProduct(product);
        redirectAttributes.addFlashAttribute("successMessage", "商品已更新。");
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
}
