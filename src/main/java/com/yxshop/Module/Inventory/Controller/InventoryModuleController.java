package com.yxshop.Module.Inventory.Controller;

import com.yxshop.Module.Inventory.Dto.InventoryQueryDto;
import com.yxshop.Module.Inventory.Dto.StockAdjustDto;
import com.yxshop.Module.Inventory.Dto.StockCheckDto;
import com.yxshop.Module.Inventory.Service.InventoryModuleService;
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
@RequestMapping("/app/inventory")
@Tag(name = "Inventory", description = "库存查询、调整、预警和下单前库存校验接口")
public class InventoryModuleController {
    private final InventoryModuleService inventoryModuleService;

    public InventoryModuleController(InventoryModuleService inventoryModuleService) {
        this.inventoryModuleService = inventoryModuleService;
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "按商品查询库存")
    public Result productStock(@PathVariable Long productId) {
        return Result.success(inventoryModuleService.getByProductId(productId));
    }

    @PostMapping("/check")
    @Operation(summary = "下单前库存校验")
    public Result check(@RequestBody StockCheckDto checkDto) {
        return Result.success(inventoryModuleService.checkStock(checkDto));
    }

    @PostMapping("/list")
    @Operation(summary = "后台/商家库存列表")
    public Result list(HttpServletRequest request, @RequestBody(required = false) InventoryQueryDto queryDto) {
        return Result.success(inventoryModuleService.listInventory(currentUserId(request), currentUserRole(request), queryDto));
    }

    @PostMapping("/warnings")
    @Operation(summary = "安全库存预警")
    public Result warnings(HttpServletRequest request, @RequestBody(required = false) InventoryQueryDto queryDto) {
        return Result.success(inventoryModuleService.listWarnings(currentUserId(request), currentUserRole(request), queryDto));
    }

    @PostMapping("/adjust")
    @Operation(summary = "库存调整")
    public Result adjust(HttpServletRequest request, @RequestBody StockAdjustDto adjustDto) {
        return Result.success(inventoryModuleService.adjustStock(currentUserId(request), currentUserRole(request), adjustDto));
    }

    @PostMapping("/reserve")
    @Operation(summary = "下单预占库存")
    public Result reserve(HttpServletRequest request, @RequestBody StockAdjustDto adjustDto) {
        return Result.success(inventoryModuleService.reserveStock(currentUserId(request), currentUserRole(request), adjustDto));
    }

    @PostMapping("/deduct-reserved")
    @Operation(summary = "支付成功扣减预占库存")
    public Result deductReserved(HttpServletRequest request, @RequestBody StockAdjustDto adjustDto) {
        return Result.success(inventoryModuleService.deductReservedStock(currentUserId(request), currentUserRole(request), adjustDto));
    }

    @PostMapping("/release-reserved")
    @Operation(summary = "取消或超时释放预占库存")
    public Result releaseReserved(HttpServletRequest request, @RequestBody StockAdjustDto adjustDto) {
        return Result.success(inventoryModuleService.releaseReservedStock(currentUserId(request), currentUserRole(request), adjustDto));
    }

    @PostMapping("/records")
    @Operation(summary = "库存流水")
    public Result records(HttpServletRequest request, @RequestBody(required = false) InventoryQueryDto queryDto) {
        return Result.success(inventoryModuleService.listRecords(currentUserId(request), currentUserRole(request), queryDto));
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
