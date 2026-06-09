package com.yxshop.Module.Auth.Dto;

import lombok.Data;

@Data
public class RegisterDto {
    private String username;
    private String email;
    private String phone;
    private String password;
    private String code;
    private String scene;
    private String nickname;
}
