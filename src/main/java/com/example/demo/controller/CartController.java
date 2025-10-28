package com.example.demo.controller;

import com.example.demo.model.Cart;
import com.example.demo.model.Product;
import com.example.demo.service.CartService;
import com.example.demo.service.ProductService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
@SessionAttributes("cart")
public class CartController {

    private final ProductService productService;
    private final CartService cartService;

    @Autowired
    public CartController(ProductService productService, CartService cartService) {
        this.productService = productService;
        this.cartService = cartService;
    }

    @ModelAttribute("cart")
    public Cart setupCart() {
        return new Cart();
    }

    @ModelAttribute("cartItemCount")
    public int cartItemCount(@ModelAttribute("cart") Cart cart) {
        return cartService.getTotalQuantity(cart);
    }

    @PostMapping("/add/{productId}")
    public String addItem(@PathVariable Long productId,
                          @RequestParam(value = "quantity", required = false, defaultValue = "1") int quantity,
                          @RequestParam(value = "redirect", required = false) String redirect,
                          @ModelAttribute("cart") Cart cart,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的商品。");
            return "redirect:/products";
        }
        cartService.addItem(cart, product, quantity);
        redirectAttributes.addFlashAttribute("successMessage", "商品已加入購物車。");
        return "redirect:" + resolveRedirectTarget(redirect, request);
    }

    @PostMapping("/update/{productId}")
    public String updateItem(@PathVariable Long productId,
                             @RequestParam("quantity") int quantity,
                             @ModelAttribute("cart") Cart cart,
                             RedirectAttributes redirectAttributes) {
        cartService.updateItemQuantity(cart, productId, quantity);
        redirectAttributes.addFlashAttribute("successMessage", "購物車已更新。");
        return "redirect:/cart";
    }

    @PostMapping("/remove/{productId}")
    public String removeItem(@PathVariable Long productId,
                             @ModelAttribute("cart") Cart cart,
                             RedirectAttributes redirectAttributes) {
        cartService.removeItem(cart, productId);
        redirectAttributes.addFlashAttribute("successMessage", "商品已從購物車移除。");
        return "redirect:/cart";
    }

    @GetMapping
    public String showCart(@ModelAttribute("cart") Cart cart, Model model) {
        cartService.calculateTotalPrice(cart);
        model.addAttribute("cart", cart);
        model.addAttribute("items", cart.getItemList());
        return "cart";
    }

    private String resolveRedirectTarget(String redirect, HttpServletRequest request) {
        String defaultTarget = "/cart";
        if (redirect == null || redirect.isBlank()) {
            return defaultTarget;
        }

        String trimmed = redirect.trim();
        if (trimmed.contains("://") || trimmed.startsWith("//")) {
            return defaultTarget;
        }

        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank()) {
            if (trimmed.equals(contextPath)) {
                trimmed = "/";
            } else if (trimmed.startsWith(contextPath + "/")) {
                trimmed = trimmed.substring(contextPath.length());
            }
        }

        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }

        return trimmed;
    }
}