package com.yxshop.Module.Marketing.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("channel")
public class ChannelEntity {
    @TableId
    private Long id;
    private String name;
    private String icon;
    @TableField("icon_color")
    private String iconColor;
    @TableField("bg_color")
    private String bgColor;
    private String path;
    private Integer sort;
    private Integer status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
