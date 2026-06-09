package com.yxshop.Module.Inventory.Vo;

import lombok.Data;

@Data
public class InventoryVo {
    private Long inventoryId;
    private Long productId;
    private String productName;
    private String productImage;
    private Long shopId;
    private String shopName;
    private Long warehouseId;
    private Integer stockQuantity;
    private Integer reservedStock;
    private Integer availableStock;
    private Integer safetyStock;
    private Integer restockThreshold;
    private Boolean warning;
    private String lastRestockDate;
}
