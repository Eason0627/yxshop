package com.yxshop.Module.Message.Service;

import com.yxshop.Module.Message.Dto.SupportMessageDto;
import com.yxshop.Module.Message.Dto.SupportTicketDto;

public interface SupportTicketService {
    Object create(Long userId, SupportTicketDto dto);

    Object myTickets(Long userId, Integer page, Integer size, String status);

    Object detail(Long userId, String role, Long ticketId);

    Object reply(Long userId, String role, Long ticketId, SupportMessageDto dto);

    Object close(Long userId, String role, Long ticketId);

    /** Admin list — all tickets, with optional keyword/status/priority filters */
    Object adminList(Integer page, Integer size, String keyword, String status, String priority);

    /** ShopOwner list — only tickets from customers who ordered at this shop */
    Object shopOwnerList(Long ownerShopId, Integer page, Integer size, String keyword, String status);

    /** Admin delete a ticket and all its messages (hard delete, admin only) */
    void delete(Long userId, String role, Long ticketId);
}
