package com.example.demo.service.impl;

import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    @Autowired
    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.getUserByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + username);
        }
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                authoritiesFor(user)
        );
    }

    private Collection<? extends GrantedAuthority> authoritiesFor(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return java.util.List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }
}
