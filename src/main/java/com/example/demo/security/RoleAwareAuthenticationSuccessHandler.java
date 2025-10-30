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
        setDefaultTargetUrl("/account/profile");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        String roleHint = request.getParameter("roleHint");
        boolean wantsAdmin = "ADMIN".equalsIgnoreCase(roleHint);
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        if (wantsAdmin && !isAdmin) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            getRedirectStrategy().sendRedirect(request, response, "/?roleError");
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
