package com.yxshop.Module.Inventory.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock_record")
public class StockRecordEntity {
    @TableId
    private Long id;
    @TableField("product_id")
    private Long productId;
    @TableField("warehouse_id")
    private Long warehouseId;
    @TableField("order_id")
    private Long orderId;
    @TableField("change_type")
    private String changeType;
    @TableField("change_quantity")
    private Integer changeQuantity;
    @TableField("before_quantity")
    private Integer beforeQuantity;
    @TableField("after_quantity")
    private Integer afterQuantity;
    private String reason;
    @TableField("operator_id")
    private Long operatorId;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
