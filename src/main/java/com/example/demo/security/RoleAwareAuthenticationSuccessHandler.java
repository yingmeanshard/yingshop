package com.example.demo.security;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

public class RoleAwareAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public RoleAwareAuthenticationSuccessHandler() {
        setDefaultTargetUrl("/products");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        boolean isStaff = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_STAFF"::equals);
        
        if (isAdmin) {
            getRedirectStrategy().sendRedirect(request, response, "/admin");
        } else if (isStaff) {
            getRedirectStrategy().sendRedirect(request, response, "/admin/staff");
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}
