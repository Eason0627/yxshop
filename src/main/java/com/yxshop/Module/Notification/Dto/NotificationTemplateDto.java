package com.yxshop.Module.Notification.Dto;

import lombok.Data;

@Data
public class NotificationTemplateDto {
    private String code;
    private String titleTemplate;
    private String contentTemplate;
    private String bizType;
    private Integer status;
}
