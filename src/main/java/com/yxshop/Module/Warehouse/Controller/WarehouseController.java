package com.yxshop.Module.Warehouse.Controller;

import com.yxshop.Module.Warehouse.Dto.WarehouseDto;
import com.yxshop.Module.Warehouse.Service.WarehouseService;
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

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/app/warehouses")
@Tag(name = "Warehouse", description = "发货仓库管理")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    @Operation(summary = "仓库列表（ShopOwner 返回本店；Admin 可传 shopId 过滤）")
    public Result list(HttpServletRequest request,
                       @RequestParam(required = false) Long shopId) {
        return Result.success(warehouseService.list(currentUserId(request), currentRole(request), shopId));
    }

    @PostMapping
    @Operation(summary = "新建或更新仓库（dto.id 非空时为更新）")
    public Result save(HttpServletRequest request, @RequestBody WarehouseDto dto) {
        return Result.success(warehouseService.saveWarehouse(currentUserId(request), currentRole(request), dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除仓库（软删除）")
    public Result delete(HttpServletRequest request, @PathVariable Long id) {
        warehouseService.delete(currentUserId(request), currentRole(request), id);
        return Result.success("已删除");
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设为默认发货仓库")
    public Result setDefault(HttpServletRequest request, @PathVariable Long id) {
        return Result.success(warehouseService.setDefault(currentUserId(request), currentRole(request), id));
    }

    private Long currentUserId(HttpServletRequest req) {
        Object v = req.getAttribute("currentUserId");
        if (v == null) throw new IllegalArgumentException("请先登录");
        return Long.valueOf(v.toString());
    }

    private String currentRole(HttpServletRequest req) {
        Object v = req.getAttribute("currentUserRole");
        return v == null ? "" : v.toString();
    }
}
