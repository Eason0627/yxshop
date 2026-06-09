package com.yxshop.Module.Marketing.Vo;

import com.yxshop.Module.Marketing.Entity.UserCouponEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PromotionCalculateVo {
    private BigDecimal goodsAmount = BigDecimal.ZERO;
    private BigDecimal activityDiscount = BigDecimal.ZERO;
    private BigDecimal couponDiscount = BigDecimal.ZERO;
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    private BigDecimal payAmount = BigDecimal.ZERO;
    private List<PromotionDiscountVo> discounts = new ArrayList<>();
    private List<UserCouponEntity> availableCoupons = new ArrayList<>();
    private UserCouponEntity selectedCoupon;
}
