package com.yxshop.Module.Notification.Controller;

import com.yxshop.Module.Notification.Dto.NotificationPushDto;
import com.yxshop.Module.Notification.Dto.NotificationTemplateDto;
import com.yxshop.Module.Notification.Service.NotificationService;
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

@RestController
@RequestMapping("/app/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result list(HttpServletRequest request) {
        return Result.success(notificationService.listMine(currentUserId(request)));
    }

    @GetMapping("/summary")
    public Result summary(HttpServletRequest request) {
        return Result.success(notificationService.summary(currentUserId(request)));
    }

    @PutMapping("/{id}/read")
    public Result read(HttpServletRequest request, @PathVariable Long id) {
        notificationService.markRead(currentUserId(request), id);
        return Result.success("已读");
    }

    @PutMapping("/read-all")
    public Result readAll(HttpServletRequest request) {
        notificationService.markAllRead(currentUserId(request));
        return Result.success("全部已读");
    }

    @GetMapping("/{id}")
    public Result getOne(HttpServletRequest request, @PathVariable Long id) {
        return Result.success(notificationService.getOne(currentUserId(request), id));
    }

    @DeleteMapping("/{id}")
    public Result delete(HttpServletRequest request, @PathVariable Long id) {
        notificationService.delete(currentUserId(request), id);
        return Result.success("已删除");
    }

    // ── Admin endpoints ────────────────────────────────────────────────────────

    @GetMapping("/admin/records")
    public Result adminList(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "20") Integer size,
                            @RequestParam(required = false) String bizType,
                            @RequestParam(required = false) Long userId) {
        return Result.success(notificationService.adminList(page, size, bizType, userId));
    }

    @PostMapping("/admin/push")
    public Result adminPush(HttpServletRequest request, @RequestBody NotificationPushDto dto) {
        return Result.success(notificationService.adminPush(currentUserId(request), dto));
    }

    @GetMapping("/admin/templates")
    public Result templates() {
        return Result.success(notificationService.listTemplates());
    }

    @PostMapping("/admin/templates")
    public Result saveTemplate(@RequestBody NotificationTemplateDto dto) {
        return Result.success(notificationService.saveTemplate(dto));
    }

    private Long currentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value == null) throw new IllegalArgumentException("请先登录");
        return Long.valueOf(String.valueOf(value));
    }
}
