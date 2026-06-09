package com.yxshop.Module.Refund.Controller;

import com.yxshop.Module.Refund.Dto.RefundCallbackDto;
import com.yxshop.Module.Refund.Service.RefundService;
import com.yxshop.Utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/refunds")
public class RefundController {
    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping("/{refundId}")
    public Result detail(@RequestAttribute("currentUserId") Object currentUserId,
                         @RequestAttribute("currentUserRole") Object currentUserRole,
                         @PathVariable Long refundId) {
        return Result.success(refundService.detail(Long.parseLong(currentUserId.toString()), String.valueOf(currentUserRole), refundId));
    }

    @PostMapping("/{refundId}/simulate-success")
    public Result simulateSuccess(@RequestAttribute("currentUserId") Object currentUserId,
                                  @RequestAttribute("currentUserRole") Object currentUserRole,
                                  @PathVariable Long refundId) {
        return Result.success(refundService.simulateSuccess(Long.parseLong(currentUserId.toString()), String.valueOf(currentUserRole), refundId));
    }

    @PostMapping("/callback")
    public Result callback(@RequestBody RefundCallbackDto dto) {
        return Result.success(refundService.callback(dto));
    }
}
