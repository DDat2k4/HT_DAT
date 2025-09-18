package com.example.usermodule.service.sso;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        log.info("OAuth2 login success: {}", authentication.getName());

        Object principal = authentication.getPrincipal();
        Map<String, Object> resp = new HashMap<>();

        if (principal instanceof OAuth2User oauth2User) {
            resp.put("username", oauth2User.getAttribute("localUsername"));
            resp.put("accessToken", oauth2User.getAttribute("localAccessToken"));
            resp.put("refreshToken", oauth2User.getAttribute("localRefreshToken"));
            resp.put("email", oauth2User.getAttribute("email"));
            resp.put("provider", detectProvider(oauth2User));
        }

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), resp);
    }

    private String detectProvider(OAuth2User user) {
        if (user.getAttributes().containsKey("sub")) return "google";
        if (user.getAttributes().containsKey("id")) return "facebook";
        return "unknown";
    }
}
