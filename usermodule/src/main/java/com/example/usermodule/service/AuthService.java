package com.example.usermodule.service;


import com.example.usermodule.data.entity.Request.LoginRequest;
import com.example.usermodule.data.entity.Response.AuthResponse;
import com.example.usermodule.data.entity.UserToken;
import com.example.usermodule.repository.UserRepository;
import com.example.usermodule.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder; // cần config bean BCryptPasswordEncoder

    /**
     * Đăng nhập: verify username/password và sinh token
     */
    public Optional<AuthResponse> login(LoginRequest req) {
        return userRepository.findByUsername(req.getUsername())
                .filter(user -> passwordEncoder.matches(req.getPassword(), user.getPasswordHash()))
                .map(user -> {
                    // fake access token (thực tế nên dùng JWT)
                    String accessToken = UUID.randomUUID().toString();

                    // refresh token
                    String refreshToken = UUID.randomUUID().toString();
                    LocalDateTime now = LocalDateTime.now();

                    UserToken token = new UserToken();
                    token.setUserId(user.getId());
                    token.setRefreshToken(refreshToken);
                    token.setCreatedAt(now);
                    token.setExpiresAt(now.plusDays(7)); // refresh token 7 ngày
                    token.setRevoked(false);

                    userTokenRepository.save(token);

                    AuthResponse res = new AuthResponse();
                    res.setAccessToken(accessToken);
                    res.setRefreshToken(refreshToken);
                    return res;
                });
    }

    /**
     * Refresh token: cấp access token mới
     */
    public Optional<AuthResponse> refresh(String refreshToken) {
        return userTokenRepository.findAll().stream()
                .filter(t -> t.getRefreshToken().equals(refreshToken) && !t.getRevoked()
                        && t.getExpiresAt().isAfter(LocalDateTime.now()))
                .findFirst()
                .flatMap(token -> userRepository.findById(token.getUserId()))
                .map(user -> {
                    AuthResponse res = new AuthResponse();
                    res.setAccessToken(UUID.randomUUID().toString());
                    res.setRefreshToken(refreshToken); // giữ nguyên refresh
                    return res;
                });
    }

    /**
     * Logout: revoke refresh token
     */
    public void logout(String refreshToken) {
        userTokenRepository.findAll().stream()
                .filter(t -> t.getRefreshToken().equals(refreshToken))
                .findFirst()
                .ifPresent(token -> {
                    token.setRevoked(true);
                    userTokenRepository.save(token);
                });
    }
}
