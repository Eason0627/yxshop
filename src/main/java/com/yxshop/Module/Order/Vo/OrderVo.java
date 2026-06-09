package com.yxshop.Module.Order.Vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderVo {
    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private Long shopId;
    private String shopName;
    private BigDecimal goodsAmount;
    private BigDecimal orderTotal;
    private String orderStatus;
    private String paymentStatus;
    private String fulfillmentStatus;
    private String afterSalesStatus;
    private Long addressId;
    private Long couponId;
    private BigDecimal couponAmount;
    private BigDecimal activityDiscount;
    private BigDecimal discountAmount;
    private String addressSnapshot;
    private String buyerRemark;
    private String adminRemark;
    private String shippingCompany;
    private String trackingNo;
    private String shippedAt;
    private String paidAt;
    private String confirmedAt;
    private String cancelledAt;
    private List<OrderItemVo> items = new ArrayList<>();
    private String createTime;
    private String updateTime;
}
