package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "users";
    }
    
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("user", new User());
        return "add-user";
    }
    
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "edit-user"; // 對應 /WEB-INF/views/edit-user.html
    }
    
	@PostMapping("/update/{id}")
	public String updateUser(@PathVariable("id") Long id, @ModelAttribute("user") User user,
			@RequestParam(value = "password", required = false) String password,
			RedirectAttributes redirectAttributes) {
		try {
			userService.updateUser(id, user, password);
			redirectAttributes.addFlashAttribute("successMessage", "使用者已更新");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
        return "redirect:/users";
    }
    
	@PostMapping("/save")
	public String saveUser(@ModelAttribute User user, @RequestParam("password") String password,
			RedirectAttributes redirectAttributes) {
		try {
			userService.saveUser(user, password);
			redirectAttributes.addFlashAttribute("successMessage", "使用者已新增");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/users";
	}
    
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return "redirect:/users";
    }
}