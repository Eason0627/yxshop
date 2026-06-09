package com.yxshop.Module.Order.Vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVo {
    private Long id;
    private Long productId;
    private Long shopId;
    private String productName;
    private String productImage;
    private String specsText;
    private BigDecimal price;
    private Integer quantity;
    private Integer refundQuantity;
    private String itemStatus;
    private String afterSalesStatus;
    private String reviewStatus;
    private BigDecimal subtotal;
}
