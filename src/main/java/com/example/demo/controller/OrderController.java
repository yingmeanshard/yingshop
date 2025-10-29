package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.OrderService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
        return OrderStatus.values();
    }

    @GetMapping
    public String listOrders(@RequestParam(value = "userId", required = false) Long userId, Model model) {
        List<Order> orders;
        if (userId != null) {
            User user = userService.getUserById(userId);
            if (user == null) {
                model.addAttribute("errorMessage", "找不到指定的使用者。");
                orders = List.of();
            } else {
                orders = orderService.listOrdersByUser(user);
                model.addAttribute("selectedUserId", userId);
            }
        } else {
            orders = orderService.getAllOrders();
        }
        model.addAttribute("orders", orders);
        model.addAttribute("users", userService.getAllUsers());
        return "orders";
    }

    @PostMapping
    public String createOrder(@RequestParam("userId") Long userId,
                              @ModelAttribute("cart") Cart cart,
                              RedirectAttributes redirectAttributes) {
        if (cart == null || cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "購物車為空，無法建立訂單。");
            return "redirect:/cart";
        }
        User user = userService.getUserById(userId);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的使用者。");
            return "redirect:/cart";
        }
        Order order;
        try {
            order = orderService.createOrder(cart, user);
            cart.clear();
            redirectAttributes.addFlashAttribute("successMessage", "訂單建立成功。");
            return "redirect:/orders/" + order.getId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/cart";
        }
    }

    @GetMapping("/{id}")
    public String viewOrder(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Order order = orderService.getOrderById(id);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "找不到指定的訂單。");
            return "redirect:/orders";
        }
        model.addAttribute("order", order);
        return "order-detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") String statusValue,
                               RedirectAttributes redirectAttributes) {
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