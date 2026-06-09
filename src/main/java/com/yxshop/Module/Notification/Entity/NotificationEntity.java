package com.yxshop.Module.Notification.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class NotificationEntity {
    @TableId
    private Long id;
    @TableField("user_id")
    private Long userId;
    private String title;
    private String content;
    @TableField("biz_type")
    private String bizType;
    @TableField("biz_id")
    private Long bizId;
    @TableField("read_status")
    private Integer readStatus;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("read_at")
    private LocalDateTime readAt;
    /** 0=正常  1=用户已从列表删除（软删除，数据保留） */
    @TableField("hidden")
    private Integer hidden;
}
