package com.yxshop.Module.Points.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_record")
public class PointsRecordEntity {
    @TableId
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("change_type")
    private String changeType;
    private Integer points;
    @TableField("before_points")
    private Integer beforePoints;
    @TableField("after_points")
    private Integer afterPoints;
    @TableField("biz_type")
    private String bizType;
    @TableField("biz_id")
    private Long bizId;
    private String description;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
