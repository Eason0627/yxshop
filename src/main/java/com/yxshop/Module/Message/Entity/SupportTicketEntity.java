package com.yxshop.Module.Message.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("support_ticket")
public class SupportTicketEntity {
    @TableId
    private Long id;
    @TableField("ticket_no")
    private String ticketNo;
    @TableField("user_id")
    private Long userId;
    private String subject;
    private String status;
    private String priority;
    @TableField("last_message")
    private String lastMessage;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
