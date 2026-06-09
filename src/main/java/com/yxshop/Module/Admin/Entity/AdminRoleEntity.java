package com.yxshop.Module.Admin.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_role")
public class AdminRoleEntity {
    @TableId
    private Long id;
    @TableField("role_code")
    private String roleCode;
    @TableField("role_name")
    private String roleName;
    private Integer status;
    private Integer sort;
    private String remark;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
