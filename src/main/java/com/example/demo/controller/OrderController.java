package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.OrderService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
@SessionAttributes("cart")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    @Autowired
    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @ModelAttribute("cart")
    public Cart initializeCart() {
        return new Cart();
    }

    @ModelAttribute("orderStatuses")
    public OrderStatus[] orderStatuses() {
        // 僅提供需求指定的狀態於前端選單（隱藏舊的 PAID）
        return new OrderStatus[] {
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PENDING_CONFIRMATION,
                OrderStatus.PENDING_SHIPMENT,
                OrderStatus.PROCESSING,
                OrderStatus.SHIPPED
        };
    }

    @ModelAttribute("paymentMethods")
    public PaymentMethod[] paymentMethods() {
        return PaymentMethod.values();
    }
    @GetMapping
    public String listOrders(@RequestParam(value = "userId", required = false) String userIdParam,
                             Model model,
                             Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isStaff = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isAdminOrStaff", isAdmin || isStaff);

        if (!isAdmin && !isStaff) {
            return "redirect:/orders/mine";
        }

        List<Order> orders;
        String trimmed = userIdParam == null ? null : userIdParam.trim();
        if (trimmed != null && !trimmed.isEmpty()) {
            try {
                Long userId = Long.parseLong(trimmed);
                User user = userService.getUserById(userId);
                if (user == null) {
                    model.addAttribute("errorMessage", "找不到指定的使用者。");
                    orders = List.of();
                } else {
                    orders = orderService.listOrdersByUser(user);
                    model.addAttribute("selectedUserId", userId);
                }
            } catch (NumberFormatException ex) {
                // 無效的 userId，視為未指定 → 顯示全部
                orders = orderService.getAllOrders();
                model.addAttribute("selectedUserId", null);
            }
        } else {
            orders = orderService.getAllOrders();
            model.addAttribute("selectedUserId", null);
        }
        model.addAttribute("orders", orders);
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("isAdminView", true);
        return "orders";
    }

    @GetMapping("/mine")
    public String myOrders(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/";
        }
        User user = userService.getUserByEmail(authentication.getName());
        if (user == null) {
            return "redirect:/";
        }
        model.addAttribute("orders", orderService.listOrdersByUser(user));
        model.addAttribute("showOwner", true);
        model.addAttribute("isAdmin", false);
        model.addAttribute("users", List.of(user));
        model.addAttribute("selectedUserId", user.getId());
        model.addAttribute("isAdminView", false);
        return "orders";
    }

    @PostMapping
    public String createOrder(@RequestParam("userId") Long userId,
                              @RequestParam("paymentMethod") String paymentMethodValue,
                              @ModelAttribute("cart") Cart cart,
                              RedirectAttributes redirectAttributes) {
        if (cart == null || cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "購物車為空，無法建立訂單。");
            return "redirect:/cart";
        }
        if (!cart.hasSelectedItems()) {
            redirectAttributes.addFlashAttribute("errorMessage", "請至少選擇一個商品進行結帳。");
            return "redirect:/cart";
        }
        User user = userService.getUserById(userId);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的使用者。");
            return "redirect:/cart";
        }
        PaymentMethod paymentMethod;
        Order order;
        try {
            paymentMethod = PaymentMethod.valueOf(paymentMethodValue);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "請選擇有效的付款方式。");
            return "redirect:/cart";
        }
        try {
            order = orderService.createOrder(cart, user, paymentMethod);
            // 只移除選中的商品，而不是清空整個購物車
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
            return "redirect:/cart";
        }
    }

    @GetMapping("/{id}")
    public String viewOrder(@PathVariable("id") Long id,
                            Authentication authentication,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        Order order = orderService.getOrderById(id);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的訂單。");
            return "redirect:/orders";
        }
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isStaff = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
        User currentUser = null;
        if (authentication != null && authentication.isAuthenticated()) {
            currentUser = userService.getUserByEmail(authentication.getName());
        }
        boolean isOwner = currentUser != null && order.getUser() != null
                && Objects.equals(order.getUser().getId(), currentUser.getId());
        if (!isAdmin && !isStaff) {
            if (currentUser == null) {
                return "redirect:/";
            }
            if (!isOwner) {
                return "order-error";
            }
        }
        model.addAttribute("order", order);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("isAdminView", isAdmin || isStaff);
        return "order-detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") String statusValue,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isStaff = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
        if (!isAdmin && !isStaff) {
            redirectAttributes.addFlashAttribute("errorMessage", "只有管理者和員工可以更新訂單狀態。");
            return "redirect:/orders/" + id;
        }
        try {
            OrderStatus status = OrderStatus.valueOf(statusValue);
            Order updatedOrder = orderService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "訂單狀態已更新為 " + updatedOrder.getStatus().getDisplayName() + "。");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
