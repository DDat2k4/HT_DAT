package com.example.usermodule.service.sso;

import com.example.usermodule.data.entity.User;
import com.example.usermodule.data.entity.UserToken;
import com.example.usermodule.repository.UserRepository;
import com.example.usermodule.repository.UserTokenRepository;
import com.example.usermodule.service.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String email = (String) oauth2User.getAttributes().get("email");
        String tmpUsername = email != null ? email : "fb-" + oauth2User.getAttributes().get("id");

        // provision local user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setUid(UUID.randomUUID());
            u.setUsername(tmpUsername);
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            u.setActive((short) 1);
            u.setCreatedAt(LocalDateTime.now());
            u.setFailedAttempts(0);
            return userRepository.save(u);
        });

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // revoke old tokens
        userTokenRepository.findActiveTokensByUserId(user.getId()).forEach(t -> {
            t.setRevoked(true);
            userTokenRepository.save(t);
        });

        // issue new tokens
        String accessToken = jwtService.generateToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        UserToken ut = new UserToken();
        ut.setUserId(user.getId());
        ut.setRefreshToken(refreshToken);
        ut.setCreatedAt(LocalDateTime.now());
        ut.setExpiresAt(LocalDateTime.now().plusDays(7));
        ut.setRevoked(false);
        userTokenRepository.save(ut);

        // attach local tokens
        Map<String, Object> attrs = new HashMap<>(oauth2User.getAttributes());
        attrs.put("localAccessToken", accessToken);
        attrs.put("localRefreshToken", refreshToken);
        attrs.put("localUsername", user.getUsername());

        return new DefaultOAuth2User(oauth2User.getAuthorities(), attrs, "id");
    }
}
