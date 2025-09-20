package com.example.usermodule.config.sso;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String FRONTEND_URL = "http://localhost:3000";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        if (authentication == null) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof OidcUser oidcUser)) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        String localAccessToken = (String) oidcUser.getAttributes().get("localAccessToken");
        String localRefreshToken = (String) oidcUser.getAttributes().get("localRefreshToken");

        if (localAccessToken != null) {
            Cookie accessCookie = new Cookie("ACCESS_TOKEN", localAccessToken);
            accessCookie.setHttpOnly(true);
            accessCookie.setSecure(false);
            accessCookie.setPath("/");
            accessCookie.setMaxAge((int) Duration.ofMinutes(15).getSeconds());
            response.addCookie(accessCookie);
        }

        if (localRefreshToken != null) {
            Cookie refreshCookie = new Cookie("REFRESH_TOKEN", localRefreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(false);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge((int) Duration.ofDays(7).getSeconds());
            response.addCookie(refreshCookie);
        }

        getRedirectStrategy().sendRedirect(request, response, FRONTEND_URL);
    }
}
