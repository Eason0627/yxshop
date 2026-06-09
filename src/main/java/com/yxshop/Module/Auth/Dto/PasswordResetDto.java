package com.yxshop.Module.Auth.Dto;

import lombok.Data;

@Data
public class PasswordResetDto {
    /** 手机号或邮箱（优先使用） */
    private String account;
    /** 兼容旧字段：仅 email 时使用 */
    private String email;
    private String code;
    private String scene;
    private String newPassword;

    /** 返回有效账号（account 优先，回退 email） */
    public String resolveAccount() {
        return (account != null && !account.isBlank()) ? account : email;
    }
}
