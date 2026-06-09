package com.yxshop.Module.Message.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message_record")
public class MessageRecordEntity {
    @TableId
    private Long id;
    @TableField("conversation_id")
    private Long conversationId;
    @TableField("message_type")
    private String messageType;
    @TableField("sender_type")
    private String senderType;
    @TableField("sender_id")
    private Long senderId;
    @TableField("sender_name")
    private String senderName;
    @TableField("sender_avatar")
    private String senderAvatar;
    private String title;
    @TableField("content_text")
    private String contentText;
    @TableField("content_json")
    private String contentJson;
    private String tag;
    @TableField("tag_color")
    private String tagColor;
    private String actions;
    @TableField("target_id")
    private Long targetId;
    private Integer status;
    @TableField("sent_at")
    private LocalDateTime sentAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
