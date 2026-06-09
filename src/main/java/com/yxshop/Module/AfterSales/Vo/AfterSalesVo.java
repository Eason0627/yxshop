package com.yxshop.Module.AfterSales.Vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AfterSalesVo {
    private Long id;
    private Long orderId;
    private Long orderItemId;
    private Long userId;
    private Long shopId;
    private Long productId;
    private String type;
    private Integer quantity;
    private BigDecimal amount;
    private String reason;
    private String description;
    private String images;
    private String returnCarrierName;
    private String returnTrackingNo;
    private Long exchangeOrderId;
    private Long refundId;
    private String status;
    private Long reviewerId;
    private String reviewRemark;
    private String createdAt;
    private String updatedAt;
}
