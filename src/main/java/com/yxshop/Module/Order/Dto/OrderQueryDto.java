package com.yxshop.Module.Order.Dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderQueryDto {
    private Integer pageNum;
    private Integer pageSize;
    private String orderStatus;
    private String paymentStatus;
    private String fulfillmentStatus;
    private String afterSalesStatus;
    private Long shopId;
    private String keyword;        // 搜索：订单号、买家备注
    private String startDate;      // 下单开始日期 yyyy-MM-dd
    private String endDate;        // 下单结束日期 yyyy-MM-dd
    private BigDecimal amountMin;  // 订单金额下限
    private BigDecimal amountMax;  // 订单金额上限
}
