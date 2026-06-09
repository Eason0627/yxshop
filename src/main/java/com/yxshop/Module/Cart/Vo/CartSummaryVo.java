package com.yxshop.Module.Cart.Vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CartSummaryVo {
    private List<CartShopVo> shops = new ArrayList<>();
    private Integer totalCount = 0;
    private Integer selectedCount = 0;
    private BigDecimal selectedAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal payAmount = BigDecimal.ZERO;
    private List<?> availableCoupons = new ArrayList<>();
    private List<?> promotionDiscounts = new ArrayList<>();
}
