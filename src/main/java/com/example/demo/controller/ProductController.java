package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public String listProducts(@RequestParam(value = "category", required = false) String category, Model model) {
        List<Product> allProducts = productService.getAllProducts();
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
            products = productService.getProductsByCategory(category);
        }

        products.sort(Comparator.comparing(Product::getId, Comparator.nullsLast(Long::compareTo)));

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category);
        return "products";
    }

    @GetMapping("/detail/{id}")
    public String showProductDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Product product = productService.getProductById(id);
        if (product == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的商品。");
            return "redirect:/products";
        }
        model.addAttribute("product", product);
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
    public String createProduct(@ModelAttribute Product product, RedirectAttributes redirectAttributes) {
        productService.saveProduct(product);
        redirectAttributes.addFlashAttribute("successMessage", "商品已建立。");
        return "redirect:/products";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Product product = productService.getProductById(id);
        if (product == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的商品。");
            return "redirect:/products";
        }
        model.addAttribute("product", product);
        model.addAttribute("formTitle", "編輯商品");
        model.addAttribute("isEdit", true);
        return "product-form";
    }

    @PostMapping("/edit/{id}")
    public String updateProduct(@PathVariable Long id, @ModelAttribute Product product, RedirectAttributes redirectAttributes) {
        product.setId(id);
        productService.saveProduct(product);
        redirectAttributes.addFlashAttribute("successMessage", "商品已更新。");
        return "redirect:/products";
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.deleteProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "商品已刪除。");
        return "redirect:/products";
    }
}