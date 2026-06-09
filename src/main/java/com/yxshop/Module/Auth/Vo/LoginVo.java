package com.yxshop.Module.Auth.Vo;

import lombok.Data;

@Data
public class LoginVo {
    private String token;
    private Long userId;
    private String username;
    private String role;
    private String userType;
}
