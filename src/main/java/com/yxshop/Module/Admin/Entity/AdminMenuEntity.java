package com.yxshop.Module.Admin.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_menu")
public class AdminMenuEntity {
    @TableId
    private Long id;
    @TableField("parent_id")
    private Long parentId;
    @TableField("menu_type")
    private String menuType;
    @TableField("menu_name")
    private String menuName;
    @TableField("menu_code")
    private String menuCode;
    @TableField("route_path")
    private String routePath;
    @TableField("component_path")
    private String componentPath;
    @TableField("permission_code")
    private String permissionCode;
    private String icon;
    private Integer sort;
    @TableField("is_visible")
    private Integer isVisible;
    private Integer status;
    private String remark;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
