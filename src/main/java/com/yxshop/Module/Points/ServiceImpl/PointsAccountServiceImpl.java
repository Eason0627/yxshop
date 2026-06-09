package com.yxshop.Module.Points.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yxshop.Module.Order.Entity.OrderEntity;
import com.yxshop.Module.Order.Entity.OrderItemEntity;
import com.yxshop.Module.Order.Mapper.OrderItemMapper;
import com.yxshop.Module.Order.Mapper.OrderModuleMapper;
import com.yxshop.Module.Points.Dto.PointsExchangeDto;
import com.yxshop.Module.Points.Entity.CheckinCampaignEntity;
import com.yxshop.Module.Points.Entity.CheckinRecordEntity;
import com.yxshop.Module.Points.Entity.PointsAccountEntity;
import com.yxshop.Module.Points.Entity.PointsGoodsEntity;
import com.yxshop.Module.Points.Entity.PointsOrderEntity;
import com.yxshop.Module.Points.Entity.PointsRecordEntity;
import com.yxshop.Module.Points.Mapper.CheckinCampaignMapper;
import com.yxshop.Module.Points.Mapper.CheckinRecordMapper;
import com.yxshop.Module.Points.Mapper.PointsAccountMapper;
import com.yxshop.Module.Points.Mapper.PointsGoodsMapper;
import com.yxshop.Module.Points.Mapper.PointsOrderMapper;
import com.yxshop.Module.Points.Mapper.PointsRecordMapper;
import com.yxshop.Module.Points.Service.PointsAccountService;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Utils.SnowflakeIdWorker;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Primary
@Service
public class PointsAccountServiceImpl extends ServiceImpl<PointsAccountMapper, PointsAccountEntity> implements PointsAccountService {

    private final PointsGoodsMapper pointsGoodsMapper;
    private final PointsOrderMapper pointsOrderMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final CheckinRecordMapper checkinRecordMapper;
    private final CheckinCampaignMapper checkinCampaignMapper;
    private final ShopModuleMapper shopMapper;
    private final OrderModuleMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(24, 1);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PointsAccountServiceImpl(PointsGoodsMapper pointsGoodsMapper,
                                    PointsOrderMapper pointsOrderMapper,
                                    PointsRecordMapper pointsRecordMapper,
                                    CheckinRecordMapper checkinRecordMapper,
                                    CheckinCampaignMapper checkinCampaignMapper,
                                    ShopModuleMapper shopMapper,
                                    OrderModuleMapper orderMapper,
                                    OrderItemMapper orderItemMapper) {
        this.pointsGoodsMapper = pointsGoodsMapper;
        this.pointsOrderMapper = pointsOrderMapper;
        this.pointsRecordMapper = pointsRecordMapper;
        this.checkinRecordMapper = checkinRecordMapper;
        this.checkinCampaignMapper = checkinCampaignMapper;
        this.shopMapper = shopMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    public Object getAccount(Long userId) {
        return ensureAccount(userId);
    }

    @Override
    public Object getRecords(Long userId, Integer page, Integer size) {
        return pointsRecordMapper.selectPage(new Page<>(safePage(page), safeSize(size)),
                new LambdaQueryWrapper<PointsRecordEntity>()
                        .eq(PointsRecordEntity::getUserId, userId)
                        .orderByDesc(PointsRecordEntity::getCreatedAt));
    }

    @Override
    public Object listGoods(Integer page, Integer size) {
        return pointsGoodsMapper.selectPage(new Page<>(safePage(page), safeSize(size)),
                new LambdaQueryWrapper<PointsGoodsEntity>()
                        .eq(PointsGoodsEntity::getStatus, 1)
                        .orderByDesc(PointsGoodsEntity::getCreatedAt));
    }

    @Override
    public Object getGoods(Long goodsId) {
        PointsGoodsEntity goods = pointsGoodsMapper.selectById(goodsId);
        if (goods == null || goods.getStatus() == null || goods.getStatus() != 1) {
            throw new RuntimeException("积分商品不存在或已下架");
        }
        return goods;
    }

    @Override
    public Object mallMeta() {
        List<PointsGoodsEntity> goodsList = pointsGoodsMapper.selectList(new LambdaQueryWrapper<PointsGoodsEntity>()
                .eq(PointsGoodsEntity::getStatus, 1)
                .orderByDesc(PointsGoodsEntity::getCreatedAt));

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PointsGoodsEntity goods : goodsList) {
            String category = normalizeCategory(goods.getDescription());
            counts.put(category, counts.getOrDefault(category, 0) + 1);
        }

        List<Map<String, Object>> categories = new ArrayList<>();
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("name", "全部");
        all.put("count", goodsList.size());
        categories.add(all);
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", entry.getKey());
            item.put("count", entry.getValue());
            categories.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", categories);
        result.put("totalGoods", goodsList.size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object exchange(Long userId, PointsExchangeDto dto) {
        if (dto == null || dto.getGoodsId() == null) {
            throw new RuntimeException("请选择积分商品");
        }
        int quantity = dto.getQuantity() == null || dto.getQuantity() <= 0 ? 1 : dto.getQuantity();
        PointsGoodsEntity goods = pointsGoodsMapper.selectById(dto.getGoodsId());
        if (goods == null || goods.getStatus() == null || goods.getStatus() != 1) {
            throw new RuntimeException("积分商品不存在或已下架");
        }
        if (goods.getStock() == null || goods.getStock() < quantity) {
            throw new RuntimeException("积分商品库存不足");
        }
        PointsAccountEntity account = ensureAccount(userId);
        int needPoints = goods.getPointsPrice() * quantity;
        if (account.getCurrentPoints() == null || account.getCurrentPoints() < needPoints) {
            throw new RuntimeException("积分不足");
        }

        int updated = pointsGoodsMapper.update(null,
                new LambdaUpdateWrapper<PointsGoodsEntity>()
                        .eq(PointsGoodsEntity::getId, goods.getId())
                        .ge(PointsGoodsEntity::getStock, quantity)
                        .setSql("stock = stock - " + quantity));
        if (updated == 0) {
            throw new RuntimeException("积分商品库存不足");
        }

        int before = account.getCurrentPoints();
        account.setCurrentPoints(before - needPoints);
        account.setTotalSpent((account.getTotalSpent() == null ? 0 : account.getTotalSpent()) + needPoints);
        account.setUpdatedAt(LocalDateTime.now());
        baseMapper.updateById(account);

        PointsOrderEntity order = new PointsOrderEntity();
        order.setId(idWorker.nextId());
        order.setOrderNo("PO" + order.getId());
        order.setUserId(userId);
        order.setGoodsId(goods.getId());
        order.setGoodsName(goods.getName());
        order.setPointsAmount(needPoints);
        order.setQuantity(quantity);
        order.setAddressSnapshot(dto.getAddressSnapshot());
        order.setStatus("Created");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        pointsOrderMapper.insert(order);

        writeRecord(userId, "Spend", needPoints, before, before - needPoints, "PointsOrder", order.getId(), "兑换积分商品");

        // 同步创建普通订单（平台自营店处理，已用积分支付，直接进入待发货）
        createNormalOrderForExchange(userId, goods, quantity, needPoints, dto.getAddressSnapshot(), order.getId());

        return order;
    }

    /**
     * 积分兑换完成后，在主订单系统同步生成一张普通订单：
     * - 店铺：平台自营店
     * - 金额：¥0（已用积分支付）
     * - 状态：已付款 / 待发货
     */
    private void createNormalOrderForExchange(Long userId, PointsGoodsEntity goods,
                                              int quantity, int pointsUsed,
                                              String addressSnapshot, Long pointsOrderId) {
        try {
            // 查询平台自营店
            ShopEntity platformShop = shopMapper.selectOne(
                    new QueryWrapper<ShopEntity>().eq("shop_name", "平台自营店").last("LIMIT 1"));
            Long shopId = platformShop != null ? platformShop.getShopId() : null;
            String shopName = platformShop != null
                    ? (platformShop.getDisplayName() != null ? platformShop.getDisplayName() : platformShop.getShopName())
                    : "平台自营店";

            LocalDateTime now = LocalDateTime.now();
            String ts = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            OrderEntity orderEntity = new OrderEntity();
            long orderId = idWorker.nextId();
            orderEntity.setOrderId(orderId);
            orderEntity.setOrderNumber("PX" + ts + Math.abs(orderId % 100000));
            orderEntity.setCustomerId(userId);
            orderEntity.setShopId(shopId);
            orderEntity.setShopName(shopName);
            orderEntity.setGoodsAmount(BigDecimal.ZERO);
            orderEntity.setOrderTotal(BigDecimal.ZERO);
            orderEntity.setCouponAmount(BigDecimal.ZERO);
            orderEntity.setActivityDiscount(BigDecimal.ZERO);
            orderEntity.setDiscountAmount(BigDecimal.ZERO);
            orderEntity.setOrderStatus("PendingShipment");
            orderEntity.setPaymentStatus("Paid");
            orderEntity.setFulfillmentStatus("Unshipped");
            orderEntity.setAfterSalesStatus("None");
            orderEntity.setAddressSnapshot(addressSnapshot);
            orderEntity.setBuyerRemark("积分兑换（消耗 " + pointsUsed + " 积分，积分订单号 " + pointsOrderId + "）");
            orderEntity.setPaidAt(now);
            orderEntity.setCreateTime(now);
            orderEntity.setUpdateTime(now);
            orderMapper.insert(orderEntity);

            OrderItemEntity item = new OrderItemEntity();
            item.setId(idWorker.nextId());
            item.setOrderId(orderId);
            item.setProductId(goods.getId());
            item.setShopId(shopId);
            item.setProductName(goods.getName());
            item.setProductImage(goods.getImage());
            item.setSpecsText("积分兑换");
            item.setPrice(BigDecimal.ZERO);
            item.setQuantity(quantity);
            item.setRefundQuantity(0);
            item.setItemStatus("Normal");
            item.setAfterSalesStatus("None");
            item.setReviewStatus("PendingReview");
            item.setCreatedAt(now);
            orderItemMapper.insert(item);
        } catch (Exception e) {
            // 主订单创建失败不影响积分兑换主流程，记录日志
            System.err.println("[PointsExchange] 同步创建普通订单失败: " + e.getMessage());
        }
    }

    @Override
    public Object listOrders(Long userId, Integer page, Integer size) {
        return pointsOrderMapper.selectPage(new Page<>(safePage(page), safeSize(size)),
                new LambdaQueryWrapper<PointsOrderEntity>()
                        .eq(PointsOrderEntity::getUserId, userId)
                        .orderByDesc(PointsOrderEntity::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object checkin(Long userId) {
        LocalDate today = LocalDate.now();
        CheckinRecordEntity existed = checkinRecordMapper.selectOne(new LambdaQueryWrapper<CheckinRecordEntity>()
                .eq(CheckinRecordEntity::getUserId, userId)
                .eq(CheckinRecordEntity::getCheckinDate, today));
        if (existed != null) {
            return buildCheckinStatus(userId, existed, true, 0);
        }

        CheckinRecordEntity yesterday = checkinRecordMapper.selectOne(new LambdaQueryWrapper<CheckinRecordEntity>()
                .eq(CheckinRecordEntity::getUserId, userId)
                .eq(CheckinRecordEntity::getCheckinDate, today.minusDays(1)));
        int continuousDays = yesterday == null ? 1 : (yesterday.getContinuousDays() == null ? 1 : yesterday.getContinuousDays() + 1);
        int basePoints = resolveWeeklyPoints(today);
        int milestoneRewardPoints = resolveMilestoneRewardPoints(continuousDays);
        int points = basePoints + milestoneRewardPoints;

        PointsAccountEntity account = ensureAccount(userId);
        int before = account.getCurrentPoints() == null ? 0 : account.getCurrentPoints();
        account.setCurrentPoints(before + points);
        account.setTotalEarned((account.getTotalEarned() == null ? 0 : account.getTotalEarned()) + points);
        account.setUpdatedAt(LocalDateTime.now());
        baseMapper.updateById(account);

        CheckinRecordEntity record = new CheckinRecordEntity();
        record.setId(idWorker.nextId());
        record.setUserId(userId);
        record.setCheckinDate(today);
        record.setContinuousDays(continuousDays);
        record.setPoints(points);
        record.setCreatedAt(LocalDateTime.now());
        checkinRecordMapper.insert(record);
        writeRecord(userId, "Earn", points, before, before + points, "Checkin", record.getId(), milestoneRewardPoints > 0 ? "每日签到+连续签到奖励" : "每日签到");
        return buildCheckinStatus(userId, record, false, milestoneRewardPoints);
    }

    @Override
    public Object todayCheckin(Long userId) {
        LocalDate today = LocalDate.now();
        CheckinRecordEntity record = checkinRecordMapper.selectOne(new LambdaQueryWrapper<CheckinRecordEntity>()
                .eq(CheckinRecordEntity::getUserId, userId)
                .eq(CheckinRecordEntity::getCheckinDate, today));
        return buildCheckinStatus(userId, record, false, 0);
    }

    @Override
    public Object checkinConfig() {
        CheckinCampaignEntity campaign = loadActiveCampaign();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("campaignId", campaign == null ? null : campaign.getId());
        result.put("name", campaign == null ? "签到活动" : campaign.getName());
        result.put("status", campaign == null ? 1 : campaign.getStatus());
        result.put("startAt", campaign == null ? null : campaign.getStartAt());
        result.put("endAt", campaign == null ? null : campaign.getEndAt());
        result.put("weeklyRules", loadWeeklyRules(campaign));
        result.put("milestones", loadMilestones(campaign));
        return result;
    }

    private PointsAccountEntity ensureAccount(Long userId) {
        PointsAccountEntity account = baseMapper.selectById(userId);
        if (account != null) {
            return account;
        }
        account = new PointsAccountEntity();
        account.setUserId(userId);
        account.setCurrentPoints(0);
        account.setTotalEarned(0);
        account.setTotalSpent(0);
        account.setExpireDate(LocalDate.now().plusYears(1));
        account.setUpdatedAt(LocalDateTime.now());
        baseMapper.insert(account);
        return account;
    }

    private void writeRecord(Long userId, String changeType, int points, int before, int after,
                             String bizType, Long bizId, String description) {
        PointsRecordEntity record = new PointsRecordEntity();
        record.setId(idWorker.nextId());
        record.setUserId(userId);
        record.setChangeType(changeType);
        record.setPoints(points);
        record.setBeforePoints(before);
        record.setAfterPoints(after);
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setDescription(description);
        record.setCreatedAt(LocalDateTime.now());
        pointsRecordMapper.insert(record);
    }

    private Map<String, Object> buildCheckinStatus(Long userId, CheckinRecordEntity todayRecord, boolean repeated, int milestoneRewardPoints) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() % 7);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<CheckinRecordEntity> weeklyRecords = checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecordEntity>()
                .eq(CheckinRecordEntity::getUserId, userId)
                .ge(CheckinRecordEntity::getCheckinDate, weekStart)
                .le(CheckinRecordEntity::getCheckinDate, weekEnd)
                .orderByAsc(CheckinRecordEntity::getCheckinDate));

        CheckinRecordEntity latestRecord = checkinRecordMapper.selectOne(new LambdaQueryWrapper<CheckinRecordEntity>()
                .eq(CheckinRecordEntity::getUserId, userId)
                .orderByDesc(CheckinRecordEntity::getCheckinDate)
                .last("LIMIT 1"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checked", todayRecord != null);
        result.put("repeated", repeated);
        result.put("points", todayRecord == null ? 0 : todayRecord.getPoints());
        result.put("continuousDays", latestRecord == null || latestRecord.getContinuousDays() == null ? 0 : latestRecord.getContinuousDays());
        result.put("record", todayRecord);
        result.put("weeklyRecords", weeklyRecords);
        result.put("milestoneRewardPoints", milestoneRewardPoints);
        return result;
    }

    private CheckinCampaignEntity loadActiveCampaign() {
        return checkinCampaignMapper.selectOne(new LambdaQueryWrapper<CheckinCampaignEntity>()
                .eq(CheckinCampaignEntity::getStatus, 1)
                .orderByDesc(CheckinCampaignEntity::getStartAt)
                .last("LIMIT 1"));
    }

    private int resolveWeeklyPoints(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        for (Map<String, Object> rule : loadWeeklyRules(loadActiveCampaign())) {
            Integer value = toInteger(rule.get("dayOfWeek"));
            if (value != null && value == dayOfWeek) {
                Integer points = toInteger(rule.get("points"));
                if (points != null && points > 0) {
                    return points;
                }
            }
        }
        return 5;
    }

    private int resolveMilestoneRewardPoints(int continuousDays) {
        for (Map<String, Object> milestone : loadMilestones(loadActiveCampaign())) {
            Integer days = toInteger(milestone.get("days"));
            if (days != null && days == continuousDays) {
                Integer rewardPoints = toInteger(milestone.get("rewardPoints"));
                return rewardPoints == null ? 0 : rewardPoints;
            }
        }
        return 0;
    }

    private List<Map<String, Object>> loadWeeklyRules(CheckinCampaignEntity campaign) {
        List<Map<String, Object>> parsed = parseJsonList(campaign == null ? null : campaign.getWeeklyRules());
        if (!parsed.isEmpty()) {
            parsed.sort(Comparator.comparingInt(item -> toInteger(item.get("dayOfWeek")) == null ? 99 : toInteger(item.get("dayOfWeek"))));
            return parsed;
        }
        List<Map<String, Object>> fallback = new ArrayList<>();
        fallback.add(rule(1, "周一", 5));
        fallback.add(rule(2, "周二", 5));
        fallback.add(rule(3, "周三", 6));
        fallback.add(rule(4, "周四", 6));
        fallback.add(rule(5, "周五", 7));
        fallback.add(rule(6, "周六", 8));
        fallback.add(rule(7, "周日", 8));
        return fallback;
    }

    private List<Map<String, Object>> loadMilestones(CheckinCampaignEntity campaign) {
        List<Map<String, Object>> parsed = parseJsonList(campaign == null ? null : campaign.getMilestones());
        if (!parsed.isEmpty()) {
            parsed.sort(Comparator.comparingInt(item -> toInteger(item.get("days")) == null ? Integer.MAX_VALUE : toInteger(item.get("days"))));
            return parsed;
        }
        List<Map<String, Object>> fallback = new ArrayList<>();
        fallback.add(milestone(3, 10, "+10积分", "ri-gift-line"));
        fallback.add(milestone(7, 30, "+30积分", "ri-medal-line"));
        fallback.add(milestone(15, 80, "+80积分", "ri-vip-crown-line"));
        fallback.add(milestone(30, 200, "+200积分", "ri-trophy-line"));
        return fallback;
    }

    private List<Map<String, Object>> parseJsonList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() { });
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private Map<String, Object> rule(int dayOfWeek, String label, int points) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("dayOfWeek", dayOfWeek);
        item.put("label", label);
        item.put("points", points);
        return item;
    }

    private Map<String, Object> milestone(int days, int rewardPoints, String label, String icon) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("days", days);
        item.put("rewardPoints", rewardPoints);
        item.put("label", label);
        item.put("icon", icon);
        return item;
    }

    private String normalizeCategory(String description) {
        String text = description == null ? "" : description;
        if (text.contains("餐饮")) return "餐饮";
        if (text.contains("数码")) return "数码";
        if (text.contains("配件")) return "配件";
        if (text.contains("生活")) return "生活";
        if (text.contains("运动")) return "运动";
        if (text.contains("美妆")) return "美妆";
        return "其他";
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private long safePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long safeSize(Integer size) {
        return size == null || size < 1 ? 20 : Math.min(size, 100);
    }
}
