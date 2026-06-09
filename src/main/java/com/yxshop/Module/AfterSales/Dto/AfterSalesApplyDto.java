package com.yxshop.Module.AfterSales.Dto;

import lombok.Data;

@Data
public class AfterSalesApplyDto {
    private Long orderId;
    private Long orderItemId;
    private String type;
    private Integer quantity;
    private String reason;
    private String description;
    private String images;
    private String returnCarrierName;
    private String returnTrackingNo;
}
