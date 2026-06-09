package com.yxshop.Module.Marketing.Vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromotionDiscountVo {
    private String discountType;
    private Long sourceId;
    private String title;
    private BigDecimal amount = BigDecimal.ZERO;
}
