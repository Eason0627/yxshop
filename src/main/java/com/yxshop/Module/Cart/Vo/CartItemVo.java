package com.yxshop.Module.Cart.Vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemVo {
    private Long id;
    private Long productId;
    private Long shopId;
    private String shopName;
    private String productName;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String image;
    private String specsText;
    private Integer quantity;
    private Integer selected;
    private Integer stock;
    private Integer availableStock;
    private Integer invalid;
    private BigDecimal subtotal;
}
