package com.yxshop.Module.Fulfillment.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("logistics_trace")
public class LogisticsTraceEntity {
    @TableId
    private Long id;
    @TableField("fulfillment_id")
    private Long fulfillmentId;
    @TableField("order_id")
    private Long orderId;
    private String status;
    private String content;
    private String location;
    /** 纬度（高德坐标系，地图选点时由前端传入，精确到小数点后6位） */
    private Double lat;
    /** 经度（高德坐标系） */
    private Double lng;
    @TableField("trace_time")
    private LocalDateTime traceTime;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
