package com.yxshop.Module.Message.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yxshop.Module.Message.Dto.SupportMessageDto;
import com.yxshop.Module.Message.Dto.SupportTicketDto;
import com.yxshop.Module.Message.Service.SupportTicketService;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Utils.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/app/support")
public class SupportTicketController {
    private final SupportTicketService supportTicketService;
    private final ShopModuleMapper shopMapper;

    public SupportTicketController(SupportTicketService supportTicketService, ShopModuleMapper shopMapper) {
        this.supportTicketService = supportTicketService;
        this.shopMapper = shopMapper;
    }

    /** User creates a support ticket */
    @PostMapping("/tickets")
    public Result create(@RequestAttribute("currentUserId") Object currentUserId,
                         @RequestBody SupportTicketDto dto) {
        return Result.success(supportTicketService.create(Long.parseLong(currentUserId.toString()), dto));
    }

    /**
     * List tickets.
     * Admin (role=Admin) sees ALL tickets with keyword/status/priority filters.
     * Regular users see only their own tickets.
     */
    @GetMapping("/tickets")
    public Result listTickets(HttpServletRequest request,
                              @RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "20") Integer pageSize,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String priority) {
        Object roleAttr = request.getAttribute("currentUserRole");
        String role = roleAttr == null ? "Customer" : String.valueOf(roleAttr);
        Long userId = Long.parseLong(String.valueOf(request.getAttribute("currentUserId")));

        if ("Admin".equals(role)) {
            return Result.success(supportTicketService.adminList(pageNum, pageSize, keyword, status, priority));
        } else if ("ShopOwner".equals(role)) {
            // 只看本店客户的工单
            QueryWrapper<ShopEntity> shopWrapper = new QueryWrapper<>();
            shopWrapper.eq("owner_user_id", userId).eq("status", "Active").last("LIMIT 1");
            ShopEntity shop = shopMapper.selectOne(shopWrapper);
            Long ownerShopId = shop != null ? shop.getShopId() : null;
            if (ownerShopId == null) {
                return Result.success(supportTicketService.myTickets(userId, pageNum, pageSize, status));
            }
            return Result.success(supportTicketService.shopOwnerList(ownerShopId, pageNum, pageSize, keyword, status));
        } else {
            return Result.success(supportTicketService.myTickets(userId, pageNum, pageSize, status));
        }
    }

    /** Ticket detail — admin can access any ticket */
    @GetMapping("/tickets/{ticketId}")
    public Result detail(HttpServletRequest request,
                         @PathVariable Long ticketId) {
        Object roleAttr = request.getAttribute("currentUserRole");
        String role = roleAttr == null ? "Customer" : String.valueOf(roleAttr);
        Long userId = Long.parseLong(String.valueOf(request.getAttribute("currentUserId")));
        return Result.success(supportTicketService.detail(userId, role, ticketId));
    }

    /** Reply to a ticket (POST /tickets/{id}/reply — frontend admin path) */
    @PostMapping("/tickets/{ticketId}/reply")
    public Result replyAlias(HttpServletRequest request,
                             @PathVariable Long ticketId,
                             @RequestBody Map<String, Object> body) {
        Object roleAttr = request.getAttribute("currentUserRole");
        String role = roleAttr == null ? "Customer" : String.valueOf(roleAttr);
        Long userId = Long.parseLong(String.valueOf(request.getAttribute("currentUserId")));
        SupportMessageDto dto = new SupportMessageDto();
        Object content = body.get("content");
        dto.setContent(content == null ? "" : content.toString());
        return Result.success(supportTicketService.reply(userId, role, ticketId, dto));
    }

    /** Original reply endpoint (messages sub-path) */
    @PostMapping("/tickets/{ticketId}/messages")
    public Result reply(HttpServletRequest request,
                        @PathVariable Long ticketId,
                        @RequestBody SupportMessageDto dto) {
        Object roleAttr = request.getAttribute("currentUserRole");
        String role = roleAttr == null ? "Customer" : String.valueOf(roleAttr);
        Long userId = Long.parseLong(String.valueOf(request.getAttribute("currentUserId")));
        return Result.success(supportTicketService.reply(userId, role, ticketId, dto));
    }

    /** Close a ticket */
    @PutMapping("/tickets/{ticketId}/close")
    public Result close(HttpServletRequest request,
                        @PathVariable Long ticketId) {
        Object roleAttr = request.getAttribute("currentUserRole");
        String role = roleAttr == null ? "Customer" : String.valueOf(roleAttr);
        Long userId = Long.parseLong(String.valueOf(request.getAttribute("currentUserId")));
        return Result.success(supportTicketService.close(userId, role, ticketId));
    }

    /** Delete a ticket (admin only) */
    @DeleteMapping("/tickets/{ticketId}")
    public Result delete(HttpServletRequest request, @PathVariable Long ticketId) {
        Object roleAttr = request.getAttribute("currentUserRole");
        String role = roleAttr == null ? "Customer" : String.valueOf(roleAttr);
        Long userId = Long.parseLong(String.valueOf(request.getAttribute("currentUserId")));
        supportTicketService.delete(userId, role, ticketId);
        return Result.success("已删除");
    }

    /** Legacy admin list endpoint */
    @GetMapping("/admin/tickets")
    public Result adminList(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "20") Integer size,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String priority) {
        return Result.success(supportTicketService.adminList(page, size, keyword, status, priority));
    }
}
