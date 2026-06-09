package com.yxshop.Module.Notification.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification_template")
public class NotificationTemplateEntity {
    @TableId
    private Long id;
    private String code;
    @TableField("title_template")
    private String titleTemplate;
    @TableField("content_template")
    private String contentTemplate;
    @TableField("biz_type")
    private String bizType;
    private Integer status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
