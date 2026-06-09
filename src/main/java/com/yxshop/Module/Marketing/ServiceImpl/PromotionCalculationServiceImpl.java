package com.yxshop.Module.Marketing.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yxshop.Module.Marketing.Dto.PromotionCalculateDto;
import com.yxshop.Module.Marketing.Dto.PromotionItemDto;
import com.yxshop.Module.Marketing.Entity.ActivityEntity;
import com.yxshop.Module.Marketing.Entity.CouponTemplateEntity;
import com.yxshop.Module.Marketing.Entity.UserCouponEntity;
import com.yxshop.Module.Marketing.Mapper.ActivityMapper;
import com.yxshop.Module.Marketing.Mapper.CouponTemplateMapper;
import com.yxshop.Module.Marketing.Mapper.UserCouponMapper;
import com.yxshop.Module.Marketing.Service.PromotionCalculationService;
import com.yxshop.Module.Marketing.Vo.PromotionCalculateVo;
import com.yxshop.Module.Marketing.Vo.PromotionDiscountVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PromotionCalculationServiceImpl implements PromotionCalculationService {
    private final ActivityMapper activityMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;

    public PromotionCalculationServiceImpl(ActivityMapper activityMapper,
                                           UserCouponMapper userCouponMapper,
                                           CouponTemplateMapper couponTemplateMapper) {
        this.activityMapper = activityMapper;
        this.userCouponMapper = userCouponMapper;
        this.couponTemplateMapper = couponTemplateMapper;
    }

    @Override
    public PromotionCalculateVo calculate(PromotionCalculateDto dto) {
        PromotionCalculateDto request = dto == null ? new PromotionCalculateDto() : dto;
        PromotionCalculateVo result = new PromotionCalculateVo();
        List<PromotionItemDto> items = request.getItems() == null ? java.util.Collections.emptyList() : request.getItems();
        BigDecimal goodsAmount = calculateGoodsAmount(items);
        result.setGoodsAmount(goodsAmount);

        BigDecimal activityDiscount = calculateActivityDiscount(items, goodsAmount, result);
        result.setActivityDiscount(activityDiscount);

        List<UserCouponEntity> availableCoupons = findAvailableCoupons(request.getUserId(), items, goodsAmount.subtract(activityDiscount));
        result.setAvailableCoupons(availableCoupons);
        BigDecimal couponDiscount = calculateCouponDiscount(request.getCouponId(), availableCoupons, goodsAmount.subtract(activityDiscount), result);
        result.setCouponDiscount(couponDiscount);

        BigDecimal totalDiscount = activityDiscount.add(couponDiscount).min(goodsAmount);
        result.setTotalDiscount(totalDiscount);
        result.setPayAmount(goodsAmount.subtract(totalDiscount).max(BigDecimal.ZERO));
        return result;
    }

    private BigDecimal calculateGoodsAmount(List<PromotionItemDto> items) {
        BigDecimal amount = BigDecimal.ZERO;
        for (PromotionItemDto item : items) {
            BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
            int quantity = item.getQuantity() == null || item.getQuantity() < 1 ? 1 : item.getQuantity();
            amount = amount.add(price.multiply(BigDecimal.valueOf(quantity)));
        }
        return amount;
    }

    private BigDecimal calculateActivityDiscount(List<PromotionItemDto> items, BigDecimal goodsAmount, PromotionCalculateVo result) {
        if (items.isEmpty() || goodsAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<ActivityEntity> wrapper = new LambdaQueryWrapper<ActivityEntity>()
                .eq(ActivityEntity::getStatus, 1)
                .and(q -> q.isNull(ActivityEntity::getStartAt).or().le(ActivityEntity::getStartAt, now))
                .and(q -> q.isNull(ActivityEntity::getEndAt).or().ge(ActivityEntity::getEndAt, now))
                .orderByDesc(ActivityEntity::getPriority);
        List<ActivityEntity> activities = activityMapper.selectList(wrapper);
        BigDecimal total = BigDecimal.ZERO;
        for (ActivityEntity activity : activities) {
            BigDecimal eligibleAmount = eligibleAmount(activity, items);
            if (eligibleAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal discount = parseDiscount(activity, eligibleAmount);
            if (activity.getMaxDiscount() != null) {
                discount = discount.min(activity.getMaxDiscount());
            }
            if (discount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            total = total.add(discount);
            PromotionDiscountVo vo = new PromotionDiscountVo();
            vo.setDiscountType("Activity");
            vo.setSourceId(activity.getId());
            vo.setTitle(activity.getTitle());
            vo.setAmount(discount);
            result.getDiscounts().add(vo);
            if (Integer.valueOf(0).equals(activity.getStackable())) {
                break;
            }
        }
        return total.min(goodsAmount);
    }

    private BigDecimal eligibleAmount(ActivityEntity activity, List<PromotionItemDto> items) {
        String scopeType = activity.getScopeType() == null ? "All" : activity.getScopeType();
        Set<Long> scopeIds = parseIds(activity.getProductIds());
        BigDecimal amount = BigDecimal.ZERO;
        for (PromotionItemDto item : items) {
            boolean matched;
            if ("Shop".equalsIgnoreCase(scopeType)) {
                matched = activity.getShopId() != null && activity.getShopId().equals(item.getShopId());
            } else if ("Product".equalsIgnoreCase(scopeType)) {
                matched = scopeIds.contains(item.getProductId());
            } else {
                matched = true;
            }
            if (matched) {
                BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
                int quantity = item.getQuantity() == null || item.getQuantity() < 1 ? 1 : item.getQuantity();
                amount = amount.add(price.multiply(BigDecimal.valueOf(quantity)));
            }
        }
        return amount;
    }

    private BigDecimal parseDiscount(ActivityEntity activity, BigDecimal amount) {
        String type = activity.getType() == null ? "" : activity.getType();
        String text = activity.getDiscountText() == null ? "" : activity.getDiscountText().trim();
        if ("discount".equalsIgnoreCase(type) || text.endsWith("折")) {
            BigDecimal rate = parseNumber(text);
            if (rate.compareTo(BigDecimal.ZERO) > 0 && rate.compareTo(BigDecimal.TEN) <= 0) {
                return amount.multiply(BigDecimal.TEN.subtract(rate)).divide(BigDecimal.TEN, 2, java.math.RoundingMode.HALF_UP);
            }
        }
        if ("full_reduction".equalsIgnoreCase(type) || text.contains("满")) {
            BigDecimal threshold = parseAfter(text, "满");
            BigDecimal reduce = parseAfter(text, "减");
            if (threshold.compareTo(BigDecimal.ZERO) > 0 && reduce.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(threshold) >= 0) {
                return reduce.min(amount);
            }
        }
        if ("minus".equalsIgnoreCase(type) || "fixed".equalsIgnoreCase(type)) {
            BigDecimal reduce = parseNumber(text);
            return reduce.min(amount);
        }
        return BigDecimal.ZERO;
    }

    private List<UserCouponEntity> findAvailableCoupons(Long userId, List<PromotionItemDto> items, BigDecimal amount) {
        if (userId == null) {
            return java.util.Collections.emptyList();
        }
        LocalDateTime now = LocalDateTime.now();
        List<UserCouponEntity> coupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCouponEntity>()
                .eq(UserCouponEntity::getUserId, userId)
                .eq(UserCouponEntity::getStatus, 1)
                .and(q -> q.isNull(UserCouponEntity::getStartAt).or().le(UserCouponEntity::getStartAt, now))
                .and(q -> q.isNull(UserCouponEntity::getEndAt).or().ge(UserCouponEntity::getEndAt, now))
                .and(q -> q.isNull(UserCouponEntity::getMinAmount).or().le(UserCouponEntity::getMinAmount, amount))
                .orderByDesc(UserCouponEntity::getValue));
        return coupons.stream().filter(coupon -> couponScopeMatched(coupon, items)).collect(Collectors.toList());
    }

    private boolean couponScopeMatched(UserCouponEntity coupon, List<PromotionItemDto> items) {
        CouponTemplateEntity template = couponTemplateMapper.selectById(coupon.getCouponTemplateId());
        if (template == null) {
            // 模板不存在时视为全场通用券，不限制适用范围
            return true;
        }
        String scopeType = template.getScopeType() == null ? "All" : template.getScopeType();
        if ("All".equalsIgnoreCase(scopeType)) {
            return true;
        }
        if ("Shop".equalsIgnoreCase(scopeType)) {
            // shopId 未配置时视为全场通用
            if (template.getShopId() == null) return true;
            return items.stream().anyMatch(item -> template.getShopId().equals(item.getShopId()));
        }
        if ("Product".equalsIgnoreCase(scopeType)) {
            Set<Long> scopeIds = parseIds(template.getScopeIds());
            // scopeIds 为空时视为全场通用
            if (scopeIds.isEmpty()) return true;
            return items.stream().anyMatch(item -> scopeIds.contains(item.getProductId()));
        }
        return true;
    }

    private BigDecimal calculateCouponDiscount(Long couponId, List<UserCouponEntity> availableCoupons, BigDecimal amount, PromotionCalculateVo result) {
        if (couponId == null) {
            return BigDecimal.ZERO;
        }
        UserCouponEntity selected = availableCoupons.stream()
                .filter(coupon -> couponId.equals(coupon.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("优惠券不可用"));
        result.setSelectedCoupon(selected);
        BigDecimal discount = selected.getValue() == null ? BigDecimal.ZERO : selected.getValue().min(amount);
        PromotionDiscountVo vo = new PromotionDiscountVo();
        vo.setDiscountType("Coupon");
        vo.setSourceId(selected.getId());
        vo.setTitle(selected.getName());
        vo.setAmount(discount);
        result.getDiscounts().add(vo);
        return discount;
    }

    private Set<Long> parseIds(String value) {
        Set<Long> ids = new HashSet<>();
        if (value == null || value.trim().isEmpty()) {
            return ids;
        }
        for (String item : value.split(",")) {
            try {
                ids.add(Long.valueOf(item.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private BigDecimal parseAfter(String text, String marker) {
        int index = text.indexOf(marker);
        if (index < 0) {
            return BigDecimal.ZERO;
        }
        return parseNumber(text.substring(index + marker.length()));
    }

    private BigDecimal parseNumber(String text) {
        StringBuilder builder = new StringBuilder();
        boolean started = false;
        for (char ch : text.toCharArray()) {
            if ((ch >= '0' && ch <= '9') || ch == '.') {
                builder.append(ch);
                started = true;
            } else if (started) {
                break;
            }
        }
        if (builder.length() == 0) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(builder.toString());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}
