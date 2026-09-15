package com.peerview.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository users;
    private final JwtService jwtService;

    public OAuth2SuccessHandler(UserRepository users, JwtService jwtService) {
        this.users = users;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oauthUser = ((OAuth2AuthenticationToken) authentication).getPrincipal();
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        User user = users.findByEmailIgnoreCase(email).orElseGet(() -> users.save(new User(name == null ? email : name, email, null)));
        String target = request.getParameter("redirect_uri");
        // TODO(assumption): OAuth returns a token through a short-lived query parameter until the frontend callback is added.
        response.sendRedirect((target == null ? "http://localhost:5173" : target) + "?token=" + jwtService.issue(user));
    }
}