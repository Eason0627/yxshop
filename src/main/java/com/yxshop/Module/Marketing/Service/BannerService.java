package com.yxshop.Module.Marketing.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxshop.Module.Marketing.Dto.ActivityDto;
import com.yxshop.Module.Marketing.Dto.BannerDto;
import com.yxshop.Module.Marketing.Dto.ChannelDto;
import com.yxshop.Module.Marketing.Dto.CouponTemplateDto;
import com.yxshop.Module.Marketing.Dto.MarketingQueryDto;
import com.yxshop.Module.Marketing.Entity.BannerEntity;
import com.yxshop.Module.Marketing.Vo.BannerVo;

import java.util.List;
import java.util.Map;

public interface BannerService extends IService<BannerEntity> {
    Map<String, Object> homeConfig();
    List<BannerVo> listPublicBanners();
    Object listChannels();
    Object listActivities(MarketingQueryDto queryDto);
    Object listCouponTemplates(MarketingQueryDto queryDto);
    Object listRankings();
    void receiveCoupon(Long userId, Long templateId);
    Object listMyCoupons(Long userId);
    BannerVo saveBanner(BannerDto dto);
    Object saveChannel(ChannelDto dto);
    Object saveActivity(ActivityDto dto);
    Object saveCouponTemplate(CouponTemplateDto dto);
    void updateStatus(String targetType, Long id, Integer status);

    // ===== Admin-only =====
    /** All banners regardless of status/time (for admin management) */
    List<Map<String, Object>> listAllBannersAdmin();
    /** Paginated activities with all-status for admin */
    Map<String, Object> listActivitiesAdmin(MarketingQueryDto queryDto);
    /** Paginated coupon templates with all-status for admin */
    Map<String, Object> listCouponsAdmin(MarketingQueryDto queryDto);
    /** Ranking lists with enriched product info */
    List<Map<String, Object>> listRankingLists();
    /** Get products in a specific ranking list, enriched */
    List<Map<String, Object>> getRankingProducts(Long listId);
    /** Add products to ranking list */
    void addRankingProducts(Long listId, List<Long> productIds);
    /** Remove product from ranking list */
    void removeRankingProduct(Long listId, Long productId);
    /** Create a new ranking list */
    com.yxshop.Module.Marketing.Entity.RankingListEntity createRankingList(java.util.Map<String, Object> data);
    /** Update a ranking list's metadata */
    void updateRankingList(Long listId, java.util.Map<String, Object> data);
    /** Delete a ranking list entirely */
    void deleteRankingList(Long listId);

    /** 校验活动归属（ShopOwner 编辑时调用）*/
    void assertActivityOwnership(Long activityId, Long ownerShopId);

    /** 校验优惠券模板归属（ShopOwner 编辑时调用）*/
    void assertCouponOwnership(Long couponId, Long ownerShopId);
}
