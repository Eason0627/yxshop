package com.yxshop.Module.Message.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxshop.Module.Message.Dto.SupportMessageDto;
import com.yxshop.Module.Message.Dto.SupportTicketDto;
import com.yxshop.Module.Message.Entity.SupportMessageEntity;
import com.yxshop.Module.Message.Entity.SupportTicketEntity;
import com.yxshop.Module.Message.Mapper.SupportMessageMapper;
import com.yxshop.Module.Message.Mapper.SupportTicketMapper;
import com.yxshop.Module.Message.Service.SupportTicketService;
import com.yxshop.Module.Order.Entity.OrderEntity;
import com.yxshop.Module.Order.Mapper.OrderModuleMapper;
import com.yxshop.Module.User.Entity.User;
import com.yxshop.Module.User.Mapper.UserMapper;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SupportTicketServiceImpl implements SupportTicketService {
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SupportTicketMapper supportTicketMapper;
    private final SupportMessageMapper supportMessageMapper;
    private final UserMapper userMapper;
    private final OrderModuleMapper orderMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(26, 1);

    public SupportTicketServiceImpl(SupportTicketMapper supportTicketMapper,
                                    SupportMessageMapper supportMessageMapper,
                                    UserMapper userMapper,
                                    OrderModuleMapper orderMapper) {
        this.supportTicketMapper = supportTicketMapper;
        this.supportMessageMapper = supportMessageMapper;
        this.userMapper = userMapper;
        this.orderMapper = orderMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object create(Long userId, SupportTicketDto dto) {
        if (dto == null || dto.getSubject() == null || dto.getSubject().trim().isEmpty()) {
            throw new RuntimeException("工单主题不能为空");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new RuntimeException("工单内容不能为空");
        }
        SupportTicketEntity ticket = new SupportTicketEntity();
        ticket.setId(idWorker.nextId());
        ticket.setTicketNo("ST" + ticket.getId());
        ticket.setUserId(userId);
        ticket.setSubject(dto.getSubject().trim());
        ticket.setPriority(dto.getPriority() == null ? "Normal" : dto.getPriority());
        ticket.setStatus("Open");
        ticket.setLastMessage(dto.getContent());
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketMapper.insert(ticket);
        appendMessage(ticket, userId, "Customer", dto.getContent());
        return buildDetail(ticket, userId, "Customer");
    }

    @Override
    public Object myTickets(Long userId, Integer page, Integer size, String status) {
        LambdaQueryWrapper<SupportTicketEntity> wrapper = new LambdaQueryWrapper<SupportTicketEntity>()
                .eq(SupportTicketEntity::getUserId, userId)
                .orderByDesc(SupportTicketEntity::getUpdatedAt);
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(SupportTicketEntity::getStatus, status.trim());
        }
        Page<SupportTicketEntity> page1 = supportTicketMapper.selectPage(new Page<>(safePage(page), safeSize(size)), wrapper);
        // Enrich with username
        Page<Map<String, Object>> result = new Page<>(page1.getCurrent(), page1.getSize(), page1.getTotal());
        result.setRecords(page1.getRecords().stream().map(t -> enrichTicket(t)).collect(Collectors.toList()));
        return result;
    }

    @Override
    public Object detail(Long userId, String role, Long ticketId) {
        SupportTicketEntity ticket = getAllowedTicket(userId, role, ticketId);
        return buildDetail(ticket, userId, role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object reply(Long userId, String role, Long ticketId, SupportMessageDto dto) {
        SupportTicketEntity ticket = getAllowedTicket(userId, role, ticketId);
        if ("Closed".equals(ticket.getStatus())) {
            throw new RuntimeException("工单已关闭，不能继续回复");
        }
        if (dto == null || dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new RuntimeException("回复内容不能为空");
        }
        String senderRole = "Admin".equals(role) ? "Admin" : ("ShopOwner".equals(role) ? "ShopOwner" : "Customer");
        appendMessage(ticket, userId, senderRole, dto.getContent());
        ticket.setStatus("Admin".equals(senderRole) || "ShopOwner".equals(senderRole) ? "Replied" : "Open");
        ticket.setLastMessage(dto.getContent());
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketMapper.updateById(ticket);
        return buildDetail(ticket, userId, role);
    }

    @Override
    public Object close(Long userId, String role, Long ticketId) {
        SupportTicketEntity ticket = getAllowedTicket(userId, role, ticketId);
        ticket.setStatus("Closed");
        ticket.setUpdatedAt(LocalDateTime.now());
        supportTicketMapper.updateById(ticket);
        return enrichTicket(ticket);
    }

    @Override
    public Object adminList(Integer page, Integer size, String keyword, String status, String priority) {
        LambdaQueryWrapper<SupportTicketEntity> wrapper = new LambdaQueryWrapper<SupportTicketEntity>()
                .orderByDesc(SupportTicketEntity::getUpdatedAt);
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(SupportTicketEntity::getStatus, status.trim());
        }
        if (priority != null && !priority.trim().isEmpty()) {
            wrapper.eq(SupportTicketEntity::getPriority, priority.trim());
        }
        // keyword matches subject or ticketNo
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(SupportTicketEntity::getSubject, keyword.trim())
                              .or().like(SupportTicketEntity::getTicketNo, keyword.trim()));
        }
        Page<SupportTicketEntity> page1 = supportTicketMapper.selectPage(new Page<>(safePage(page), safeSize(size)), wrapper);
        Page<Map<String, Object>> result = new Page<>(page1.getCurrent(), page1.getSize(), page1.getTotal());
        result.setRecords(page1.getRecords().stream().map(this::enrichTicket).collect(Collectors.toList()));
        return result;
    }

    @Override
    public Object shopOwnerList(Long ownerShopId, Integer page, Integer size, String keyword, String status) {
        // 查询该店铺的所有下单用户 ID
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrderEntity> orderWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        orderWrapper.eq("shop_id", ownerShopId).select("DISTINCT customer_id");
        List<Long> shopCustomerIds = orderMapper.selectList(orderWrapper)
                .stream()
                .filter(java.util.Objects::nonNull)
                .map(OrderEntity::getCustomerId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (shopCustomerIds.isEmpty()) {
            // 该店铺还没有订单，直接返回空列表
            Page<Map<String, Object>> empty = new Page<>(safePage(page), safeSize(size), 0);
            empty.setRecords(java.util.Collections.emptyList());
            return empty;
        }

        LambdaQueryWrapper<SupportTicketEntity> wrapper = new LambdaQueryWrapper<SupportTicketEntity>()
                .in(SupportTicketEntity::getUserId, shopCustomerIds)
                .orderByDesc(SupportTicketEntity::getUpdatedAt);
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq(SupportTicketEntity::getStatus, status.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(SupportTicketEntity::getSubject, keyword.trim())
                              .or().like(SupportTicketEntity::getTicketNo, keyword.trim()));
        }
        Page<SupportTicketEntity> page1 = supportTicketMapper.selectPage(new Page<>(safePage(page), safeSize(size)), wrapper);
        Page<Map<String, Object>> result = new Page<>(page1.getCurrent(), page1.getSize(), page1.getTotal());
        result.setRecords(page1.getRecords().stream().map(this::enrichTicket).collect(Collectors.toList()));
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Build a full ticket detail map with 'replies' list for the frontend.
     * Frontend expects: { id, ticketNo, subject, username, userId, priority, status,
     *                     content, createdAt, updatedAt, replies:[{content,isAdmin,createTime}] }
     */
    private Map<String, Object> buildDetail(SupportTicketEntity ticket, Long userId, String role) {
        List<SupportMessageEntity> messages = supportMessageMapper.selectList(
                new LambdaQueryWrapper<SupportMessageEntity>()
                        .eq(SupportMessageEntity::getTicketId, ticket.getId())
                        .orderByAsc(SupportMessageEntity::getCreatedAt));

        // First customer message is the "original content" field only (not duplicated in replies)
        String originalContent = messages.stream()
                .filter(m -> "Customer".equals(m.getSenderRole()))
                .map(SupportMessageEntity::getContent)
                .findFirst().orElse("");

        // Find the ID of the very first message so we can exclude it from replies
        // (it is already exposed via the 'content' field; including it again causes duplicates)
        Long firstMsgId = messages.isEmpty() ? null : messages.get(0).getId();

        List<Map<String, Object>> replies = messages.stream()
                // Skip the first message — it's shown separately as "content" in the original design.
                // Both admin UI and app UI now show it inside the chat via the first list item, so we
                // include ALL messages here (no skipping) and the admin drawer no longer shows a
                // separate "original content" section.
                .map(m -> {
                    boolean isAdmin = "Admin".equals(m.getSenderRole()) || "ShopOwner".equals(m.getSenderRole());
                    Map<String, Object> reply = new LinkedHashMap<>();
                    reply.put("content", m.getContent());
                    reply.put("isAdmin", isAdmin);
                    reply.put("createTime", m.getCreatedAt() == null ? "" : m.getCreatedAt().format(DT_FMT));
                    User sender = m.getSenderId() == null ? null : userMapper.getUserById(m.getSenderId());
                    reply.put("username", isAdmin ? "客服" : (sender == null ? "用户" : notBlank(sender.getNick_name(), sender.getUsername())));
                    return reply;
                }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", ticket.getId());
        result.put("ticketNo", ticket.getTicketNo());
        result.put("subject", ticket.getSubject());
        result.put("userId", ticket.getUserId());
        User user = ticket.getUserId() == null ? null : userMapper.getUserById(ticket.getUserId());
        result.put("username", user == null ? String.valueOf(ticket.getUserId()) : notBlank(user.getNick_name(), user.getUsername()));
        result.put("priority", ticket.getPriority());
        result.put("status", ticket.getStatus());
        result.put("content", originalContent);
        result.put("message", originalContent);
        result.put("createdAt", ticket.getCreatedAt() == null ? "" : ticket.getCreatedAt().format(DT_FMT));
        result.put("updatedAt", ticket.getUpdatedAt() == null ? "" : ticket.getUpdatedAt().format(DT_FMT));
        result.put("replies", replies);
        return result;
    }

    /** Enrich a ticket entity with username for list views */
    private Map<String, Object> enrichTicket(SupportTicketEntity ticket) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", ticket.getId());
        map.put("ticketNo", ticket.getTicketNo());
        map.put("subject", ticket.getSubject());
        map.put("userId", ticket.getUserId());
        User user = ticket.getUserId() == null ? null : userMapper.getUserById(ticket.getUserId());
        map.put("username", user == null ? String.valueOf(ticket.getUserId()) : notBlank(user.getNick_name(), user.getUsername()));
        map.put("priority", ticket.getPriority());
        map.put("status", ticket.getStatus());
        map.put("lastMessage", ticket.getLastMessage());
        map.put("createdAt", ticket.getCreatedAt() == null ? "" : ticket.getCreatedAt().format(DT_FMT));
        map.put("updatedAt", ticket.getUpdatedAt() == null ? "" : ticket.getUpdatedAt().format(DT_FMT));
        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, String role, Long ticketId) {
        SupportTicketEntity ticket = supportTicketMapper.selectById(ticketId);
        if (ticket == null) throw new RuntimeException("工单不存在");
        if (!"Admin".equals(role) && !"ShopOwner".equals(role)) {
            throw new RuntimeException("仅管理员可删除工单");
        }
        // 删除工单消息
        supportMessageMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SupportMessageEntity>()
                .eq(SupportMessageEntity::getTicketId, ticketId));
        // 删除工单
        supportTicketMapper.deleteById(ticketId);
    }

    private SupportTicketEntity getAllowedTicket(Long userId, String role, Long ticketId) {
        SupportTicketEntity ticket = supportTicketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("工单不存在");
        }
        if (!"Admin".equals(role) && !"ShopOwner".equals(role) && !ticket.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该工单");
        }
        return ticket;
    }

    private void appendMessage(SupportTicketEntity ticket, Long senderId, String senderRole, String content) {
        SupportMessageEntity message = new SupportMessageEntity();
        message.setId(idWorker.nextId());
        message.setTicketId(ticket.getId());
        message.setSenderId(senderId);
        message.setSenderRole(senderRole);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        supportMessageMapper.insert(message);
    }

    private String notBlank(String first, String second) {
        return (first != null && !first.trim().isEmpty()) ? first : second;
    }

    private long safePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long safeSize(Integer size) {
        return size == null || size < 1 ? 20 : Math.min(size, 100);
    }
}
