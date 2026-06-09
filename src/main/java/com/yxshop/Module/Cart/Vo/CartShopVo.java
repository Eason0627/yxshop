package com.yxshop.Module.Cart.Vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CartShopVo {
    private Long shopId;
    private String shopName;
    private List<CartItemVo> items = new ArrayList<>();
    private BigDecimal selectedAmount = BigDecimal.ZERO;
    private Integer selectedCount = 0;
}
