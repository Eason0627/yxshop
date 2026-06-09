package com.yxshop.Module.Fulfillment.Controller;

import com.yxshop.Module.Fulfillment.Dto.ShipOrderDto;
import com.yxshop.Module.Fulfillment.Dto.TraceDto;
import com.yxshop.Module.Fulfillment.Service.FulfillmentService;
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
@RequestMapping("/app/fulfillment")
@Tag(name = "Fulfillment", description = "发货和物流轨迹接口")
public class FulfillmentController {
    private final FulfillmentService fulfillmentService;

    public FulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @PostMapping("/ship")
    @Operation(summary = "商家发货")
    public Result ship(HttpServletRequest request, @RequestBody ShipOrderDto dto) {
        return Result.success(fulfillmentService.ship(currentUserId(request), currentUserRole(request), dto));
    }

    @PostMapping("/traces")
    @Operation(summary = "追加物流轨迹")
    public Result trace(HttpServletRequest request, @RequestBody TraceDto dto) {
        return Result.success(fulfillmentService.addTrace(currentUserId(request), currentUserRole(request), dto));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "按订单查询物流")
    public Result byOrder(HttpServletRequest request, @PathVariable Long orderId) {
        return Result.success(fulfillmentService.getByOrder(currentUserId(request), currentUserRole(request), orderId));
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
