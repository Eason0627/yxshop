package com.yxshop.Module.Points.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_order")
public class PointsOrderEntity {
    @TableId
    private Long id;
    @TableField("order_no")
    private String orderNo;
    @TableField("user_id")
    private Long userId;
    @TableField("goods_id")
    private Long goodsId;
    @TableField("goods_name")
    private String goodsName;
    @TableField("points_amount")
    private Integer pointsAmount;
    private Integer quantity;
    @TableField("address_snapshot")
    private String addressSnapshot;
    private String status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
