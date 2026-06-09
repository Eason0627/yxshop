package com.yxshop.Module.Notification.Vo;

import lombok.Data;

@Data
public class NotificationRedisMessage {
    private String originNodeId;
    private Long userId;
    private String type;
    private Object data;
}
