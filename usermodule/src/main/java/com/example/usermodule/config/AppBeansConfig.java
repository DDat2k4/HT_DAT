package com.example.usermodule.config;

import com.example.usermodule.config.jwt.JwtAuthenticationFilter;
import com.example.usermodule.service.sso.CustomOidcUserService;
import com.example.usermodule.service.sso.CustomOAuth2UserService;
import com.example.usermodule.service.sso.OAuth2FailureHandler;
import com.example.usermodule.service.sso.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AppBeansConfig {

    // PasswordEncoder dùng chung
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}
