package com.yxshop.Module.Fulfillment.Dto;

import lombok.Data;

@Data
public class ShipOrderDto {
    private Long orderId;
    private String carrierName;
    private String trackingNo;
    private String location;
}
