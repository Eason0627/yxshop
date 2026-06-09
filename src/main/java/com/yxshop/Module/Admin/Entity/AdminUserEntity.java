package com.yxshop.Module.Admin.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_user")
public class AdminUserEntity {
    @TableId
    private Long id;
    private String username;
    @TableField("password_hash")
    private String passwordHash;
    @TableField("real_name")
    private String realName;
    private String nickname;
    private String mobile;
    private String email;
    private Integer status;
    @TableField("is_super_admin")
    private Integer isSuperAdmin;
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
}
