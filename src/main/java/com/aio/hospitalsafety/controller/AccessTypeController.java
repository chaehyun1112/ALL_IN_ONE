package com.aio.hospitalsafety.controller;

import com.aio.hospitalsafety.common.AuthenticationSessionManager;
import com.aio.hospitalsafety.common.SessionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccessTypeController {

    @GetMapping("/access-type")
    public String accessType(Authentication authentication,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        HttpSession session = AuthenticationSessionManager.expireAuthentication(
                request, response, authentication);
        session.removeAttribute(SessionConstants.LOGIN_ACCESS_TYPE);
        return "html/access-type";
    }
}
