package com.example.demo.controller;

import com.example.demo.model.Address;
import com.example.demo.model.User;
import com.example.demo.service.AddressService;
import com.example.demo.service.UserService;
import java.util.List;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final UserService userService;
    private final UserDetailsService userDetailsService;
    private final AddressService addressService;

    @Autowired
    public AccountController(UserService userService,
            UserDetailsService userDetailsService,
            AddressService addressService) {
        this.userService = userService;
        this.userDetailsService = userDetailsService;
        this.addressService = addressService;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        List<Address> addresses = addressService.findByUser(user.getId());
        model.addAttribute("addresses", addresses);
        model.addAttribute("defaultAddressId", user.getDefaultAddressId());
        model.addAttribute("newAddress", new Address());
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

    @PostMapping("/addresses")
    public String createAddress(@ModelAttribute("newAddress") Address form,
                                @RequestParam(value = "setDefault", required = false, defaultValue = "false") boolean setDefault,
                                RedirectAttributes redirectAttributes) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        form.setUser(user);
        Address saved = addressService.save(form);
        if (setDefault && saved.getId() != null) {
            addressService.markDefault(user.getId(), saved.getId());
        }
        redirectAttributes.addFlashAttribute("successMessageAddress", "地址已新增");
        return "redirect:/account/profile#addressesSection";
    }

    @PostMapping("/addresses/{id}/delete")
    public String deleteAddress(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Address address = ensureOwnedAddress(id, redirectAttributes);
        if (address != null) {
            addressService.delete(id);
            redirectAttributes.addFlashAttribute("successMessageAddress", "地址已刪除");
        }
        return "redirect:/account/profile#addressesSection";
    }

    @PostMapping("/addresses/{id}/default")
    public String markDefaultAddress(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Address address = ensureOwnedAddress(id, redirectAttributes);
        if (address != null) {
            try {
                addressService.markDefault(address.getUser().getId(), id);
                redirectAttributes.addFlashAttribute("successMessageAddress", "預設地址已更新");
            } catch (IllegalArgumentException ex) {
                redirectAttributes.addFlashAttribute("errorMessageAddress", ex.getMessage());
            }
        }
        return "redirect:/account/profile#addressesSection";
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

    private Address ensureOwnedAddress(Long id, RedirectAttributes redirectAttributes) {
        User user = currentUser();
        if (user == null) {
            return null;
        }
        Address address = addressService.getById(id);
        if (address == null || address.getUser() == null || !address.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessageAddress", "無法找到該地址");
            return null;
        }
        return address;
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
