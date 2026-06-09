package com.yxshop.Module.Notification.WebSocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yxshop.Module.Notification.Vo.NotificationPushVo;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketSessionManager {
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    /** 所有在线管理员 session（Admin / ShopOwner 角色） */
    private final Set<WebSocketSession> adminSessions = ConcurrentHashMap.newKeySet();

    public NotificationWebSocketSessionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void add(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void remove(Long userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.remove(session);
        if (userSessions.isEmpty()) {
            sessions.remove(userId);
        }
        adminSessions.remove(session); // 安全调用，非管理员也不影响
    }

    public void addAdmin(WebSocketSession session) {
        adminSessions.add(session);
    }

    /** 推送事件给所有在线管理员 */
    public void pushToAdmins(String type, Object data) {
        if (adminSessions.isEmpty()) return;
        NotificationPushVo payload = new NotificationPushVo();
        payload.setType(type);
        payload.setData(data);
        String json = toJson(payload);
        for (WebSocketSession session : adminSessions) {
            if (!session.isOpen()) continue;
            try { session.sendMessage(new TextMessage(json)); } catch (IOException ignored) { }
        }
    }

    public int onlineCount(Long userId) {
        Set<WebSocketSession> userSessions = sessions.getOrDefault(userId, Collections.emptySet());
        return userSessions.size();
    }

    public void push(Long userId, String type, Object data) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }
        NotificationPushVo payload = new NotificationPushVo();
        payload.setType(type);
        payload.setData(data);
        String json = toJson(payload);
        for (WebSocketSession session : userSessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException ignored) {
            }
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("消息序列化失败", e);
        }
    }
}
