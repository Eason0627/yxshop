package com.yxshop.Module.Notification.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxshop.Module.Notification.Dto.NotificationTemplateDto;
import com.yxshop.Module.Notification.Entity.NotificationEntity;

import java.util.List;
import java.util.Map;

public interface NotificationService extends IService<NotificationEntity> {
    void send(Long userId, String title, String content, String bizType, Long bizId);

    void sendByTemplate(Long userId, String templateCode, Map<String, Object> params, Long bizId);

    List<NotificationEntity> listMine(Long userId);

    Map<String, Object> summary(Long userId);

    void markRead(Long userId, Long id);

    void markAllRead(Long userId);

    /** 删除单条通知（仅本人可删） */
    void delete(Long userId, Long id);

    /** 获取单条通知详情 */
    NotificationEntity getOne(Long userId, Long id);

    /** 管理员查询所有通知记录（分页） */
    Object adminList(Integer page, Integer size, String bizType, Long userId);

    /** 管理员推送通知给指定用户或广播 */
    Object adminPush(Long adminId, com.yxshop.Module.Notification.Dto.NotificationPushDto dto);

    Object saveTemplate(NotificationTemplateDto dto);

    Object listTemplates();
}
