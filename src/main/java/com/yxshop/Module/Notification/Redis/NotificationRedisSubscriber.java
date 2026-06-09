package com.yxshop.Module.Notification.Redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yxshop.Module.Notification.Vo.NotificationRedisMessage;
import com.yxshop.Module.Notification.WebSocket.NotificationWebSocketSessionManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationRedisSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final NotificationWebSocketSessionManager sessionManager;
    private final NotificationNodeId nodeId;

    public NotificationRedisSubscriber(ObjectMapper objectMapper,
                                       NotificationWebSocketSessionManager sessionManager,
                                       NotificationNodeId nodeId) {
        this.objectMapper = objectMapper;
        this.sessionManager = sessionManager;
        this.nodeId = nodeId;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            NotificationRedisMessage payload = objectMapper.readValue(new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8), NotificationRedisMessage.class);
            if (nodeId.value().equals(payload.getOriginNodeId())) {
                return;
            }
            sessionManager.push(payload.getUserId(), payload.getType(), payload.getData());
        } catch (Exception ignored) {
        }
    }
}
