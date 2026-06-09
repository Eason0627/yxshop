package com.yxshop.Module.Message.Dto;

import lombok.Data;

@Data
public class SupportTicketDto {
    private String subject;
    private String content;
    private String priority;
}
