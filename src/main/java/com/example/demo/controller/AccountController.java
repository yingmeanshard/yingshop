package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final UserService userService;
    private final UserDetailsService userDetailsService;

    @Autowired
    public AccountController(UserService userService, UserDetailsService userDetailsService) {
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("user") User form,
                                RedirectAttributes redirectAttributes) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        try {
            User updatedUser = userService.updateProfile(user.getId(), form.getName(), form.getEmail(), form.getPhoneNumber());
            refreshAuthentication(updatedUser);
            redirectAttributes.addFlashAttribute("successMessage", "個人資料已更新");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/account/profile";
    }

    private void refreshAuthentication(User updatedUser) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth == null || !currentAuth.isAuthenticated() ||
                currentAuth instanceof AnonymousAuthenticationToken) {
            return;
        }
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(updatedUser.getEmail());
            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    currentAuth.getCredentials(),
                    userDetails.getAuthorities()
            );
            newAuth.setDetails(currentAuth.getDetails());
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        } catch (UsernameNotFoundException ex) {
            SecurityContextHolder.clearContext();
        }
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.getUserByEmail(authentication.getName());
    }
}