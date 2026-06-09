package com.yxshop.Module.Auth.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuthTokenEntity {
    private String tokenId;
    private Long userId;
    private String userType;
    private String role;
    private LocalDateTime expiresAt;
}
