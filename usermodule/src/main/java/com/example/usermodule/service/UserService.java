package com.example.usermodule.service;

import com.example.usermodule.data.entity.Response.UserDTO;
import com.example.usermodule.data.entity.Role;
import com.example.usermodule.data.entity.UserToken;
import com.example.usermodule.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
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
            List<Object[]> roleObjects = userRoleRepository.findRolesByUserId(user.getId());
            List<Role> roles = roleObjects.stream()
                    .map(obj -> {
                        Role r = new Role();
                        r.setId(((Number) obj[0]).longValue());
                        r.setName((String) obj[1]);
                        r.setDescription((String) obj[2]);
                        return r;
                    }).toList();
            dto.setRoles(roles.stream().map(Role::getName).collect(Collectors.toList()));

            // permissions theo role
            List<String> permissions = roles.stream()
                    .flatMap(role -> {
                        List<Object[]> permObjs = rolePermissionRepository.findPermissionsByRoleId(role.getId());
                        return permObjs.stream().map(obj -> (String) obj[1]); // obj[1] = code
                    })
                    .distinct()
                    .toList();
            dto.setPermissions(permissions);

            // active tokens
            List<UserToken> tokens = userTokenRepository.findActiveTokensByUserId(user.getId());
            dto.setActiveTokens(tokens.stream().map(UserToken::getRefreshToken).toList());

            return dto;
        });
    }
}
