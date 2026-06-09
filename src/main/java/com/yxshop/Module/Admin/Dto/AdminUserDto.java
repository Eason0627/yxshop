package com.yxshop.Module.Admin.Dto;

import lombok.Data;

@Data
public class AdminUserDto {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String mobile;
    private String email;
    private Integer status;
    private Integer isSuperAdmin;
    private String remark;
}
