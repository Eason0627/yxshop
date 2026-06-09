package com.yxshop.Module.AfterSales.Controller;

import com.yxshop.Module.AfterSales.Dto.AfterSalesApplyDto;
import com.yxshop.Module.AfterSales.Dto.AfterSalesQueryDto;
import com.yxshop.Module.AfterSales.Dto.AfterSalesReviewDto;
import com.yxshop.Module.AfterSales.Service.AfterSalesService;
import com.yxshop.Utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/app/after-sales")
@Tag(name = "AfterSales", description = "退款、退货退款、换货申请和审核接口")
public class AfterSalesModuleController {
    private final AfterSalesService afterSalesService;

    public AfterSalesModuleController(AfterSalesService afterSalesService) {
        this.afterSalesService = afterSalesService;
    }

    @PostMapping
    @Operation(summary = "申请售后")
    public Result apply(HttpServletRequest request, @RequestBody AfterSalesApplyDto dto) {
        return Result.success(afterSalesService.apply(currentUserId(request), dto));
    }

    @PostMapping("/list")
    @Operation(summary = "售后列表")
    public Result list(HttpServletRequest request, @RequestBody(required = false) AfterSalesQueryDto queryDto) {
        return Result.success(afterSalesService.list(currentUserId(request), currentUserRole(request), queryDto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "售后详情")
    public Result detail(HttpServletRequest request, @PathVariable Long id) {
        return Result.success(afterSalesService.detail(currentUserId(request), currentUserRole(request), id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消售后申请")
    public Result cancel(HttpServletRequest request, @PathVariable Long id) {
        return Result.success(afterSalesService.cancel(currentUserId(request), id));
    }

    @PostMapping("/review")
    @Operation(summary = "审核售后申请")
    public Result review(HttpServletRequest request, @RequestBody AfterSalesReviewDto dto) {
        return Result.success(afterSalesService.review(currentUserId(request), currentUserRole(request), dto));
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
