package com.yxshop.Module.Marketing.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.Catalog.Entity.AppProductEntity;
import com.yxshop.Module.Catalog.Mapper.AppProductMapper;
import com.yxshop.Module.Marketing.Dto.ActivityDto;
import com.yxshop.Module.Marketing.Dto.BannerDto;
import com.yxshop.Module.Marketing.Dto.ChannelDto;
import com.yxshop.Module.Marketing.Dto.CouponTemplateDto;
import com.yxshop.Module.Marketing.Dto.MarketingQueryDto;
import com.yxshop.Module.Marketing.Entity.ActivityEntity;
import com.yxshop.Module.Marketing.Entity.BannerEntity;
import com.yxshop.Module.Marketing.Entity.ChannelEntity;
import com.yxshop.Module.Marketing.Entity.CouponTemplateEntity;
import com.yxshop.Module.Marketing.Entity.RankingListEntity;
import com.yxshop.Module.Marketing.Entity.UserCouponEntity;
import com.yxshop.Module.Marketing.Mapper.ActivityMapper;
import com.yxshop.Module.Marketing.Mapper.BannerMapper;
import com.yxshop.Module.Marketing.Mapper.ChannelMapper;
import com.yxshop.Module.Marketing.Mapper.CouponTemplateMapper;
import com.yxshop.Module.Marketing.Mapper.RankingListMapper;
import com.yxshop.Module.Marketing.Mapper.UserCouponMapper;
import com.yxshop.Module.Marketing.Service.BannerService;
import com.yxshop.Module.Marketing.Vo.BannerVo;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Utils.AliOSSUtils;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Primary
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, BannerEntity> implements BannerService {

    private final ChannelMapper channelMapper;
    private final ActivityMapper activityMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final RankingListMapper rankingListMapper;
    private final AppProductMapper appProductMapper;
    private final ShopModuleMapper shopModuleMapper;
    private final AliOSSUtils aliOSSUtils;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(6, 1);

    public BannerServiceImpl(ChannelMapper channelMapper,
                             ActivityMapper activityMapper,
                             CouponTemplateMapper couponTemplateMapper,
                             UserCouponMapper userCouponMapper,
                             RankingListMapper rankingListMapper,
                             AppProductMapper appProductMapper,
                             ShopModuleMapper shopModuleMapper,
                             AliOSSUtils aliOSSUtils) {
        this.channelMapper = channelMapper;
        this.activityMapper = activityMapper;
        this.couponTemplateMapper = couponTemplateMapper;
        this.userCouponMapper = userCouponMapper;
        this.rankingListMapper = rankingListMapper;
        this.appProductMapper = appProductMapper;
        this.shopModuleMapper = shopModuleMapper;
        this.aliOSSUtils = aliOSSUtils;
    }

    /** 若是 OSS 对象键则生成预签名 URL，否则原样返回 */
    private String signUrl(String url) {
        if (url == null || url.isBlank()) return url;
        if (AliOSSUtils.isObjectKey(url)) {
            String signed = aliOSSUtils.generatePresignedUrl(url, 120);
            return signed != null ? signed : url;
        }
        return url;
    }

    @Override
    public Map<String, Object> homeConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("banners", listPublicBanners());
        result.put("channels", listChannels());
        result.put("activities", listHomeActivities());
        result.put("rankings", listRankings());
        result.put("brandShops", listHomeBrandShops());
        
        // 瀑布流各类卡片
        result.put("podium", listHomePodium(3));           // 颁奖台 TOP3
        result.put("hotSaleProducts", listHomeProducts("hotSale", 6));
        result.put("editorPicks", listHomeProducts("editorPick", 4));  // 编辑精选
        result.put("brandPicks", listHomeBrandPickProducts(4));        // 品牌精选
        result.put("activityCards", listHomeActivityCards(3));          // 活动卡片
        result.put("recommendProducts", listHomeProducts("recommend", 50));
        return result;
    }

    @Override
    public List<BannerVo> listPublicBanners() {
        QueryWrapper<BannerEntity> wrapper = new QueryWrapper<>();
        LocalDateTime now = LocalDateTime.now();
        wrapper.eq("status", 1)
                .and(item -> item.isNull("start_at").or().le("start_at", now))
                .and(item -> item.isNull("end_at").or().ge("end_at", now))
                .orderByAsc("sort").orderByDesc("updated_at");
        return list(wrapper).stream().map(this::toBannerVo).collect(Collectors.toList());
    }

    @Override
    public Object listChannels() {
        QueryWrapper<ChannelEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByAsc("sort").orderByAsc("id");
        return channelMapper.selectList(wrapper);
    }

    @Override
    public Object listActivities(MarketingQueryDto queryDto) {
        MarketingQueryDto query = queryDto == null ? new MarketingQueryDto() : queryDto;
        QueryWrapper<ActivityEntity> wrapper = new QueryWrapper<>();
        wrapper.eq(query.getStatus() == null, "status", 1);
        if (query.getStatus() != null) wrapper.eq("status", query.getStatus());
        if (query.getShopId() != null) wrapper.eq("shop_id", query.getShopId());
        if (query.getType() != null) wrapper.eq("type", query.getType());
        if (query.getKeyword() != null && !query.getKeyword().trim().isEmpty()) wrapper.like("title", query.getKeyword());
        wrapper.orderByDesc("hot_percent").orderByDesc("updated_at");
        return activityMapper.selectList(wrapper);
    }

    @Override
    public Object listCouponTemplates(MarketingQueryDto queryDto) {
        MarketingQueryDto query = queryDto == null ? new MarketingQueryDto() : queryDto;
        QueryWrapper<CouponTemplateEntity> wrapper = new QueryWrapper<>();
        wrapper.eq(query.getStatus() == null, "status", 1);
        if (query.getStatus() != null) wrapper.eq("status", query.getStatus());
        if (query.getShopId() != null) wrapper.eq("shop_id", query.getShopId());
        if (query.getKeyword() != null && !query.getKeyword().trim().isEmpty()) wrapper.like("name", query.getKeyword());
        wrapper.gt("remaining_count", 0).orderByDesc("updated_at");
        return couponTemplateMapper.selectList(wrapper);
    }

    @Override
    public Object listRankings() {
        QueryWrapper<RankingListEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByDesc("update_time");
        return rankingListMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveCoupon(Long userId, Long templateId) {
        CouponTemplateEntity template = couponTemplateMapper.selectById(templateId);
        if (template == null || !Integer.valueOf(1).equals(template.getStatus())) {
            throw new IllegalArgumentException("优惠券不存在或已停用");
        }
        if (template.getRemainingCount() == null || template.getRemainingCount() <= 0) {
            throw new IllegalArgumentException("优惠券已领完");
        }
        QueryWrapper<UserCouponEntity> exists = new QueryWrapper<>();
        exists.eq("user_id", userId).eq("coupon_template_id", templateId).last("LIMIT 1");
        if (userCouponMapper.selectOne(exists) != null) {
            throw new IllegalArgumentException("不能重复领取该优惠券");
        }
        UserCouponEntity coupon = new UserCouponEntity();
        coupon.setId(idWorker.nextId());
        coupon.setUserId(userId);
        coupon.setCouponTemplateId(templateId);
        coupon.setName(template.getName());
        coupon.setType(template.getType());
        coupon.setLabel(template.getLabel());
        coupon.setMinAmount(template.getMinAmount());
        coupon.setValue(template.getValue());
        coupon.setDescription(template.getDescription());
        coupon.setStatus(1);
        coupon.setReceivedAt(LocalDateTime.now());
        coupon.setStartAt(template.getStartAt());
        coupon.setEndAt(template.getEndAt());
        coupon.setCreatedAt(LocalDateTime.now());
        coupon.setUpdatedAt(LocalDateTime.now());
        userCouponMapper.insert(coupon);

        UpdateWrapper<CouponTemplateEntity> update = new UpdateWrapper<>();
        update.eq("id", templateId).gt("remaining_count", 0).setSql("remaining_count = remaining_count - 1");
        couponTemplateMapper.update(null, update);
    }

    @Override
    public Object listMyCoupons(Long userId) {
        // 返回全部券，前端分 "可使用 / 已使用 / 已过期" 三个 Tab 展示
        QueryWrapper<UserCouponEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("received_at");
        return userCouponMapper.selectList(wrapper);
    }

    @Override
    public BannerVo saveBanner(BannerDto dto) {
        BannerEntity banner = dto.getId() == null ? new BannerEntity() : getById(dto.getId());
        if (banner == null) banner = new BannerEntity();
        if (banner.getId() == null) {
            banner.setId(idWorker.nextId());
            banner.setCreatedAt(LocalDateTime.now());
        }
        banner.setTitle(dto.getTitle());
        banner.setImage(dto.getImage());
        banner.setLinkType(dto.getLinkType());
        banner.setLinkTarget(dto.getLinkTarget());
        banner.setSort(dto.getSort() == null ? 0 : dto.getSort());
        banner.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        banner.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(banner);
        return toBannerVo(banner);
    }

    @Override
    public Object saveChannel(ChannelDto dto) {
        ChannelEntity channel = dto.getId() == null ? new ChannelEntity() : channelMapper.selectById(dto.getId());
        if (channel == null) channel = new ChannelEntity();
        if (channel.getId() == null) {
            channel.setId(idWorker.nextId());
            channel.setCreatedAt(LocalDateTime.now());
        }
        channel.setName(dto.getName());
        channel.setIcon(dto.getIcon());
        channel.setIconColor(dto.getIconColor());
        channel.setBgColor(dto.getBgColor());
        channel.setPath(dto.getPath());
        channel.setSort(dto.getSort() == null ? 0 : dto.getSort());
        channel.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        channel.setUpdatedAt(LocalDateTime.now());
        if (dto.getId() == null) channelMapper.insert(channel); else channelMapper.updateById(channel);
        return channel;
    }

    @Override
    public Object saveActivity(ActivityDto dto) {
        ActivityEntity activity = dto.getId() == null ? new ActivityEntity() : activityMapper.selectById(dto.getId());
        if (activity == null) activity = new ActivityEntity();
        if (activity.getId() == null) {
            activity.setId(idWorker.nextId());
            activity.setCreatedAt(LocalDateTime.now());
        }
        activity.setType(dto.getType() == null ? "platform" : dto.getType());
        activity.setTitle(dto.getTitle());
        activity.setSubtitle(dto.getSubtitle());
        activity.setTag(dto.getTag());
        activity.setTagColor(dto.getTagColor());
        activity.setTagBg(dto.getTagBg());
        activity.setImage(dto.getImage());
        activity.setDiscountText(dto.getDiscountText());
        activity.setShopId(dto.getShopId());
        activity.setScopeType(dto.getScopeType());
        activity.setProductIds(dto.getProductIds());
        activity.setProductSnapshot(dto.getProductSnapshot());
        activity.setStackable(dto.getStackable() == null ? 1 : dto.getStackable());
        activity.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        activity.setMaxDiscount(dto.getMaxDiscount());
        activity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        activity.setStartAt(parseDateTime(dto.getStartTime()));
        activity.setEndAt(parseDateTime(dto.getEndTime()));
        activity.setUpdatedAt(LocalDateTime.now());
        if (dto.getId() == null) activityMapper.insert(activity); else activityMapper.updateById(activity);
        return activity;
    }

    @Override
    public Object saveCouponTemplate(CouponTemplateDto dto) {
        CouponTemplateEntity coupon = dto.getId() == null ? new CouponTemplateEntity() : couponTemplateMapper.selectById(dto.getId());
        if (coupon == null) coupon = new CouponTemplateEntity();
        if (coupon.getId() == null) {
            coupon.setId(idWorker.nextId());
            coupon.setCreatedAt(LocalDateTime.now());
        }
        coupon.setName(dto.getName());
        coupon.setLabel(dto.getLabel());
        coupon.setType(dto.getType());
        coupon.setValue(dto.getValue());
        coupon.setMinAmount(dto.getMinAmount());
        coupon.setDescription(dto.getDescription());
        coupon.setSourceType(dto.getSourceType() == null ? "platform" : dto.getSourceType());
        coupon.setShopId(dto.getShopId());
        coupon.setScopeType(dto.getScopeType());
        coupon.setScopeIds(dto.getScopeIds());
        coupon.setTotalCount(dto.getTotalCount() == null ? 0 : dto.getTotalCount());
        coupon.setRemainingCount(dto.getRemainingCount() == null ? coupon.getTotalCount() : dto.getRemainingCount());
        coupon.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        coupon.setStartAt(parseDateTime(dto.getStartTime()));
        coupon.setEndAt(parseDateTime(dto.getEndTime()));
        coupon.setUpdatedAt(LocalDateTime.now());
        if (dto.getId() == null) couponTemplateMapper.insert(coupon); else couponTemplateMapper.updateById(coupon);
        return coupon;
    }

    @Override
    public void updateStatus(String targetType, Long id, Integer status) {
        // Normalize: accept both singular and plural form
        String t = targetType == null ? "" : targetType.toLowerCase().replaceAll("s$", "");
        if ("banner".equals(t)) {
            BannerEntity entity = new BannerEntity();
            entity.setId(id);
            entity.setStatus(status);
            updateById(entity);
        } else if ("channel".equals(t)) {
            ChannelEntity entity = new ChannelEntity();
            entity.setId(id);
            entity.setStatus(status);
            channelMapper.updateById(entity);
        } else if ("activity".equals(t) || "activitie".equals(t)) {
            ActivityEntity entity = new ActivityEntity();
            entity.setId(id);
            entity.setStatus(status);
            activityMapper.updateById(entity);
        } else if ("coupon".equals(t)) {
            CouponTemplateEntity entity = new CouponTemplateEntity();
            entity.setId(id);
            entity.setStatus(status);
            couponTemplateMapper.updateById(entity);
        } else {
            throw new IllegalArgumentException("不支持的营销对象类型: " + targetType);
        }
    }

    // ===== Admin-only implementations =====

    @Override
    public List<Map<String, Object>> listAllBannersAdmin() {
        QueryWrapper<BannerEntity> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort").orderByDesc("updated_at");
        return list(wrapper).stream().map(b -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", b.getId());
            row.put("title", b.getTitle());
            row.put("image", signUrl(b.getImage()));
            row.put("linkType", b.getLinkType());
            row.put("linkTarget", b.getLinkTarget());
            row.put("link", b.getLinkTarget()); // alias for frontend
            row.put("sort", b.getSort());
            row.put("status", b.getStatus() == null ? 0 : b.getStatus());
            row.put("startAt", b.getStartAt() == null ? null : b.getStartAt().toString());
            row.put("endAt", b.getEndAt() == null ? null : b.getEndAt().toString());
            return row;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> listActivitiesAdmin(MarketingQueryDto queryDto) {
        MarketingQueryDto q = queryDto == null ? new MarketingQueryDto() : queryDto;
        int page = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int size = q.getPageSize() == null || q.getPageSize() < 1 ? 20 : q.getPageSize();
        QueryWrapper<ActivityEntity> wrapper = new QueryWrapper<>();
        if (q.getStatus() != null) wrapper.eq("status", q.getStatus());
        if (q.getType() != null && !q.getType().isEmpty()) wrapper.eq("type", q.getType());
        if (q.getKeyword() != null && !q.getKeyword().trim().isEmpty()) wrapper.like("title", q.getKeyword());
        if (q.getStartDate() != null && !q.getStartDate().isEmpty()) wrapper.ge("start_at", q.getStartDate() + " 00:00:00");
        if (q.getEndDate() != null && !q.getEndDate().isEmpty()) wrapper.le("end_at", q.getEndDate() + " 23:59:59");
        if (q.getShopId() != null) wrapper.eq("shop_id", q.getShopId());
        IPage<ActivityEntity> resultPage = activityMapper.selectPage(new Page<>(page, size), wrapper.orderByDesc("updated_at"));
        List<Map<String, Object>> records = resultPage.getRecords().stream().map(a -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", a.getId());
            row.put("title", a.getTitle());
            row.put("subtitle", a.getSubtitle());
            row.put("type", a.getType());
            row.put("tag", a.getTag());
            row.put("image", signUrl(a.getImage()));
            row.put("discountText", a.getDiscountText());
            row.put("hotPercent", defaultInt(a.getHotPercent()));
            row.put("participants", defaultInt(a.getParticipants()));
            row.put("status", a.getStatus() == null ? 0 : a.getStatus());
            row.put("startTime", a.getStartAt() == null ? null : a.getStartAt().toString().replace("T", " "));
            row.put("endTime", a.getEndAt() == null ? null : a.getEndAt().toString().replace("T", " "));
            row.put("priority", a.getPriority());
            row.put("shopId", a.getShopId());
            return row;
        }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", resultPage.getTotal());
        result.put("pageNum", page);
        result.put("pageSize", size);
        return result;
    }

    @Override
    public Map<String, Object> listCouponsAdmin(MarketingQueryDto queryDto) {
        MarketingQueryDto q = queryDto == null ? new MarketingQueryDto() : queryDto;
        int page = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int size = q.getPageSize() == null || q.getPageSize() < 1 ? 12 : q.getPageSize();
        QueryWrapper<CouponTemplateEntity> wrapper = new QueryWrapper<>();
        if (q.getStatus() != null) wrapper.eq("status", q.getStatus());
        if (q.getType() != null && !q.getType().isEmpty()) wrapper.eq("type", q.getType());
        if (q.getKeyword() != null && !q.getKeyword().trim().isEmpty()) wrapper.like("name", q.getKeyword());
        if (q.getShopId() != null) wrapper.eq("shop_id", q.getShopId());
        IPage<CouponTemplateEntity> resultPage = couponTemplateMapper.selectPage(new Page<>(page, size), wrapper.orderByDesc("updated_at"));
        List<Map<String, Object>> records = resultPage.getRecords().stream().map(c -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", c.getId());
            row.put("name", c.getName());
            row.put("type", c.getType());
            row.put("value", c.getValue());
            row.put("discountValue", c.getValue()); // alias
            row.put("minAmount", c.getMinAmount());
            row.put("totalCount", c.getTotalCount());
            row.put("remainingCount", c.getRemainingCount());
            row.put("status", c.getStatus() == null ? 0 : c.getStatus());
            row.put("startTime", c.getStartAt() == null ? null : c.getStartAt().toString().replace("T", " "));
            row.put("endTime", c.getEndAt() == null ? null : c.getEndAt().toString().replace("T", " "));
            row.put("description", c.getDescription());
            row.put("scopeType", c.getScopeType());
            row.put("shopId", c.getShopId());
            row.put("receivedCount", (c.getTotalCount() == null ? 0 : c.getTotalCount()) - (c.getRemainingCount() == null ? 0 : c.getRemainingCount()));
            return row;
        }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", resultPage.getTotal());
        result.put("pageNum", page);
        result.put("pageSize", size);
        return result;
    }

    @Override
    public List<Map<String, Object>> listRankingLists() {
        QueryWrapper<RankingListEntity> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("update_time");
        return rankingListMapper.selectList(wrapper).stream().map(r -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", r.getId());
            row.put("name", r.getName());
            row.put("categoryName", r.getCategoryName());
            row.put("status", r.getStatus());
            // Count items
            int count = 0;
            if (r.getItems() != null && r.getItems().trim().startsWith("[")) {
                try {
                    count = r.getItems().trim().split(",").length;
                    if (r.getItems().trim().equals("[]")) count = 0;
                } catch (Exception ignored) { }
            }
            row.put("productCount", count);
            return row;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getRankingProducts(Long listId) {
        RankingListEntity ranking = rankingListMapper.selectById(listId);
        if (ranking == null) throw new IllegalArgumentException("榜单不存在");
        List<Long> productIds = parseItemIds(ranking.getItems());
        if (productIds.isEmpty()) return new java.util.ArrayList<>();
        QueryWrapper<AppProductEntity> wrapper = new QueryWrapper<>();
        wrapper.in("id", productIds);
        List<AppProductEntity> products = appProductMapper.selectList(wrapper);
        // Sort by the order of productIds
        java.util.Map<Long, AppProductEntity> productMap = products.stream()
                .collect(Collectors.toMap(AppProductEntity::getId, p -> p));
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            AppProductEntity p = productMap.get(productIds.get(i));
            if (p == null) continue;
            Map<String, Object> row = new HashMap<>();
            row.put("id", p.getId());
            row.put("name", p.getName());
            row.put("price", p.getPrice());
            row.put("mainImage", p.getMainImage());
            row.put("sales", defaultInt(p.getSales()));
            row.put("score", p.getRating());
            row.put("rankWeight", productIds.size() - i); // higher = earlier in list = more weight
            result.add(row);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRankingProducts(Long listId, List<Long> productIds) {
        RankingListEntity ranking = rankingListMapper.selectById(listId);
        if (ranking == null) throw new IllegalArgumentException("榜单不存在");
        List<Long> current = parseItemIds(ranking.getItems());
        for (Long pid : productIds) {
            if (!current.contains(pid)) current.add(pid);
        }
        ranking.setItems(toItemsJson(current));
        ranking.setUpdateTime(LocalDateTime.now());
        rankingListMapper.updateById(ranking);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRankingProduct(Long listId, Long productId) {
        RankingListEntity ranking = rankingListMapper.selectById(listId);
        if (ranking == null) throw new IllegalArgumentException("榜单不存在");
        List<Long> current = parseItemIds(ranking.getItems());
        current.removeIf(id -> id.equals(productId));
        ranking.setItems(toItemsJson(current));
        ranking.setUpdateTime(LocalDateTime.now());
        rankingListMapper.updateById(ranking);
    }

    @Override
    public RankingListEntity createRankingList(java.util.Map<String, Object> data) {
        RankingListEntity entity = new RankingListEntity();
        entity.setId(idWorker.nextId());
        entity.setName(data.get("name") == null ? "新榜单" : String.valueOf(data.get("name")));
        if (data.get("categoryName") != null) entity.setCategoryName(String.valueOf(data.get("categoryName")));
        entity.setStatus(data.get("status") != null ? Integer.valueOf(String.valueOf(data.get("status"))) : 1);
        entity.setItems("[]");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        rankingListMapper.insert(entity);
        return entity;
    }

    @Override
    public void updateRankingList(Long listId, java.util.Map<String, Object> data) {
        RankingListEntity entity = rankingListMapper.selectById(listId);
        if (entity == null) throw new IllegalArgumentException("榜单不存在");
        if (data.get("name") != null) entity.setName(String.valueOf(data.get("name")));
        if (data.get("categoryName") != null) entity.setCategoryName(String.valueOf(data.get("categoryName")));
        if (data.get("status") != null) entity.setStatus(Integer.valueOf(String.valueOf(data.get("status"))));
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        rankingListMapper.updateById(entity);
    }

    @Override
    public void deleteRankingList(Long listId) {
        rankingListMapper.deleteById(listId);
    }

    /** Parse items JSON string like "[1234,5678]" into list of Long IDs */
    private List<Long> parseItemIds(String items) {
        List<Long> result = new java.util.ArrayList<>();
        if (items == null || items.trim().isEmpty() || items.trim().equals("[]")) return result;
        String s = items.trim().replaceAll("^\\[|\\]$", "").trim();
        if (s.isEmpty()) return result;
        for (String part : s.split(",")) {
            try {
                String cleaned = part.trim().replaceAll("[^0-9]", "");
                if (!cleaned.isEmpty()) result.add(Long.parseLong(cleaned));
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    /** Serialize list of IDs to JSON array string */
    private String toItemsJson(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(ids.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private List<ActivityEntity> listHomeActivities() {
        QueryWrapper<ActivityEntity> wrapper = new QueryWrapper<>();
        LocalDateTime now = LocalDateTime.now();
        wrapper.eq("status", 1)
                .and(item -> item.isNull("start_at").or().le("start_at", now))
                .and(item -> item.isNull("end_at").or().ge("end_at", now))
                .orderByDesc("priority")
                .orderByDesc("hot_percent")
                .last("LIMIT 6");
        return activityMapper.selectList(wrapper);
    }

    private List<Map<String, Object>> listHomeBrandShops() {
        QueryWrapper<ShopEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "Active")
                .eq("is_brand_shop", 1)
                .orderByDesc("followers")
                .orderByDesc("sales")
                .last("LIMIT 8");
        return shopModuleMapper.selectList(wrapper).stream().map(shop -> {
            Map<String, Object> item = new HashMap<>();
            item.put("shopId", shop.getShopId());
            item.put("name", firstNotBlank(shop.getDisplayName(), shop.getShopName()));
            item.put("logo", firstNotBlank(shop.getLogo(), shop.getAvatar()));
            item.put("banner", shop.getBanner());
            item.put("description", shop.getShopDescription());
            item.put("followers", defaultInt(shop.getFollowers()));
            item.put("productCount", defaultInt(shop.getProductCount()));
            item.put("sales", defaultInt(shop.getSales()));
            item.put("rating", shop.getRating());
            return item;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> listHomeProducts(String scene, int limit) {
        QueryWrapper<AppProductEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
                .eq("audit_status", "Approved");
        if ("hotSale".equals(scene)) {
            wrapper.orderByDesc("sales").orderByDesc("rating");
        } else {
            wrapper.orderByDesc("likes").orderByDesc("sales").orderByDesc("created_at");
        }
        wrapper.last("LIMIT " + limit);
        return appProductMapper.selectList(wrapper).stream().map(this::toHomeProductCard).collect(Collectors.toList());
    }

    private Map<String, Object> toHomeProductCard(AppProductEntity product) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", product.getId());
        item.put("name", product.getName());
        item.put("subtitle", product.getSubtitle());
        item.put("price", product.getPrice());
        item.put("originalPrice", product.getOriginalPrice());
        item.put("image", signUrl(product.getMainImage()));
        item.put("tag", product.getTag());
        item.put("tagColor", product.getTagColor());
        item.put("tagBg", product.getTagBg());
        item.put("sales", defaultInt(product.getSales()));
        item.put("rating", product.getRating());
        item.put("likes", defaultInt(product.getLikes()));
        item.put("shopId", product.getShopId());
        ShopEntity shop = product.getShopId() == null ? null : shopModuleMapper.selectById(product.getShopId());
        if (shop != null) {
            item.put("shopName", firstNotBlank(shop.getDisplayName(), shop.getShopName()));
            item.put("shopLogo", firstNotBlank(shop.getLogo(), shop.getAvatar()));
        }
        return item;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    @Override
    public void assertActivityOwnership(Long activityId, Long ownerShopId) {
        ActivityEntity activity = activityMapper.selectById(activityId);
        if (activity == null) throw new IllegalArgumentException("活动不存在");
        if (!ownerShopId.equals(activity.getShopId())) {
            throw new IllegalArgumentException("无权编辑其他店铺的活动");
        }
    }

    @Override
    public void assertCouponOwnership(Long couponId, Long ownerShopId) {
        CouponTemplateEntity coupon = couponTemplateMapper.selectById(couponId);
        if (coupon == null) throw new IllegalArgumentException("优惠券模板不存在");
        if (!ownerShopId.equals(coupon.getShopId())) {
            throw new IllegalArgumentException("无权编辑其他店铺的优惠券");
        }
    }

    private String firstNotBlank(String first, String second) {
        return first == null || first.trim().isEmpty() ? second : first;
    }

    /** Parse datetime string to LocalDateTime; returns null on failure */
    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            String cleaned = s.trim().replace("T", " ");
            if (cleaned.length() == 10) cleaned += " 00:00:00";
            return java.time.LocalDateTime.parse(cleaned,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return null;
        }
    }

    private BannerVo toBannerVo(BannerEntity banner) {
        BannerVo vo = new BannerVo();
        vo.setId(banner.getId());
        vo.setTitle(banner.getTitle());
        vo.setImage(signUrl(banner.getImage()));
        vo.setLinkType(banner.getLinkType());
        vo.setLinkTarget(banner.getLinkTarget());
        return vo;
    }

    private List<Map<String, Object>> listHomePodium(int limit) {
        QueryWrapper<AppProductEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
                .eq("audit_status", "Approved")
                .orderByDesc("sales")
                .orderByDesc("rating")
                .last("LIMIT " + limit);
        List<AppProductEntity> products = appProductMapper.selectList(wrapper);
        int rank = 1;
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (AppProductEntity p : products) {
            Map<String, Object> item = toHomeProductCard(p);
            item.put("rank", rank++);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> listHomeBrandPickProducts(int limit) {
        QueryWrapper<AppProductEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
                .eq("audit_status", "Approved")
                .inSql("shop_id", "SELECT shop_id FROM shop WHERE status='Active' AND is_brand_shop=1")
                .orderByDesc("sales")
                .orderByDesc("rating")
                .last("LIMIT " + limit);
        return appProductMapper.selectList(wrapper).stream()
                .map(p -> {
                    Map<String, Object> item = toHomeProductCard(p);
                    item.put("tag", item.get("shopName") + "官方");
                    item.put("tagColor", "#FFFFFF");
                    item.put("tagBg", "#165DFF");
                    return item;
                }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> listHomeActivityCards(int limit) {
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<ActivityEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
                .and(item -> item.isNull("start_at").or().le("start_at", now))
                .and(item -> item.isNull("end_at").or().ge("end_at", now))
                .orderByDesc("hot_percent")
                .orderByDesc("priority")
                .last("LIMIT " + limit);
        return activityMapper.selectList(wrapper).stream().map(act -> {
            Map<String, Object> card = new HashMap<>();
            card.put("id", act.getId());
            card.put("title", act.getTitle());
            card.put("subtitle", act.getSubtitle());
            card.put("image", act.getImage());
            card.put("tag", act.getTag());
            card.put("tagColor", act.getTagColor());
            card.put("tagBg", act.getTagBg());
            card.put("discountText", act.getDiscountText());
            card.put("hotPercent", defaultInt(act.getHotPercent()));
            card.put("participants", defaultInt(act.getParticipants()));
            card.put("endAt", act.getEndAt() != null ? act.getEndAt().toString() : null);
            card.put("type", act.getType());
            return card;
        }).collect(Collectors.toList());
    }
}
