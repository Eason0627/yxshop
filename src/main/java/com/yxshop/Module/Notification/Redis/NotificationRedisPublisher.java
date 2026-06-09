package com.yxshop.Module.Notification.Redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yxshop.Module.Notification.Vo.NotificationRedisMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationRedisPublisher {
    public static final String CHANNEL = "yxshop:notification:push";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationNodeId nodeId;

    public NotificationRedisPublisher(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                      ObjectMapper objectMapper,
                                      NotificationNodeId nodeId) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.objectMapper = objectMapper;
        this.nodeId = nodeId;
    }

    public void publish(Long userId, String type, Object data) {
        if (redisTemplate == null || userId == null) {
            return;
        }
        NotificationRedisMessage message = new NotificationRedisMessage();
        message.setOriginNodeId(nodeId.value());
        message.setUserId(userId);
        message.setType(type);
        message.setData(data);
        try {
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException ignored) {
        }
    }
}
