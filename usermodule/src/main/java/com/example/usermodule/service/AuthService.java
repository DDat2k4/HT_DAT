package com.example.usermodule.service;

import com.example.usermodule.data.entity.User;
import com.example.usermodule.data.entity.UserToken;
import com.example.usermodule.data.request.AuthProperties;
import com.example.usermodule.data.response.AuthResponse;
import com.example.usermodule.exception.AuthException;
import com.example.usermodule.repository.UserRepository;
import com.example.usermodule.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    // LOGIN
    public AuthResponse login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("User not found"));

        // Check locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AuthException("Account is locked until " + user.getLockedUntil());
        }

        // Check password
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);

            if (user.getFailedAttempts() >= authProperties.getMaxFailedAttempts()) {
                user.setLockedUntil(LocalDateTime.now()
                        .plus(authProperties.getLockDurationMinutes(), ChronoUnit.MINUTES));
                user.setFailedAttempts(0);
                log.warn("User {} locked until {}", username, user.getLockedUntil());
            }

            userRepository.save(user);
            log.warn("Login failed for user {}, attempts={}", username, user.getFailedAttempts());
            throw new AuthException("Invalid credentials");
        }

        // Success
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("User {} logged in successfully at {}", username, user.getLastLogin());
        return generateTokens(user);
    }

    // REFRESH TOKEN
    public AuthResponse refreshToken(String refreshToken) {
        UserToken token = userTokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new AuthException("Refresh token not found or revoked"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthException("Refresh token expired");
        }

        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("User not found"));

        if (!jwtService.validateToken(refreshToken, user.getUsername())) {
            throw new AuthException("Invalid refresh token");
        }

        log.info("Refresh token used for user {}", username);
        String newAccessToken = jwtService.generateToken(user.getUsername());
        return new AuthResponse(newAccessToken, refreshToken);
    }

    // LOGOUT (1 device)
    public void logout(String refreshToken) {
        UserToken token = userTokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new AuthException("Refresh token not found or already revoked"));

        token.setRevoked(true);
        userTokenRepository.save(token);
        log.info("Refresh token revoked for userId={}", token.getUserId());
    }

    // LOGOUT ALL DEVICES
    public void logoutAll(Long userId) {
        userTokenRepository.findActiveTokensByUserId(userId)
                .forEach(token -> {
                    token.setRevoked(true);
                    userTokenRepository.save(token);
                });
        log.info("All refresh tokens revoked for userId={}", userId);
    }

    // CHANGE PASSWORD
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new AuthException("Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // revoke all tokens
        logoutAll(user.getId());
        log.info("Password changed and tokens revoked for user {}", username);
    }

    // REUSABLE: generate tokens
    private AuthResponse generateTokens(User user) {
        // revoke old tokens
        userTokenRepository.findActiveTokensByUserId(user.getId())
                .forEach(token -> {
                    token.setRevoked(true);
                    userTokenRepository.save(token);
                });

        String accessToken = jwtService.generateToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        UserToken userToken = new UserToken();
        userToken.setUserId(user.getId());
        userToken.setRefreshToken(refreshToken);
        userToken.setCreatedAt(LocalDateTime.now());
        userToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        userToken.setRevoked(false);
        userTokenRepository.save(userToken);

        return new AuthResponse(accessToken, refreshToken);
    }
}
