package com.yxshop.Module.Inventory.Dto;

import lombok.Data;

@Data
public class StockAdjustDto {
    private Long productId;
    private Long warehouseId;
    private Long orderId;
    private Integer quantity;
    private Integer safetyStock;
    private Integer restockThreshold;
    private String adjustType;
    private String reason;
}
