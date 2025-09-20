package com.example.usermodule.service.sso;

import com.example.usermodule.data.response.AuthResponse;
import com.example.usermodule.repository.UserRepository;
import com.example.usermodule.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        log.info("OAuth2 login success: {}", authentication.getName());

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof OAuth2User oauth2User)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String username = oauth2User.getAttribute("localUsername");
        String accessToken = oauth2User.getAttribute("localAccessToken");
        String refreshToken = oauth2User.getAttribute("localRefreshToken");

        Set<String> roles = new HashSet<>();
        Set<String> permissions = new HashSet<>();

        // Lấy roles/permissions từ DB
        if (username != null) {
            userRepository.findByUsername(username).ifPresent(user -> {
                userService.getUserDetail(user.getId()).ifPresent(detail -> {
                    if (detail.getRoles() != null) roles.addAll(detail.getRoles());
                    if (detail.getPermissions() != null) permissions.addAll(detail.getPermissions());
                });
            });
        }

        AuthResponse authResponse = new AuthResponse(
                accessToken,
                refreshToken,
                roles,
                permissions
        );

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), authResponse);
    }

    private String detectProvider(OAuth2User user) {
        if (user.getAttributes().containsKey("sub")) return "google";
        if (user.getAttributes().containsKey("id")) return "facebook";
        return "unknown";
    }
}
