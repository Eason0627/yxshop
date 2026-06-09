package com.yxshop.Module.Fulfillment.Vo;

import lombok.Data;

@Data
public class LogisticsVo {
    private Long orderId;
    private String carrier;
    private String trackingNumber;
    private String status;
}
