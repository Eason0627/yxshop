package com.yxshop.Module.Admin.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_login_log")
public class AdminLoginLogEntity {
    @TableId
    private Long id;
    @TableField("admin_user_id")
    private Long adminUserId;
    private String username;
    @TableField("login_ip")
    private String loginIp;
    private String device;
    private String browser;
    private String os;
    private Integer status;
    @TableField("failure_reason")
    private String failureReason;
    @TableField("login_at")
    private LocalDateTime loginAt;
}
