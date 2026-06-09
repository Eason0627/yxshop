package com.yxshop.Module.Notification.Redis;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationNodeId {
    private final String value = UUID.randomUUID().toString();

    public String value() {
        return value;
    }
}
