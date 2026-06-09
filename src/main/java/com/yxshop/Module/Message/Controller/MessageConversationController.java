package com.yxshop.Module.Message.Controller;

import com.yxshop.Module.Message.Dto.MessageSendDto;
import com.yxshop.Module.Message.Entity.MessageConversationEntity;
import com.yxshop.Module.Message.Service.MessageConversationService;
import com.yxshop.Module.Notification.WebSocket.NotificationWebSocketSessionManager;
import com.yxshop.Utils.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/app/messages")
public class MessageConversationController {
    private final MessageConversationService messageConversationService;
    private final NotificationWebSocketSessionManager wsManager;

    public MessageConversationController(MessageConversationService messageConversationService,
                                          NotificationWebSocketSessionManager wsManager) {
        this.messageConversationService = messageConversationService;
        this.wsManager = wsManager;
    }

    @GetMapping
    public Result list(HttpServletRequest request,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(messageConversationService.listConversations(currentUserId(request), page, size));
    }

    @GetMapping("/{conversationId}")
    public Result messages(HttpServletRequest request,
                           @PathVariable Long conversationId,
                           @RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "50") Integer size) {
        return Result.success(messageConversationService.getMessages(currentUserId(request), conversationId, page, size));
    }

    @PostMapping("/{conversationId}")
    public Result send(HttpServletRequest request,
                       @PathVariable Long conversationId,
                       @RequestBody MessageSendDto dto) {
        dto.setConversationId(conversationId);
        Object record = messageConversationService.sendMessage(currentUserId(request), dto);
        // 实时通知所有在线管理员
        try { wsManager.pushToAdmins("im.user.message.new", record); } catch (Exception ignored) { }
        return Result.success(record);
    }

    @GetMapping("/unread-count")
    public Result unreadCount(HttpServletRequest request) {
        return Result.success(messageConversationService.unreadCount(currentUserId(request)));
    }

    @PutMapping("/{conversationId}/read")
    public Result markRead(HttpServletRequest request,
                           @PathVariable Long conversationId) {
        return Result.success(messageConversationService.markRead(currentUserId(request), conversationId));
    }

    @DeleteMapping("/{conversationId}")
    public Result delete(HttpServletRequest request,
                         @PathVariable Long conversationId) {
        return Result.success(messageConversationService.deleteConversation(currentUserId(request), conversationId));
    }

    /**
     * User starts or resumes the customer-service IM conversation.
     * Creates a conversation if none exists, marks it read, and returns
     * the conversationId plus recent message records.
     */
    @PostMapping("/service/start")
    public Result startServiceConversation(HttpServletRequest request) {
        Long userId = currentUserId(request);
        MessageConversationEntity conv = messageConversationService.ensureSystemConversation(
                userId, "customer_service", null, "在线客服", null, 0L);
        messageConversationService.markRead(userId, conv.getId());
        Object messages = messageConversationService.getMessages(userId, conv.getId(), 1, 50);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", conv.getId());
        if (messages instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> msgMap = (Map<String, Object>) messages;
            result.put("records", msgMap.get("records"));
            result.put("total", msgMap.get("total"));
        }
        return Result.success(result);
    }

    private Long currentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value == null) {
            throw new IllegalArgumentException("请先登录");
        }
        return Long.valueOf(String.valueOf(value));
    }
}
