package com.yxshop.Module.Points.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("points_account")
public class PointsAccountEntity {
    @TableId("user_id")
    private Long userId;
    @TableField("current_points")
    private Integer currentPoints;
    @TableField("total_earned")
    private Integer totalEarned;
    @TableField("total_spent")
    private Integer totalSpent;
    @TableField("expire_date")
    private LocalDate expireDate;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
