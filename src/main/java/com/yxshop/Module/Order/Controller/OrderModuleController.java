package com.yxshop.Module.Order.Controller;

import com.yxshop.Module.Order.Dto.OrderCreateDto;
import com.yxshop.Module.Order.Dto.OrderQueryDto;
import com.yxshop.Module.Order.Service.OrderModuleService;
import com.yxshop.Utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/app/orders")
@Tag(name = "Order", description = "下单、订单列表、详情和状态流转接口")
public class OrderModuleController {
    private final OrderModuleService orderModuleService;

    public OrderModuleController(OrderModuleService orderModuleService) {
        this.orderModuleService = orderModuleService;
    }

    @PostMapping
    @Operation(summary = "创建订单并预占库存")
    public Result create(HttpServletRequest request, @RequestBody OrderCreateDto dto) {
        return Result.success(orderModuleService.createOrder(currentUserId(request), dto));
    }

    @PostMapping("/list")
    @Operation(summary = "订单列表")
    public Result list(HttpServletRequest request, @RequestBody(required = false) OrderQueryDto queryDto) {
        return Result.success(orderModuleService.listOrders(currentUserId(request), currentUserRole(request), queryDto));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "订单详情")
    public Result detail(HttpServletRequest request, @PathVariable Long orderId) {
        return Result.success(orderModuleService.getOrderDetail(currentUserId(request), currentUserRole(request), orderId));
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "取消订单并释放预占库存")
    public Result cancel(HttpServletRequest request, @PathVariable Long orderId, @RequestParam(required = false) String reason) {
        return Result.success(orderModuleService.cancelOrder(currentUserId(request), currentUserRole(request), orderId, reason));
    }

    @PutMapping("/{orderId}/confirm-receive")
    @Operation(summary = "确认收货")
    public Result confirmReceive(HttpServletRequest request, @PathVariable Long orderId) {
        return Result.success(orderModuleService.confirmReceive(currentUserId(request), orderId));
    }

    @PostMapping("/{orderId}/remind-shipment")
    @Operation(summary = "提醒商家发货")
    public Result remindShipment(HttpServletRequest request, @PathVariable Long orderId) {
        return Result.success(orderModuleService.remindShipment(currentUserId(request), orderId));
    }

    @PutMapping("/{orderId}/ship")
    @Operation(summary = "订单发货（填写快递信息）")
    public Result ship(HttpServletRequest request, @PathVariable Long orderId,
                       @RequestBody Map<String, String> body) {
        String company = body.getOrDefault("company", "");
        String trackingNo = body.getOrDefault("trackingNo", "");
        String remark = body.get("remark");
        return Result.success(orderModuleService.shipOrder(
                currentUserId(request), currentUserRole(request), orderId, company, trackingNo, remark));
    }

    @PostMapping("/{orderId}/arbitrate")
    @Operation(summary = "平台仲裁（Admin 专属）")
    public Result arbitrate(HttpServletRequest request, @PathVariable Long orderId,
                            @RequestBody Map<String, String> body) {
        if (!"Admin".equals(currentUserRole(request))) {
            return Result.error("无权操作，仅平台管理员可仲裁");
        }
        String remark = body.get("remark");
        return Result.success(orderModuleService.arbitrate(currentUserId(request), orderId, remark));
    }

    @PutMapping("/{orderId}/after-sales")
    @Operation(summary = "更新售后状态（管理员/店主处理售后申请）")
    public Result updateAfterSales(HttpServletRequest request, @PathVariable Long orderId,
                                   @RequestBody Map<String, String> body) {
        String afterSalesStatus = body.get("afterSalesStatus");
        String remark = body.get("remark");
        return Result.success(orderModuleService.updateAfterSalesStatus(
                currentUserId(request), currentUserRole(request), orderId, afterSalesStatus, remark));
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "删除订单（软删除，仅限已取消/已完成）")
    public Result deleteOrder(HttpServletRequest request, @PathVariable Long orderId) {
        orderModuleService.deleteOrder(currentUserId(request), currentUserRole(request), orderId);
        return Result.success(null);
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
