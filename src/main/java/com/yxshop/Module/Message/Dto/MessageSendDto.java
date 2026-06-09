package com.yxshop.Module.Message.Dto;

import lombok.Data;

@Data
public class MessageSendDto {
    private Long conversationId;
    private String contentText;
    private String messageType;
}
