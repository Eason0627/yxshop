package com.yxshop.Module.Message.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxshop.Module.Message.Entity.MessageConversationEntity;
import com.yxshop.Module.Message.Entity.MessageRecordEntity;
import com.yxshop.Module.Message.Mapper.MessageConversationMapper;
import com.yxshop.Module.Message.Mapper.MessageRecordMapper;
import com.yxshop.Module.Message.Service.MessageConversationService;
import com.yxshop.Module.Notification.WebSocket.NotificationWebSocketSessionManager;
import com.yxshop.Module.Order.Entity.OrderEntity;
import com.yxshop.Module.Order.Mapper.OrderModuleMapper;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Module.User.Entity.User;
import com.yxshop.Module.User.Mapper.UserMapper;
import com.yxshop.Utils.Result;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin-facing IM conversation inbox at /app/support/conversations.
 * Allows admin to view all users' IM conversations and reply to them.
 */
@RestController
@RequestMapping("/app/support/conversations")
public class SupportConversationController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MessageConversationMapper conversationMapper;
    private final MessageRecordMapper messageRecordMapper;
    private final UserMapper userMapper;
    private final MessageConversationService messageConversationService;
    private final NotificationWebSocketSessionManager wsManager;
    private final OrderModuleMapper orderMapper;
    private final ShopModuleMapper shopMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(28, 2);

    public SupportConversationController(MessageConversationMapper conversationMapper,
                                          MessageRecordMapper messageRecordMapper,
                                          UserMapper userMapper,
                                          MessageConversationService messageConversationService,
                                          NotificationWebSocketSessionManager wsManager,
                                          OrderModuleMapper orderMapper,
                                          ShopModuleMapper shopMapper) {
        this.conversationMapper = conversationMapper;
        this.messageRecordMapper = messageRecordMapper;
        this.userMapper = userMapper;
        this.messageConversationService = messageConversationService;
        this.wsManager = wsManager;
        this.orderMapper = orderMapper;
        this.shopMapper = shopMapper;
    }

    // ── Role helper ───────────────────────────────────────────────────────────

    private void requireAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("currentUserRole");
        String r = role == null ? "" : role.toString();
        if (!"Admin".equals(r) && !"ShopOwner".equals(r)) {
            throw new RuntimeException("仅管理员可执行此操作");
        }
    }

    /**
     * Admin/ShopOwner conversation list.
     * Admin sees all; ShopOwner sees only conversations from customers who ordered in their shop.
     */
    @GetMapping
    public Result listAll(HttpServletRequest request,
                          @RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "50") Integer pageSize) {
        requireAdmin(request);
        Object roleAttr = request.getAttribute("currentUserRole");
        String role = roleAttr == null ? "" : roleAttr.toString();
        Object userIdAttr = request.getAttribute("currentUserId");

        LambdaQueryWrapper<MessageConversationEntity> wrapper = new LambdaQueryWrapper<MessageConversationEntity>()
                .eq(MessageConversationEntity::getStatus, 1)
                .orderByDesc(MessageConversationEntity::getLastMessageTime)
                .orderByDesc(MessageConversationEntity::getUpdatedAt);

        if ("ShopOwner".equals(role) && userIdAttr != null) {
            Long ownerId = Long.valueOf(userIdAttr.toString());
            QueryWrapper<ShopEntity> shopQw = new QueryWrapper<>();
            shopQw.eq("owner_user_id", ownerId).eq("status", "Active").last("LIMIT 1");
            ShopEntity shop = shopMapper.selectOne(shopQw);
            if (shop != null) {
                QueryWrapper<OrderEntity> orderQw = new QueryWrapper<>();
                orderQw.eq("shop_id", shop.getShopId()).select("DISTINCT customer_id");
                java.util.List<Long> customerIds = orderMapper.selectList(orderQw)
                        .stream()
                        .filter(java.util.Objects::nonNull)
                        .map(OrderEntity::getCustomerId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .collect(java.util.stream.Collectors.toList());
                if (customerIds.isEmpty()) {
                    // 该店铺暂无订单客户，返回空列表
                    java.util.Map<String, Object> empty = new java.util.LinkedHashMap<>();
                    empty.put("records", java.util.Collections.emptyList());
                    empty.put("total", 0);
                    empty.put("current", pageNum);
                    empty.put("size", pageSize);
                    return Result.success(empty);
                }
                wrapper.in(MessageConversationEntity::getUserId, customerIds);
            }
        }

        Page<MessageConversationEntity> entityPage = conversationMapper.selectPage(
                new Page<>(safePage(pageNum), safeSize(pageSize)), wrapper);

        List<Map<String, Object>> records = entityPage.getRecords().stream()
                .map(this::enrichConversation)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", entityPage.getTotal());
        result.put("current", entityPage.getCurrent());
        result.put("size", entityPage.getSize());
        return Result.success(result);
    }

    /**
     * Get messages for a specific conversation (oldest first).
     * Admin can access any conversation regardless of userId.
     */
    @GetMapping("/{conversationId}/messages")
    public Result getMessages(HttpServletRequest request,
                              @PathVariable Long conversationId,
                              @RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "50") Integer pageSize) {
        requireAdmin(request);
        MessageConversationEntity conv = conversationMapper.selectById(conversationId);
        if (conv == null) throw new RuntimeException("会话不存在");

        LambdaQueryWrapper<MessageRecordEntity> wrapper = new LambdaQueryWrapper<MessageRecordEntity>()
                .eq(MessageRecordEntity::getConversationId, conversationId)
                .eq(MessageRecordEntity::getStatus, 1)
                .orderByAsc(MessageRecordEntity::getSentAt);
        Page<MessageRecordEntity> msgPage = messageRecordMapper.selectPage(
                new Page<>(safePage(pageNum), safeSize(pageSize)), wrapper);

        List<Map<String, Object>> messages = msgPage.getRecords().stream()
                .map(this::toMessageMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", conversationId);
        result.put("records", messages);
        result.put("total", msgPage.getTotal());
        return Result.success(result);
    }

    /**
     * Admin sends a reply into a conversation.
     * Pushes a real-time WebSocket event to the conversation owner.
     */
    @PostMapping("/{conversationId}/reply")
    @Transactional(rollbackFor = Exception.class)
    public Result reply(HttpServletRequest request,
                        @RequestAttribute("currentUserId") Object currentUserId,
                        @PathVariable Long conversationId,
                        @RequestBody Map<String, Object> body) {
        requireAdmin(request);
        MessageConversationEntity conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) throw new RuntimeException("会话不存在");

        Object contentObj = body.get("content");
        String content = contentObj == null ? "" : contentObj.toString().trim();
        if (content.isEmpty()) throw new RuntimeException("消息内容不能为空");

        Long adminId = Long.parseLong(currentUserId.toString());

        MessageRecordEntity record = new MessageRecordEntity();
        record.setId(idWorker.nextId());
        record.setConversationId(conversationId);
        record.setMessageType("text");
        record.setSenderType("Admin");
        record.setSenderId(adminId);
        record.setContentText(content);
        record.setStatus(1);
        record.setSentAt(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        messageRecordMapper.insert(record);

        conversation.setLastMessage(content.length() > 100 ? content.substring(0, 100) + "..." : content);
        conversation.setLastMessageTime(record.getSentAt());
        conversation.setUnreadCount((conversation.getUnreadCount() == null ? 0 : conversation.getUnreadCount()) + 1);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        // Push real-time event to the user via WebSocket
        try {
            wsManager.push(conversation.getUserId(), "im.message.new", toMessageMap(record));
        } catch (Exception ignored) { }

        return Result.success(toMessageMap(record));
    }

    /**
     * Admin proactively starts an IM conversation with a user.
     * Creates or resumes the customer_service conversation for that user,
     * sends the initial message, and pushes a WebSocket event.
     */
    @PostMapping("/start")
    @Transactional(rollbackFor = Exception.class)
    public Result startConversation(HttpServletRequest request,
                                    @RequestAttribute("currentUserId") Object currentUserId,
                                    @RequestBody Map<String, Object> body) {
        requireAdmin(request);
        Object userIdObj = body.get("userId");
        Object contentObj = body.get("content");
        if (userIdObj == null) throw new RuntimeException("userId不能为空");
        Long targetUserId = Long.parseLong(userIdObj.toString());
        String content = contentObj == null ? "" : contentObj.toString().trim();
        if (content.isEmpty()) throw new RuntimeException("消息内容不能为空");

        Long adminId = Long.parseLong(currentUserId.toString());

        // Ensure the target user's customer_service conversation exists
        MessageConversationEntity conv = messageConversationService.ensureSystemConversation(
                targetUserId, "customer_service", null, "在线客服", null, 0L);

        // Insert message record as Admin
        MessageRecordEntity record = new MessageRecordEntity();
        record.setId(idWorker.nextId());
        record.setConversationId(conv.getId());
        record.setMessageType("text");
        record.setSenderType("Admin");
        record.setSenderId(adminId);
        record.setContentText(content);
        record.setStatus(1);
        record.setSentAt(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        messageRecordMapper.insert(record);

        conv.setLastMessage(content.length() > 100 ? content.substring(0, 100) + "..." : content);
        conv.setLastMessageTime(record.getSentAt());
        conv.setUnreadCount((conv.getUnreadCount() == null ? 0 : conv.getUnreadCount()) + 1);
        conv.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conv);

        // Notify user
        try {
            wsManager.push(targetUserId, "im.message.new", toMessageMap(record));
        } catch (Exception ignored) { }

        return Result.success(enrichConversation(conv));
    }

    /**
     * Admin hard-deletes a conversation and all its messages.
     */
    @DeleteMapping("/{conversationId}")
    @Transactional(rollbackFor = Exception.class)
    public Result deleteConversation(HttpServletRequest request,
                                     @PathVariable Long conversationId) {
        requireAdmin(request);
        MessageConversationEntity conv = conversationMapper.selectById(conversationId);
        if (conv == null) throw new RuntimeException("会话不存在");
        messageRecordMapper.delete(new LambdaQueryWrapper<MessageRecordEntity>()
                .eq(MessageRecordEntity::getConversationId, conversationId));
        conversationMapper.deleteById(conversationId);
        return Result.success("已删除");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<String, Object> enrichConversation(MessageConversationEntity conv) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", conv.getId());
        map.put("userId", conv.getUserId());
        User user = conv.getUserId() == null ? null : userMapper.getUserById(conv.getUserId());
        String username = user == null ? "用户" + conv.getUserId() : notBlank(user.getNick_name(), user.getUsername());
        String userAvatar = user == null ? null : user.getAvatar();
        map.put("username", username);
        map.put("userAvatar", userAvatar);
        map.put("name", conv.getName() != null ? conv.getName() : username);
        map.put("avatar", conv.getAvatar() != null ? conv.getAvatar() : userAvatar);
        map.put("lastMessage", conv.getLastMessage());
        map.put("lastTime", conv.getLastMessageTime() != null
                ? conv.getLastMessageTime().format(DT_FMT)
                : (conv.getUpdatedAt() != null ? conv.getUpdatedAt().format(DT_FMT) : ""));
        map.put("unreadCount", conv.getUnreadCount() == null ? 0 : conv.getUnreadCount());
        map.put("conversationType", conv.getConversationType());
        map.put("createdAt", conv.getCreatedAt() != null ? conv.getCreatedAt().format(DT_FMT) : "");
        return map;
    }

    private Map<String, Object> toMessageMap(MessageRecordEntity msg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", msg.getId());
        map.put("conversationId", msg.getConversationId());
        map.put("content", msg.getContentText());
        map.put("isAdmin", "Admin".equals(msg.getSenderType()) || "System".equals(msg.getSenderType()));
        map.put("senderType", msg.getSenderType());
        map.put("senderId", msg.getSenderId());
        map.put("messageType", msg.getMessageType());
        map.put("createTime", msg.getSentAt() != null ? msg.getSentAt().format(DT_FMT) : "");
        map.put("sentAt", msg.getSentAt() != null ? msg.getSentAt().format(DT_FMT) : "");
        return map;
    }

    private String notBlank(String first, String second) {
        return (first != null && !first.trim().isEmpty()) ? first : second;
    }

    private long safePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long safeSize(Integer size) {
        return size == null || size < 1 ? 20 : Math.min(size, 200);
    }
}
