package com.yxshop.Module.Message.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxshop.Module.Message.Dto.MessageSendDto;
import com.yxshop.Module.Message.Entity.MessageConversationEntity;
import com.yxshop.Module.Message.Entity.MessageRecordEntity;
import com.yxshop.Module.Message.Mapper.MessageConversationMapper;
import com.yxshop.Module.Message.Mapper.MessageRecordMapper;
import com.yxshop.Module.Message.Service.MessageConversationService;
import com.yxshop.Module.Message.Vo.MessageConversationVo;
import com.yxshop.Module.Notification.WebSocket.NotificationWebSocketSessionManager;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageConversationServiceImpl implements MessageConversationService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MessageConversationMapper conversationMapper;
    private final MessageRecordMapper messageRecordMapper;
    private final NotificationWebSocketSessionManager wsManager;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(27, 1);

    public MessageConversationServiceImpl(MessageConversationMapper conversationMapper,
                                          MessageRecordMapper messageRecordMapper,
                                          NotificationWebSocketSessionManager wsManager) {
        this.conversationMapper = conversationMapper;
        this.messageRecordMapper = messageRecordMapper;
        this.wsManager = wsManager;
    }

    @Override
    public Object listConversations(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<MessageConversationEntity> wrapper = new LambdaQueryWrapper<MessageConversationEntity>()
                .eq(MessageConversationEntity::getUserId, userId)
                .eq(MessageConversationEntity::getStatus, 1)
                .orderByDesc(MessageConversationEntity::getLastMessageTime);
        Page<MessageConversationEntity> result = conversationMapper.selectPage(
                new Page<>(safePage(page), safeSize(size)), wrapper);

        List<MessageConversationVo> vos = result.getRecords().stream().map(e -> {
            MessageConversationVo vo = new MessageConversationVo();
            vo.setId(e.getId());
            vo.setConversationType(e.getConversationType());
            vo.setName(e.getName());
            vo.setAvatar(e.getAvatar());
            vo.setLastMessage(e.getLastMessage());
            vo.setUnreadCount(e.getUnreadCount());
            vo.setLastMessageTime(e.getLastMessageTime() != null
                    ? e.getLastMessageTime().format(DT_FMT)
                    : (e.getUpdatedAt() != null ? e.getUpdatedAt().format(DT_FMT) : ""));
            return vo;
        }).collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", vos);
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return data;
    }

    @Override
    public Object getMessages(Long userId, Long conversationId, Integer page, Integer size) {
        MessageConversationEntity conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在");
        }

        LambdaQueryWrapper<MessageRecordEntity> wrapper = new LambdaQueryWrapper<MessageRecordEntity>()
                .eq(MessageRecordEntity::getConversationId, conversationId)
                .eq(MessageRecordEntity::getStatus, 1)
                .orderByAsc(MessageRecordEntity::getSentAt);
        Page<MessageRecordEntity> result = messageRecordMapper.selectPage(
                new Page<>(safePage(page), safeSize(size)), wrapper);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("conversation", conversation);
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object sendMessage(Long userId, MessageSendDto dto) {
        if (dto == null || dto.getContentText() == null || dto.getContentText().trim().isEmpty()) {
            throw new RuntimeException("消息内容不能为空");
        }

        MessageConversationEntity conversation;
        if (dto.getConversationId() != null) {
            conversation = conversationMapper.selectById(dto.getConversationId());
            if (conversation == null || !conversation.getUserId().equals(userId)) {
                throw new RuntimeException("会话不存在");
            }
        } else {
            throw new RuntimeException("会话ID不能为空");
        }

        return createMessageRecord(conversation, userId, "Customer", dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object markRead(Long userId, Long conversationId) {
        MessageConversationEntity conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在");
        }
        conversation.setUnreadCount(0);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return conversation;
    }

    @Override
    public Object unreadCount(Long userId) {
        List<MessageConversationEntity> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<MessageConversationEntity>()
                        .eq(MessageConversationEntity::getUserId, userId)
                        .eq(MessageConversationEntity::getStatus, 1));
        int total = conversations.stream().mapToInt(c -> c.getUnreadCount() == null ? 0 : c.getUnreadCount()).sum();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("unreadCount", total);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object deleteConversation(Long userId, Long conversationId) {
        MessageConversationEntity conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在");
        }
        conversation.setStatus(0);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return conversation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageConversationEntity ensureSystemConversation(Long userId, String conversationType,
                                                               String groupType, String name, String avatar, Long targetId) {
        LambdaQueryWrapper<MessageConversationEntity> wrapper = new LambdaQueryWrapper<MessageConversationEntity>()
                .eq(MessageConversationEntity::getUserId, userId)
                .eq(MessageConversationEntity::getConversationType, conversationType)
                .eq(MessageConversationEntity::getTargetId, targetId)
                .eq(MessageConversationEntity::getStatus, 1);

        MessageConversationEntity existing = conversationMapper.selectOne(wrapper);
        if (existing != null) {
            return existing;
        }

        MessageConversationEntity conversation = new MessageConversationEntity();
        conversation.setId(idWorker.nextId());
        conversation.setUserId(userId);
        conversation.setConversationType(conversationType);
        conversation.setGroupType(groupType);
        conversation.setName(name);
        conversation.setAvatar(avatar);
        conversation.setUnreadCount(0);
        conversation.setTargetId(targetId);
        conversation.setStatus(1);
        conversationMapper.insert(conversation);
        return conversation;
    }

    /**
     * 创建消息记录并更新会话
     */
    @Transactional(rollbackFor = Exception.class)
    public Object createMessageRecord(MessageConversationEntity conversation, Long senderId,
                                       String senderType, MessageSendDto dto) {
        MessageRecordEntity record = new MessageRecordEntity();
        record.setId(idWorker.nextId());
        record.setConversationId(conversation.getId());
        record.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : "text");
        record.setSenderType(senderType);
        record.setSenderId(senderId);
        record.setContentText(dto.getContentText());
        record.setStatus(1);
        record.setSentAt(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        messageRecordMapper.insert(record);

        // 更新会话
        conversation.setLastMessage(dto.getContentText().length() > 100
                ? dto.getContentText().substring(0, 100) + "..." : dto.getContentText());
        conversation.setLastMessageTime(record.getSentAt());
        // 仅对方（Admin）消息才计入用户未读数；用户自己发的消息不累加
        if (!"Customer".equals(senderType)) {
            conversation.setUnreadCount((conversation.getUnreadCount() == null ? 0 : conversation.getUnreadCount()) + 1);
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        // 实时推送给所有在线管理员
        if ("Customer".equals(senderType)) {
            try {
                java.util.Map<String, Object> push = new java.util.LinkedHashMap<>();
                push.put("id", record.getId());
                push.put("conversationId", record.getConversationId());
                push.put("content", record.getContentText());
                push.put("isAdmin", false);
                push.put("senderType", record.getSenderType());
                push.put("senderId", record.getSenderId());
                push.put("messageType", record.getMessageType());
                String ts = record.getSentAt() != null ? record.getSentAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
                push.put("createTime", ts);
                push.put("sentAt", ts);
                wsManager.pushToAdmins("im.user.message.new", push);
            } catch (Exception ignored) { }
        }

        return record;
    }

    /**
     * 系统消息快捷发送（供其他模块调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public Object sendSystemMessage(Long userId, String conversationType, String groupType,
                                     String name, String avatar, Long targetId,
                                     String title, String contentText, String messageType) {
        MessageConversationEntity conversation = ensureSystemConversation(
                userId, conversationType, groupType, name, avatar, targetId);

        MessageSendDto dto = new MessageSendDto();
        dto.setConversationId(conversation.getId());
        dto.setContentText(contentText);
        dto.setMessageType(messageType != null ? messageType : "system");
        return createMessageRecord(conversation, 0L, "System", dto);
    }

    private long safePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long safeSize(Integer size) {
        return size == null || size < 1 ? 20 : Math.min(size, 100);
    }
}
