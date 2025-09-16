package com.example.usermodule.service;

import com.example.usermodule.data.entity.User;
import com.example.usermodule.data.entity.UserToken;
import com.example.usermodule.data.response.AuthResponse;
import com.example.usermodule.repository.UserRepository;
import com.example.usermodule.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    //LOGIN
    public AuthResponse login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if account is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Account is locked until " + user.getLockedUntil());
        }

        // Check password
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            // Tăng failed attempts
            user.setFailedAttempts(user.getFailedAttempts() + 1);

            // Lock account nếu vượt quá số lần cho phép
            if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
                user.setFailedAttempts(0); // reset đếm sau khi khóa
            }

            userRepository.save(user);
            throw new RuntimeException("Invalid credentials");
        }

        // Login thành công → reset failedAttempts và lockedUntil
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Revoke old tokens
        userTokenRepository.findActiveTokensByUserId(user.getId())
                .forEach(token -> {
                    token.setRevoked(true);
                    userTokenRepository.save(token);
                });

        // Generate new tokens
        String accessToken = jwtService.generateToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        // Save refresh token in DB
        UserToken userToken = new UserToken();
        userToken.setUserId(user.getId());
        userToken.setRefreshToken(refreshToken);
        userToken.setCreatedAt(LocalDateTime.now());
        userToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        userToken.setRevoked(false);
        userTokenRepository.save(userToken);

        return new AuthResponse(accessToken, refreshToken);
    }

    //REFRESH TOKEN
    public AuthResponse refreshToken(String refreshToken) {
        UserToken token = userTokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found or revoked"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!jwtService.validateToken(refreshToken, user.getUsername())) {
            throw new RuntimeException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateToken(user.getUsername());
        return new AuthResponse(newAccessToken, refreshToken);
    }

    //LOGOUT 1 TOKEN
    public void logout(String refreshToken) {
        UserToken token = userTokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found or already revoked"));

        token.setRevoked(true);
        userTokenRepository.save(token);
    }

    //LOGOUT ALL DEVICES
    public void logoutAll(Long userId) {
        userTokenRepository.findActiveTokensByUserId(userId)
                .forEach(token -> {
                    token.setRevoked(true);
                    userTokenRepository.save(token);
                });
    }

    //CHANGE PASSWORD
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Old password is incorrect");
        }

        // Update mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke tất cả refresh tokens → user phải login lại
        userTokenRepository.findActiveTokensByUserId(user.getId())
                .forEach(token -> {
                    token.setRevoked(true);
                    userTokenRepository.save(token);
                });
    }

}
