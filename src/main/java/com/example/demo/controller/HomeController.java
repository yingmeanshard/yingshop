package com.example.demo.controller;

import com.example.demo.model.User;
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

    @Autowired
    public HomeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
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
        }
        model.addAttribute("loginError", error != null);
        return "home";
    }
}
