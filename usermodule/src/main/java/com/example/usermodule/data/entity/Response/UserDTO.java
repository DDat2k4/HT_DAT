package com.example.usermodule.data.entity.Response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String name;
    private String avatar;
    private List<String> roles;
    private List<String> permissions;
    private List<String> activeTokens;
    private LocalDateTime lastLogin;
}
