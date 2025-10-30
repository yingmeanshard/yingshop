package com.example.demo.controller;

import com.example.demo.model.PasswordResetToken;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.service.PasswordResetService;
import com.example.demo.service.UserService;
import com.example.demo.web.RegistrationForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @Autowired
    public AuthController(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/register")
    public String showRegister(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("registrationForm") RegistrationForm form,
                           RedirectAttributes redirectAttributes) {
        try {
            validateRegistration(form);
            userService.registerUser(form.getName(), form.getEmail(), form.getPassword(), UserRole.CUSTOMER);
            redirectAttributes.addFlashAttribute("registrationSuccess", "註冊成功，現在可以登入。");
            return "redirect:/";
        } catch (IllegalArgumentException ex) {
            RegistrationForm safeCopy = new RegistrationForm();
            safeCopy.setName(form.getName());
            safeCopy.setEmail(form.getEmail());
            redirectAttributes.addFlashAttribute("registrationForm", safeCopy);
            redirectAttributes.addFlashAttribute("registrationError", ex.getMessage());
            return "redirect:/register";
        }
    }

    private void validateRegistration(RegistrationForm form) {
        if (form.getName() == null || form.getName().isBlank()) {
            throw new IllegalArgumentException("請輸入姓名。");
        }
        if (form.getEmail() == null || form.getEmail().isBlank()) {
            throw new IllegalArgumentException("請輸入 Email。");
        }
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            throw new IllegalArgumentException("請輸入密碼。");
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("兩次輸入的密碼不一致。");
        }
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
            redirectAttributes.addFlashAttribute("registrationSuccess", "密碼已更新，請重新登入。");
            return "redirect:/";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addAttribute("token", token);
            return "redirect:/users/reset";
        }
    }
}
