package com.example.usermodule.service;

import com.example.usermodule.data.pojo.UserDTO;
import com.example.usermodule.data.entity.Role;
import com.example.usermodule.data.entity.UserToken;
import com.example.usermodule.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final UserTokenRepository userTokenRepository;

    /**
     * Lấy thông tin user đầy đủ (profile + roles + permissions + tokens)
     */
    public Optional<UserDTO> getUserDetail(Long userId) {
        return userRepository.findById(userId).map(user -> {
            UserDTO dto = new UserDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setPhone(user.getPhone());
            dto.setLastLogin(user.getLastLogin());

            // profile
            userProfileRepository.findByUserId(user.getId()).ifPresent(profile -> {
                dto.setName(profile.getName());
                dto.setAvatar(profile.getAvatar());
            });

            // roles
            Set<Role> roles = userRoleRepository.findRolesByUserId(user.getId())
                    .stream()
                    .map(obj -> new Role(
                            ((Number) obj[0]).longValue(),
                            (String) obj[1],
                            (String) obj[2]
                    ))
                    .collect(Collectors.toSet());

            dto.setRoles(
                    roles.stream()
                            .map(Role::getName)
                            .collect(Collectors.toSet())
            );

            // permissions theo roles
            Set<String> permissions = roles.stream()
                    .flatMap(role ->
                            rolePermissionRepository.findPermissionsByRoleId(role.getId())
                                    .stream()
                                    .map(obj -> (String) obj[1])
                    )
                    .collect(Collectors.toSet());
            dto.setPermissions(permissions);

            // active tokens (refreshTokens)
            List<String> activeTokens = userTokenRepository.findActiveTokensByUserId(user.getId())
                    .stream()
                    .map(UserToken::getRefreshToken)
                    .toList();
            dto.setActiveTokens(activeTokens);

            return dto;
        });
    }
}