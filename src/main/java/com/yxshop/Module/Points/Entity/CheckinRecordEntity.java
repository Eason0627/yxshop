package com.yxshop.Module.Points.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("checkin_record")
public class CheckinRecordEntity {
    @TableId
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("checkin_date")
    private LocalDate checkinDate;
    private Integer points;
    @TableField("continuous_days")
    private Integer continuousDays;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
