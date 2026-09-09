package com.aio.hospitalsafety.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

public final class AuthenticationSessionManager {

    private AuthenticationSessionManager() {
    }

    public static HttpSession expireAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        HttpSession session = request.getSession();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return session;
        }

        Object hospitalDomain = session.getAttribute(SessionConstants.HOSPITAL_DOMAIN);
        Object accessType = session.getAttribute(SessionConstants.LOGIN_ACCESS_TYPE);

        new SecurityContextLogoutHandler().logout(request, response, authentication);

        HttpSession newSession = request.getSession(true);
        if (hospitalDomain != null) {
            newSession.setAttribute(SessionConstants.HOSPITAL_DOMAIN, hospitalDomain);
        }
        if (accessType != null) {
            newSession.setAttribute(SessionConstants.LOGIN_ACCESS_TYPE, accessType);
        }
        return newSession;
    }
}
