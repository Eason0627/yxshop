package com.yxshop.Module.Fulfillment.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fulfillment_order")
public class FulfillmentEntity {
    @TableId
    private Long id;
    @TableField("order_id")
    private Long orderId;
    @TableField("shop_id")
    private Long shopId;
    @TableField("carrier_name")
    private String carrierName;
    @TableField("tracking_no")
    private String trackingNo;
    private String status;
    @TableField("shipped_at")
    private LocalDateTime shippedAt;
    @TableField("received_at")
    private LocalDateTime receivedAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
