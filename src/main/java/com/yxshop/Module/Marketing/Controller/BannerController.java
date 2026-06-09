package com.yxshop.Module.Marketing.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yxshop.Module.Marketing.Dto.ActivityDto;
import com.yxshop.Module.Marketing.Dto.BannerDto;
import com.yxshop.Module.Marketing.Dto.ChannelDto;
import com.yxshop.Module.Marketing.Dto.CouponTemplateDto;
import com.yxshop.Module.Marketing.Dto.MarketingQueryDto;
import com.yxshop.Module.Marketing.Dto.PromotionCalculateDto;
import com.yxshop.Module.Marketing.Service.BannerService;
import com.yxshop.Module.Marketing.Service.PromotionCalculationService;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/app/marketing")
@Tag(name = "Marketing", description = "首页配置、活动、优惠券和榜单接口")
public class BannerController {

    private final BannerService bannerService;
    private final PromotionCalculationService promotionCalculationService;
    private final ShopModuleMapper shopMapper;

    public BannerController(BannerService bannerService, PromotionCalculationService promotionCalculationService,
                            ShopModuleMapper shopMapper) {
        this.bannerService = bannerService;
        this.promotionCalculationService = promotionCalculationService;
        this.shopMapper = shopMapper;
    }

    @GetMapping("/home")
    @Operation(summary = "首页营销配置")
    public Result home() {
        return Result.success(bannerService.homeConfig());
    }

    @GetMapping("/banners")
    @Operation(summary = "Banner列表")
    public Result banners() {
        return Result.success(bannerService.listPublicBanners());
    }

    @GetMapping("/channels")
    @Operation(summary = "频道列表")
    public Result channels() {
        return Result.success(bannerService.listChannels());
    }

    @PostMapping("/activities")
    @Operation(summary = "活动列表")
    public Result activities(@RequestBody(required = false) MarketingQueryDto queryDto) {
        return Result.success(bannerService.listActivities(queryDto));
    }

    @PostMapping("/coupons")
    @Operation(summary = "领券中心")
    public Result coupons(@RequestBody(required = false) MarketingQueryDto queryDto) {
        return Result.success(bannerService.listCouponTemplates(queryDto));
    }

    @PostMapping("/coupons/{templateId}/receive")
    @Operation(summary = "领取优惠券")
    public Result receiveCoupon(HttpServletRequest request, @PathVariable Long templateId) {
        bannerService.receiveCoupon(currentUserId(request), templateId);
        return Result.success("领取成功");
    }

    @GetMapping("/my-coupons")
    @Operation(summary = "我的优惠券")
    public Result myCoupons(HttpServletRequest request) {
        return Result.success(bannerService.listMyCoupons(currentUserId(request)));
    }

    @GetMapping("/rankings")
    @Operation(summary = "榜单")
    public Result rankings() {
        return Result.success(bannerService.listRankings());
    }

    @PostMapping("/calculate")
    @Operation(summary = "营销优惠计算")
    public Result calculate(HttpServletRequest request, @RequestBody PromotionCalculateDto dto) {
        if (dto == null) {
            dto = new PromotionCalculateDto();
        }
        Object userId = request.getAttribute("currentUserId");
        if (userId != null && dto.getUserId() == null) {
            dto.setUserId(Long.valueOf(String.valueOf(userId)));
        }
        return Result.success(promotionCalculationService.calculate(dto));
    }

    @PostMapping("/admin/banners")
    @Operation(summary = "保存Banner")
    public Result saveBanner(HttpServletRequest request, @RequestBody BannerDto dto) {
        assertAdmin(request);
        return Result.success(bannerService.saveBanner(dto));
    }

    @PostMapping("/admin/channels")
    @Operation(summary = "保存频道")
    public Result saveChannel(HttpServletRequest request, @RequestBody ChannelDto dto) {
        assertAdmin(request);
        return Result.success(bannerService.saveChannel(dto));
    }

    @PostMapping("/admin/activities")
    @Operation(summary = "保存活动（Admin 可创建平台活动，ShopOwner 只能创建/编辑本店活动）")
    public Result saveActivity(HttpServletRequest request, @RequestBody ActivityDto dto) {
        assertAdminOrShopOwner(request);
        if ("ShopOwner".equals(currentRole(request))) {
            Long ownerShopId = resolveOwnerShopId(currentUserId(request));
            if (dto.getId() != null) {
                // 编辑时：校验该活动是否属于本店
                bannerService.assertActivityOwnership(dto.getId(), ownerShopId);
            }
            dto.setShopId(ownerShopId);
        }
        return Result.success(bannerService.saveActivity(dto));
    }

    @PostMapping("/admin/coupons")
    @Operation(summary = "保存优惠券模板（Admin 或 ShopOwner）")
    public Result saveCoupon(HttpServletRequest request, @RequestBody CouponTemplateDto dto) {
        assertAdminOrShopOwner(request);
        if ("ShopOwner".equals(currentRole(request))) {
            Long ownerShopId = resolveOwnerShopId(currentUserId(request));
            if (dto.getId() != null) {
                // 编辑时：校验该优惠券是否属于本店
                bannerService.assertCouponOwnership(dto.getId(), ownerShopId);
            }
            dto.setShopId(ownerShopId);
        }
        return Result.success(bannerService.saveCouponTemplate(dto));
    }

    @PutMapping("/admin/{targetType}/{id}/status")
    @Operation(summary = "更新营销对象状态（Admin 全类型；ShopOwner 仅限本店 activities/coupons）")
    public Result updateStatus(HttpServletRequest request,
                               @PathVariable String targetType,
                               @PathVariable Long id,
                               @RequestBody(required = false) Map<String, Object> body,
                               @RequestParam(required = false) Integer status) {
        // Accept status from JSON body OR query param
        Integer finalStatus = status;
        if (finalStatus == null && body != null && body.get("status") != null) {
            finalStatus = Integer.valueOf(String.valueOf(body.get("status")));
        }
        if (finalStatus == null) throw new IllegalArgumentException("status 不能为空");

        String role = currentRole(request);
        if ("ShopOwner".equals(role)) {
            // ShopOwner 只能操作本店的活动/优惠券
            String t = targetType == null ? "" : targetType.toLowerCase().replaceAll("s$", "");
            if ("activity".equals(t) || "activitie".equals(t)) {
                bannerService.assertActivityOwnership(id, resolveOwnerShopId(currentUserId(request)));
            } else if ("coupon".equals(t)) {
                bannerService.assertCouponOwnership(id, resolveOwnerShopId(currentUserId(request)));
            } else {
                return Result.error("ShopOwner 无权修改该类型的营销对象状态");
            }
        } else if (!"Admin".equals(role)) {
            return Result.error("无权操作");
        }

        bannerService.updateStatus(targetType, id, finalStatus);
        return Result.success("状态已更新");
    }

    // ===== Admin-only endpoints =====

    @GetMapping("/admin/banners")
    @Operation(summary = "后台Banner全量列表")
    public Result adminBanners(HttpServletRequest request) {
        assertAdmin(request);
        return Result.success(bannerService.listAllBannersAdmin());
    }

    @PostMapping("/admin/activities/list")
    @Operation(summary = "后台活动分页列表（Admin 全量，ShopOwner 仅本店）")
    public Result adminActivities(HttpServletRequest request,
                                   @RequestBody(required = false) MarketingQueryDto queryDto) {
        assertAdminOrShopOwner(request);
        if ("ShopOwner".equals(currentRole(request))) {
            if (queryDto == null) queryDto = new MarketingQueryDto();
            queryDto.setShopId(resolveOwnerShopId(currentUserId(request)));
        }
        return Result.success(bannerService.listActivitiesAdmin(queryDto));
    }

    @PostMapping("/admin/coupons/list")
    @Operation(summary = "后台优惠券分页列表（Admin 全量，ShopOwner 仅本店）")
    public Result adminCoupons(HttpServletRequest request,
                                @RequestBody(required = false) MarketingQueryDto queryDto) {
        assertAdminOrShopOwner(request);
        if ("ShopOwner".equals(currentRole(request))) {
            if (queryDto == null) queryDto = new MarketingQueryDto();
            queryDto.setShopId(resolveOwnerShopId(currentUserId(request)));
        }
        return Result.success(bannerService.listCouponsAdmin(queryDto));
    }

    @GetMapping("/admin/ranking-lists")
    @Operation(summary = "后台榜单列表")
    public Result adminRankingLists(HttpServletRequest request) {
        assertAdmin(request);
        return Result.success(bannerService.listRankingLists());
    }

    @GetMapping("/admin/ranking-lists/{listId}/products")
    @Operation(summary = "后台榜单商品列表")
    public Result adminRankingProducts(HttpServletRequest request, @PathVariable Long listId) {
        assertAdmin(request);
        return Result.success(bannerService.getRankingProducts(listId));
    }

    @PostMapping("/admin/ranking-lists/{listId}/products/batch")
    @Operation(summary = "批量加入榜单")
    public Result adminAddRankingProducts(HttpServletRequest request, @PathVariable Long listId,
                                           @RequestBody Map<String, Object> body) {
        assertAdmin(request);
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) body.get("productIds");
        if (rawIds == null || rawIds.isEmpty()) throw new IllegalArgumentException("productIds 不能为空");
        List<Long> productIds = rawIds.stream().map(Number::longValue).collect(java.util.stream.Collectors.toList());
        bannerService.addRankingProducts(listId, productIds);
        return Result.success("已添加");
    }

    @DeleteMapping("/admin/ranking-lists/{listId}/products/{productId}")
    @Operation(summary = "从榜单移除商品")
    public Result adminRemoveRankingProduct(HttpServletRequest request,
                                             @PathVariable Long listId, @PathVariable Long productId) {
        assertAdmin(request);
        bannerService.removeRankingProduct(listId, productId);
        return Result.success("已移除");
    }

    @PostMapping("/admin/ranking-lists")
    @Operation(summary = "创建榜单")
    public Result adminCreateRankingList(HttpServletRequest request, @RequestBody Map<String, Object> data) {
        assertAdmin(request);
        return Result.success(bannerService.createRankingList(data));
    }

    @PutMapping("/admin/ranking-lists/{listId}")
    @Operation(summary = "更新榜单信息")
    public Result adminUpdateRankingList(HttpServletRequest request, @PathVariable Long listId,
                                          @RequestBody Map<String, Object> data) {
        assertAdmin(request);
        bannerService.updateRankingList(listId, data);
        return Result.success("已更新");
    }

    @DeleteMapping("/admin/ranking-lists/{listId}")
    @Operation(summary = "删除榜单")
    public Result adminDeleteRankingList(HttpServletRequest request, @PathVariable Long listId) {
        assertAdmin(request);
        bannerService.deleteRankingList(listId);
        return Result.success("已删除");
    }

    private Long currentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value == null) throw new IllegalArgumentException("请先登录");
        return Long.valueOf(String.valueOf(value));
    }

    private String currentRole(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserRole");
        return value == null ? "" : String.valueOf(value);
    }

    private void assertAdmin(HttpServletRequest request) {
        if (!"Admin".equals(currentRole(request))) {
            throw new IllegalArgumentException("仅管理员可操作");
        }
    }

    private void assertAdminOrShopOwner(HttpServletRequest request) {
        String role = currentRole(request);
        if (!"Admin".equals(role) && !"ShopOwner".equals(role)) {
            throw new IllegalArgumentException("无权操作");
        }
    }

    private Long resolveOwnerShopId(Long userId) {
        QueryWrapper<ShopEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("owner_user_id", userId).eq("status", "Active").last("LIMIT 1");
        ShopEntity shop = shopMapper.selectOne(wrapper);
        if (shop == null) throw new IllegalArgumentException("当前账号没有已激活的店铺");
        return shop.getShopId();
    }
}
