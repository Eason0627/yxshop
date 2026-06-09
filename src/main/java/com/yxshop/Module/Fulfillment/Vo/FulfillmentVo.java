package com.yxshop.Module.Fulfillment.Vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FulfillmentVo {
    private Long id;
    private Long orderId;
    private Long shopId;
    private String carrierName;
    private String trackingNo;
    private String status;
    private String shippedAt;
    private String receivedAt;
    private List<LogisticsTraceVo> traces = new ArrayList<>();
}
