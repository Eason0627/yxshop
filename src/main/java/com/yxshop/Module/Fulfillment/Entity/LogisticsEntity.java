package com.yxshop.Module.Fulfillment.Entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("logistics")
public class LogisticsEntity {
    @TableId
    private Long logisticsId;
    private Long orderId;
    private String trackingNumber;
    private String carrier;
    private String status;
}
