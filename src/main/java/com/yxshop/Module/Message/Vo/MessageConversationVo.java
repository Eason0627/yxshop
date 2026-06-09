package com.yxshop.Module.Message.Vo;

import lombok.Data;

@Data
public class MessageConversationVo {
    private Long id;
    private String conversationType;
    private String name;
    private String avatar;
    private String lastMessage;
    private Integer unreadCount;
    /** Formatted as "yyyy-MM-dd HH:mm:ss", empty string if null */
    private String lastMessageTime;
}
