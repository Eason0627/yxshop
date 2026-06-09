package com.yxshop.Module.Notification.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.Notification.Dto.NotificationPushDto;
import com.yxshop.Module.Notification.Dto.NotificationTemplateDto;
import com.yxshop.Module.Notification.Entity.NotificationEntity;
import com.yxshop.Module.Notification.Entity.NotificationTemplateEntity;
import com.yxshop.Module.Notification.Mapper.NotificationMapper;
import com.yxshop.Module.Notification.Mapper.NotificationTemplateMapper;
import com.yxshop.Module.Notification.Redis.NotificationRedisPublisher;
import com.yxshop.Module.Notification.Service.NotificationService;
import com.yxshop.Module.Notification.WebSocket.NotificationWebSocketSessionManager;
import com.yxshop.Module.User.Mapper.UserMapper;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Primary
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, NotificationEntity> implements NotificationService {
    private final NotificationTemplateMapper templateMapper;
    private final NotificationWebSocketSessionManager webSocketSessionManager;
    private final NotificationRedisPublisher redisPublisher;
    private final UserMapper userMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(11, 1);

    public NotificationServiceImpl(NotificationTemplateMapper templateMapper,
                                   NotificationWebSocketSessionManager webSocketSessionManager,
                                   NotificationRedisPublisher redisPublisher,
                                   UserMapper userMapper) {
        this.templateMapper = templateMapper;
        this.webSocketSessionManager = webSocketSessionManager;
        this.redisPublisher = redisPublisher;
        this.userMapper = userMapper;
    }

    @Override
    public void send(Long userId, String title, String content, String bizType, Long bizId) {
        if (userId == null) {
            return;
        }
        NotificationEntity notification = new NotificationEntity();
        notification.setId(idWorker.nextId());
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setBizType(bizType);
        notification.setBizId(bizId);
        notification.setReadStatus(0);
        notification.setCreatedAt(LocalDateTime.now());
        save(notification);
        push(userId, "notification.new", notification);
        push(userId, "notification.summary", summary(userId));
    }

    @Override
    public void sendByTemplate(Long userId, String templateCode, Map<String, Object> params, Long bizId) {
        QueryWrapper<NotificationTemplateEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("code", templateCode).eq("status", 1).last("LIMIT 1");
        NotificationTemplateEntity template = templateMapper.selectOne(wrapper);
        if (template == null) {
            throw new IllegalArgumentException("消息模板不存在或已停用");
        }
        Map<String, Object> values = params == null ? new HashMap<>() : params;
        send(userId, render(template.getTitleTemplate(), values), render(template.getContentTemplate(), values), template.getBizType(), bizId);
    }

    @Override
    public List<NotificationEntity> listMine(Long userId) {
        QueryWrapper<NotificationEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .and(w -> w.isNull("hidden").or().eq("hidden", 0))
               .orderByDesc("created_at");
        return list(wrapper);
    }

    @Override
    public Map<String, Object> summary(Long userId) {
        Map<String, Object> result = new HashMap<>();
        QueryWrapper<NotificationEntity> unreadWrapper = new QueryWrapper<>();
        unreadWrapper.eq("user_id", userId).eq("read_status", 0);
        result.put("unreadCount", count(unreadWrapper));
        result.put("recent", list(new QueryWrapper<NotificationEntity>()
                .eq("user_id", userId)
                .orderByDesc("created_at")
                .last("LIMIT 5")));
        result.put("onlineConnections", webSocketSessionManager.onlineCount(userId));
        return result;
    }

    @Override
    public void markRead(Long userId, Long id) {
        NotificationEntity notification = getById(id);
        if (notification == null || !userId.equals(notification.getUserId())) {
            throw new IllegalArgumentException("消息不存在");
        }
        notification.setReadStatus(1);
        notification.setReadAt(LocalDateTime.now());
        updateById(notification);
        push(userId, "notification.read", notification);
        push(userId, "notification.summary", summary(userId));
    }

    @Override
    public void delete(Long userId, Long id) {
        NotificationEntity notification = getById(id);
        if (notification == null || !userId.equals(notification.getUserId())) {
            throw new IllegalArgumentException("消息不存在");
        }
        // 软删除：标记 hidden=1，保留数据库记录
        notification.setHidden(1);
        updateById(notification);
        push(userId, "notification.summary", summary(userId));
    }

    @Override
    public NotificationEntity getOne(Long userId, Long id) {
        NotificationEntity notification = getById(id);
        if (notification == null || !userId.equals(notification.getUserId())) {
            throw new IllegalArgumentException("消息不存在");
        }
        return notification;
    }

    @Override
    public Object adminList(Integer page, Integer size, String bizType, Long targetUserId) {
        QueryWrapper<NotificationEntity> wrapper = new QueryWrapper<>();
        if (bizType != null && !bizType.trim().isEmpty()) {
            wrapper.eq("biz_type", bizType.trim().toUpperCase());
        }
        if (targetUserId != null) {
            wrapper.eq("user_id", targetUserId);
        }
        wrapper.orderByDesc("created_at");
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        Page<NotificationEntity> entityPage = baseMapper.selectPage(
                new Page<>(safePage, safeSize), wrapper);

        List<Map<String, Object>> records = entityPage.getRecords().stream().map(n -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", n.getId());
            map.put("userId", n.getUserId());
            // Try to get username
            try {
                com.yxshop.Module.User.Entity.User u = userMapper.getUserById(n.getUserId());
                map.put("username", u == null ? String.valueOf(n.getUserId()) :
                        (u.getNick_name() != null && !u.getNick_name().isEmpty() ? u.getNick_name() : u.getUsername()));
            } catch (Exception e) {
                map.put("username", String.valueOf(n.getUserId()));
            }
            map.put("title", n.getTitle());
            map.put("content", n.getContent());
            map.put("bizType", n.getBizType());
            map.put("bizId", n.getBizId());
            map.put("readStatus", n.getReadStatus());
            map.put("createdAt", n.getCreatedAt());
            map.put("readAt", n.getReadAt());
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", entityPage.getTotal());
        result.put("current", entityPage.getCurrent());
        result.put("size", entityPage.getSize());
        return result;
    }

    @Override
    public Object adminPush(Long adminId, NotificationPushDto dto) {
        if (dto == null) throw new IllegalArgumentException("推送内容不能为空");
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) throw new IllegalArgumentException("通知标题不能为空");
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) throw new IllegalArgumentException("通知内容不能为空");
        String bizType = dto.getBizType() == null ? "SYSTEM" : dto.getBizType().toUpperCase();

        List<Long> targetIds = dto.getUserIds();
        if (targetIds == null || targetIds.isEmpty()) {
            // 广播：查询所有用户 ID
            targetIds = userMapper.selectList(null).stream()
                    .map(u -> u.getId())
                    .collect(Collectors.toList());
        }

        int sent = 0;
        for (Long uid : targetIds) {
            try {
                send(uid, dto.getTitle().trim(), dto.getContent().trim(), bizType, dto.getBizId());
                sent++;
            } catch (Exception ignored) { }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sent", sent);
        result.put("total", targetIds.size());
        return result;
    }

    @Override
    public void markAllRead(Long userId) {
        UpdateWrapper<NotificationEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("read_status", 0)
                .set("read_status", 1)
                .set("read_at", LocalDateTime.now());
        update(wrapper);
        push(userId, "notification.readAll", summary(userId));
    }

    @Override
    public Object saveTemplate(NotificationTemplateDto dto) {
        if (dto == null || dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("模板编码不能为空");
        }
        QueryWrapper<NotificationTemplateEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("code", dto.getCode()).last("LIMIT 1");
        NotificationTemplateEntity template = templateMapper.selectOne(wrapper);
        if (template == null) {
            template = new NotificationTemplateEntity();
            template.setId(idWorker.nextId());
            template.setCode(dto.getCode());
            template.setCreatedAt(LocalDateTime.now());
        }
        template.setTitleTemplate(dto.getTitleTemplate());
        template.setContentTemplate(dto.getContentTemplate());
        template.setBizType(dto.getBizType());
        template.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        template.setUpdatedAt(LocalDateTime.now());
        if (templateMapper.selectById(template.getId()) == null) {
            templateMapper.insert(template);
        } else {
            templateMapper.updateById(template);
        }
        return template;
    }

    @Override
    public Object listTemplates() {
        return templateMapper.selectList(new QueryWrapper<NotificationTemplateEntity>().orderByAsc("code"));
    }

    private String render(String template, Map<String, Object> params) {
        if (template == null) {
            return null;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return result;
    }

    private void push(Long userId, String type, Object data) {
        webSocketSessionManager.push(userId, type, data);
        redisPublisher.publish(userId, type, data);
    }
}
