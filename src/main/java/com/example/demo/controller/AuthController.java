package com.example.demo.controller;

import com.example.demo.model.PasswordResetToken;
import com.example.demo.model.User;
import com.example.demo.service.PasswordResetService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
@RequestMapping
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @Autowired
    public AuthController(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/users/request-reset")
    public String requestResetForm() {
        return "password-reset-request";
    }

    @PostMapping("/users/request-reset")
    public String requestReset(@RequestParam("email") String email,
                               RedirectAttributes redirectAttributes) {
        User user = userService.getUserByEmail(email);
        if (user != null) {
            PasswordResetToken token = passwordResetService.createToken(user);
            String resetUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/users/reset")
                    .toUriString();
            passwordResetService.sendResetInstructions(token, resetUrl);
        }
        redirectAttributes.addFlashAttribute("successMessage", "若 Email 存在，將寄送重設連結");
        return "redirect:/users/request-reset";
    }

    @GetMapping("/users/reset")
    public String resetForm(@RequestParam(value = "token", required = false) String token,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        PasswordResetToken resetToken = passwordResetService.validateToken(token);
        if (resetToken == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "重設連結無效或已過期");
            return "redirect:/users/request-reset";
        }
        model.addAttribute("token", token);
        return "password-reset";
    }

    @PostMapping("/users/reset")
    public String reset(@RequestParam("token") String token,
                        @RequestParam("password") String password,
                        RedirectAttributes redirectAttributes) {
        PasswordResetToken resetToken = passwordResetService.validateToken(token);
        if (resetToken == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "重設連結無效或已過期");
            return "redirect:/users/request-reset";
        }
        try {
            passwordResetService.resetPassword(resetToken, password);
            redirectAttributes.addFlashAttribute("successMessage", "密碼已更新，請重新登入");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addAttribute("token", token);
            return "redirect:/users/reset";
        }
    }
}