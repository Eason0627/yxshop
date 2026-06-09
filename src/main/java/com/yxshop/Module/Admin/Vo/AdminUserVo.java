package com.yxshop.Module.Admin.Vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVo {
    private Long id;
    private String username;
    private String realName;
    private String role = "Admin";
    private String nickname;
    private String mobile;
    private String email;
    private String avatar;
    private String jobTitle;
    private Integer status;
    private Integer isSuperAdmin;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
