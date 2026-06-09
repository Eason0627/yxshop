package com.yxshop.Module.Message.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message_conversation")
public class MessageConversationEntity {
    @TableId
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("conversation_type")
    private String conversationType;
    @TableField("group_type")
    private String groupType;
    private String name;
    private String avatar;
    @TableField("last_message")
    private String lastMessage;
    @TableField("last_message_time")
    private LocalDateTime lastMessageTime;
    @TableField("unread_count")
    private Integer unreadCount;
    @TableField("target_id")
    private Long targetId;
    private Integer status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
