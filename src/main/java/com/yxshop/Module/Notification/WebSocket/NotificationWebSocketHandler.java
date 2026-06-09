package com.yxshop.Module.Notification.WebSocket;

import com.yxshop.Utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    private static final String USER_ID_KEY = "currentUserId";

    private final NotificationWebSocketSessionManager sessionManager;

    public NotificationWebSocketHandler(NotificationWebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = parseUserId(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid token"));
            return;
        }
        String role = parseRole(session);
        session.getAttributes().put(USER_ID_KEY, userId);
        session.getAttributes().put("currentUserRole", role);
        sessionManager.add(userId, session);
        if ("Admin".equals(role) || "ShopOwner".equals(role)) {
            sessionManager.addAdmin(session);
        }
        session.sendMessage(new TextMessage("{\"type\":\"connected\",\"data\":{\"userId\":\"" + userId + "\"}}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload)) {
            session.sendMessage(new TextMessage("{\"type\":\"pong\",\"data\":{}}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object value = session.getAttributes().get(USER_ID_KEY);
        if (value != null) {
            sessionManager.remove(Long.valueOf(String.valueOf(value)), session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Object value = session.getAttributes().get(USER_ID_KEY);
        if (value != null) {
            sessionManager.remove(Long.valueOf(String.valueOf(value)), session);
        }
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private Long parseUserId(WebSocketSession session) {
        Claims claims = parseClaims(session);
        Object id = claims == null ? null : claims.get("id");
        return id == null ? null : Long.valueOf(String.valueOf(id));
    }

    private String parseRole(WebSocketSession session) {
        Claims claims = parseClaims(session);
        Object role = claims == null ? null : claims.get("role");
        return role == null ? "Customer" : String.valueOf(role);
    }

    private Claims parseClaims(WebSocketSession session) {
        String token = queryParam(session.getUri(), "token");
        if (token == null || token.trim().isEmpty()) token = firstProtocol(session);
        if (token == null || token.trim().isEmpty()) return null;
        if (token.startsWith("Bearer ")) token = token.substring(7).trim();
        try {
            return JwtUtils.checkToken(token);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String queryParam(URI uri, String name) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String pair : uri.getQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && name.equals(parts[0])) {
                return parts[1];
            }
        }
        return null;
    }

    private String firstProtocol(WebSocketSession session) {
        return session.getAcceptedProtocol() == null ? null : session.getAcceptedProtocol();
    }
}
