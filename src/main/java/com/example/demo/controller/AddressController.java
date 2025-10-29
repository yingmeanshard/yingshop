package com.example.demo.controller;

import com.example.demo.model.Address;
import com.example.demo.model.User;
import com.example.demo.service.AddressService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/addresses")
public class AddressController {

    private final AddressService addressService;
    private final UserService userService;

    @Autowired
    public AddressController(AddressService addressService, UserService userService) {
        this.addressService = addressService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        List<Address> addresses = addressService.findByUser(user.getId());
        model.addAttribute("addresses", addresses);
        model.addAttribute("defaultAddressId", user.getDefaultAddressId());
        return "addresses";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("address", new Address());
        model.addAttribute("formTitle", "新增地址");
        model.addAttribute("defaultAddressId", user.getDefaultAddressId());
        return "address-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Address address = ensureOwnedAddress(id, redirectAttributes);
        if (address == null) {
            return "redirect:/addresses";
        }
        model.addAttribute("address", address);
        model.addAttribute("formTitle", "編輯地址");
        User user = currentUser();
        model.addAttribute("defaultAddressId", user != null ? user.getDefaultAddressId() : null);
        return "address-form";
    }

    @PostMapping
    public String create(@ModelAttribute Address address,
                         @RequestParam(value = "setDefault", required = false, defaultValue = "false") boolean setDefault,
                         RedirectAttributes redirectAttributes) {
        User user = currentUser();
        if (user == null) {
            return "redirect:/login";
        }
        address.setUser(user);
        Address savedAddress = addressService.save(address);
        if (setDefault && savedAddress.getId() != null) {
            addressService.markDefault(user.getId(), savedAddress.getId());
        }
        redirectAttributes.addFlashAttribute("successMessage", "地址已新增");
        return "redirect:/addresses";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute Address form,
                         @RequestParam(value = "setDefault", required = false, defaultValue = "false") boolean setDefault,
                         RedirectAttributes redirectAttributes) {
        Address address = ensureOwnedAddress(id, redirectAttributes);
        if (address == null) {
            return "redirect:/addresses";
        }
        address.setRecipientName(form.getRecipientName());
        address.setPhoneNumber(form.getPhoneNumber());
        address.setAddressLine1(form.getAddressLine1());
        address.setAddressLine2(form.getAddressLine2());
        address.setCity(form.getCity());
        address.setState(form.getState());
        address.setPostalCode(form.getPostalCode());
        addressService.save(address);
        if (setDefault) {
            addressService.markDefault(address.getUser().getId(), address.getId());
        }
        redirectAttributes.addFlashAttribute("successMessage", "地址已更新");
        return "redirect:/addresses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Address address = ensureOwnedAddress(id, redirectAttributes);
        if (address != null) {
            addressService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "地址已刪除");
        }
        return "redirect:/addresses";
    }

    @PostMapping("/{id}/default")
    public String markDefault(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Address address = ensureOwnedAddress(id, redirectAttributes);
        if (address != null) {
            try {
                addressService.markDefault(address.getUser().getId(), id);
                redirectAttributes.addFlashAttribute("successMessage", "預設地址已更新");
            } catch (IllegalArgumentException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            }
        }
        return "redirect:/addresses";
    }

    private Address ensureOwnedAddress(Long id, RedirectAttributes redirectAttributes) {
        User user = currentUser();
        if (user == null) {
            return null;
        }
        Address address = addressService.getById(id);
        if (address == null || address.getUser() == null || !address.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "無法找到該地址");
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