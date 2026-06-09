package com.yxshop.Module.Message.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("support_message")
public class SupportMessageEntity {
    @TableId
    private Long id;
    @TableField("ticket_id")
    private Long ticketId;
    @TableField("sender_id")
    private Long senderId;
    @TableField("sender_role")
    private String senderRole;
    private String content;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
