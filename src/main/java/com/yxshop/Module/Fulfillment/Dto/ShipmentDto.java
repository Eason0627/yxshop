package com.yxshop.Module.Fulfillment.Dto;

import lombok.Data;

@Data
public class ShipmentDto {
    private Long orderId;
    private String carrier;
    private String trackingNumber;
}
