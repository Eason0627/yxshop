package com.yxshop.Module.Notification.Dto;

import lombok.Data;

import java.util.List;

@Data
public class NotificationPushDto {
    /** 推送对象：null / 空列表 = 广播给所有用户，否则指定 userId 列表 */
    private List<Long> userIds;
    private String title;
    private String content;
    /** SYSTEM / ORDER / LOGISTICS / MARKETING / ANNOUNCE 等 */
    private String bizType;
    private Long bizId;
}
