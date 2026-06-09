package com.yxshop.Module.Shop.Controller;

import com.yxshop.Module.Shop.Dto.ShopDecorationDto;
import com.yxshop.Module.Shop.Dto.ShopDto;
import com.yxshop.Module.Shop.Dto.ShopQueryDto;
import com.yxshop.Module.Shop.Dto.ShopReviewDto;
import com.yxshop.Module.Shop.Service.ShopModuleService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/app/shops")
@Tag(name = "Shop", description = "店铺展示、开店申请和审核接口")
public class ShopModuleController {
    private final ShopModuleService shopModuleService;

    public ShopModuleController(ShopModuleService shopModuleService) {
        this.shopModuleService = shopModuleService;
    }

    @GetMapping("/{shopId}")
    @Operation(summary = "获取店铺详情")
    public Result detail(@PathVariable Long shopId) {
        return Result.success(shopModuleService.getShopDetail(shopId));
    }

    @GetMapping("/detail/{shopId}")
    @Operation(summary = "公开店铺详情")
    public Result publicDetail(@PathVariable Long shopId) {
        return Result.success(shopModuleService.getShopDetail(shopId));
    }

    @GetMapping("/{shopId}/home")
    @Operation(summary = "店铺首页聚合")
    public Result home(@PathVariable Long shopId) {
        return Result.success(shopModuleService.getShopHome(shopId));
    }

    @PostMapping("/public")
    @Operation(summary = "App店铺列表")
    public Result publicList(@RequestBody(required = false) ShopQueryDto queryDto) {
        return Result.success(shopModuleService.listPublicShops(queryDto));
    }

    @GetMapping("/me")
    @Operation(summary = "当前用户店铺")
    public Result myShops(HttpServletRequest request) {
        return Result.success(shopModuleService.listMyShops(currentUserId(request)));
    }

    @PostMapping("/apply")
    @Operation(summary = "提交开店申请")
    public Result apply(HttpServletRequest request, @RequestBody ShopDto shopDto) {
        return Result.success(shopModuleService.applyShop(currentUserId(request), shopDto));
    }

    @PutMapping("/{shopId}")
    @Operation(summary = "商家修改自己的店铺")
    public Result update(HttpServletRequest request, @PathVariable Long shopId, @RequestBody ShopDto shopDto) {
        return Result.success(shopModuleService.updateMyShop(currentUserId(request), shopId, shopDto));
    }

    @PutMapping("/{shopId}/decoration")
    @Operation(summary = "保存店铺装修")
    public Result decoration(HttpServletRequest request, @PathVariable Long shopId, @RequestBody ShopDecorationDto decorationDto) {
        return Result.success(shopModuleService.saveDecoration(currentUserId(request), currentUserRole(request), shopId, decorationDto));
    }

    @DeleteMapping("/{shopId}")
    @Operation(summary = "注销/停用店铺")
    public Result disable(HttpServletRequest request, @PathVariable Long shopId) {
        shopModuleService.disableShop(currentUserId(request), currentUserRole(request), shopId);
        return Result.success("店铺已停用");
    }

    @PostMapping("/admin/list")
    @Operation(summary = "后台店铺列表")
    public Result adminList(@RequestBody(required = false) ShopQueryDto queryDto) {
        return Result.success(shopModuleService.listAdminShops(queryDto));
    }

    @PutMapping("/{shopId}/status")
    @Operation(summary = "后台更新店铺状态（Active/Inactive）")
    public Result updateStatus(@PathVariable Long shopId, @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "Active");
        return Result.success(shopModuleService.updateShopStatus(shopId, status));
    }

    @PostMapping("/admin/update/{shopId}")
    @Operation(summary = "后台管理员修改任意店铺信息")
    public Result adminUpdate(@PathVariable Long shopId, @RequestBody ShopDto shopDto) {
        return Result.success(shopModuleService.adminUpdateShop(shopId, shopDto));
    }

    @PostMapping("/admin/review")
    @Operation(summary = "后台审核开店申请")
    public Result review(HttpServletRequest request, @RequestBody ShopReviewDto reviewDto) {
        if (!"Admin".equals(currentUserRole(request))) {
            throw new IllegalArgumentException("仅管理员可审核店铺");
        }
        shopModuleService.reviewShop(currentUserId(request), reviewDto);
        return Result.success("审核完成");
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
