package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/checkout")
@SessionAttributes("cart")
public class CheckoutController {

    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final MessageSource messageSource;

    @Autowired
    public CheckoutController(OrderService orderService, UserService userService, ProductService productService, MessageSource messageSource) {
        this.orderService = orderService;
        this.userService = userService;
        this.productService = productService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String checkout(@ModelAttribute("cart") Cart cart,
                           Authentication authentication,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("errorMessage", "請先登入。");
            return "redirect:/cart";
        }

        if (cart == null || cart.isEmpty() || !cart.hasSelectedItems()) {
            redirectAttributes.addFlashAttribute("errorMessage", "請至少選擇一個商品進行結帳。");
            return "redirect:/cart";
        }

        User user = userService.getUserByEmail(authentication.getName());
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到使用者資訊。");
            return "redirect:/cart";
        }

        // 準備選中商品的詳細信息（包含產品圖片）
        List<Map<String, Object>> selectedItemsWithDetails = new ArrayList<>();
        for (CartItem cartItem : cart.getSelectedItems()) {
            Product product = productService.getProductById(cartItem.getProductId());
            if (product != null) {
                Map<String, Object> itemDetail = new HashMap<>();
                itemDetail.put("cartItem", cartItem);
                itemDetail.put("product", product);
                selectedItemsWithDetails.add(itemDetail);
            }
        }

        model.addAttribute("selectedItems", selectedItemsWithDetails);
        model.addAttribute("selectedTotalPrice", cart.getSelectedTotalPrice());
        model.addAttribute("user", user);
        model.addAttribute("deliveryPaymentMethods", DeliveryPaymentMethod.values());

        return "checkout";
    }

    @PostMapping
    public String createOrder(@RequestParam("userId") Long userId,
                              @RequestParam("recipientName") String recipientName,
                              @RequestParam("recipientPhone") String recipientPhone,
                              @RequestParam("recipientEmail") String recipientEmail,
                              @RequestParam("recipientAddress") String recipientAddress,
                              @RequestParam("deliveryPaymentMethod") String deliveryPaymentMethodValue,
                              @ModelAttribute("cart") Cart cart,
                              RedirectAttributes redirectAttributes) {
        if (cart == null || cart.isEmpty() || !cart.hasSelectedItems()) {
            redirectAttributes.addFlashAttribute("errorMessage", "購物車為空，無法建立訂單。");
            return "redirect:/cart";
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的使用者。");
            return "redirect:/cart";
        }

        DeliveryPaymentMethod deliveryPaymentMethod;
        try {
            deliveryPaymentMethod = DeliveryPaymentMethod.valueOf(deliveryPaymentMethodValue);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "請選擇有效的配送付款方式。");
            return "redirect:/checkout";
        }

        // 驗證：如果選擇貨到付款，地址不能是"自取"
        if (deliveryPaymentMethod == DeliveryPaymentMethod.CASH_ON_DELIVERY) {
            if (recipientAddress != null && (recipientAddress.trim().equals("自取") || recipientAddress.trim().equalsIgnoreCase("pickup"))) {
                String errorMsg = messageSource.getMessage("checkout.error.address.pickup.not.allowed", null, LocaleContextHolder.getLocale());
                redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
                return "redirect:/checkout";
            }
        }

        try {
            Order order = orderService.createOrder(cart, user, deliveryPaymentMethod,
                    recipientName, recipientPhone, recipientEmail, recipientAddress);
            
            // 只移除選中的商品
            List<Long> selectedProductIds = cart.getSelectedItems().stream()
                    .map(CartItem::getProductId)
                    .collect(Collectors.toList());
            for (Long productId : selectedProductIds) {
                cart.getItems().remove(productId);
            }
            cart.recalculateTotalPrice();
            
            redirectAttributes.addFlashAttribute("successMessage", "訂單建立成功。");
            return "redirect:/orders/" + order.getId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/checkout";
        }
    }
}

