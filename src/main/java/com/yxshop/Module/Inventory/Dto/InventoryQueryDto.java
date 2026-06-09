package com.yxshop.Module.Inventory.Dto;

import lombok.Data;

@Data
public class InventoryQueryDto {
    private Long productId;
    private Long warehouseId;
    private Long shopId;
    private Boolean warningOnly;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
