package com.yxshop.Module.Payment.Controller;

import com.yxshop.Module.Payment.Dto.PaymentCallbackDto;
import com.yxshop.Module.Payment.Dto.PaymentCreateDto;
import com.yxshop.Module.Payment.Service.PaymentService;
import com.yxshop.Utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/app/payments")
@Tag(name = "Payment", description = "支付单、模拟支付和支付回调接口")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "创建支付单")
    public Result create(HttpServletRequest request, @RequestBody PaymentCreateDto dto) {
        return Result.success(paymentService.createPayment(currentUserId(request), dto));
    }

    @PostMapping("/{paymentId}/simulate-pay")
    @Operation(summary = "模拟支付成功")
    public Result simulatePay(HttpServletRequest request, @PathVariable Long paymentId) {
        return Result.success(paymentService.simulatePay(currentUserId(request), paymentId));
    }

    @PostMapping("/callback")
    @Operation(summary = "支付回调")
    public Result callback(@RequestBody PaymentCallbackDto dto) {
        return Result.success(paymentService.handleCallback(dto));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "支付单详情")
    public Result detail(HttpServletRequest request, @PathVariable Long paymentId) {
        return Result.success(paymentService.getPayment(currentUserId(request), currentUserRole(request), paymentId));
    }

    private Long currentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value == null) {
            throw new IllegalArgumentException("请先登录");
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String currentUserRole(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserRole");
        return value == null ? null : String.valueOf(value);
    }
}
