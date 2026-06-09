package com.yxshop.Module.Points.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_goods")
public class PointsGoodsEntity {
    @TableId
    private Long id;
    private String name;
    private String description;
    private String image;
    @TableField("points_price")
    private Integer pointsPrice;
    private Integer stock;
    private Integer status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
