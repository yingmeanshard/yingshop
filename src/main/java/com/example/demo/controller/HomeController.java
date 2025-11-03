package com.example.demo.controller;

import com.example.demo.model.Order;
import com.example.demo.model.User;
import com.example.demo.service.OrderService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final UserService userService;
    private final OrderService orderService;

    @Autowired
    public HomeController(UserService userService, OrderService orderService) {
        this.userService = userService;
        this.orderService = orderService;
    }

    @GetMapping({"/", ""})
    public String home(Authentication authentication,
                       @RequestParam(value = "error", required = false) String error,
                       Model model) {
        boolean isAuthenticated = authentication != null &&
                authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken);
        model.addAttribute("isAuthenticated", isAuthenticated);
        if (isAuthenticated) {
            User current = userService.getUserByEmail(authentication.getName());
            model.addAttribute("currentUser", current);
            if (current != null) {
                model.addAttribute("recentOrders", orderService.listOrdersByUser(current));
            }
        }
        model.addAttribute("loginError", error != null);
        return "home";
    }

    @GetMapping("/yingshop")
    public String homeAlias() {
        return "redirect:/";
    }
}
