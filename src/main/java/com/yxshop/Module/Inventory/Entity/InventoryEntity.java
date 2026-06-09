package com.yxshop.Module.Inventory.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inventory")
public class InventoryEntity {
    @TableId("inventory_id")
    private Long inventoryId;
    @TableField("product_id")
    private Long productId;
    @TableField("warehouse_id")
    private Long warehouseId;
    @TableField("stock_quantity")
    private Integer stockQuantity;
    @TableField("reserved_stock")
    private Integer reservedStock;
    @TableField("safety_stock")
    private Integer safetyStock;
    @TableField("last_restock_date")
    private LocalDateTime lastRestockDate;
    @TableField("restock_threshold")
    private Integer restockThreshold;
    @TableField("createTime")
    private LocalDateTime createTime;
    @TableField("updateTime")
    private LocalDateTime updateTime;
}
