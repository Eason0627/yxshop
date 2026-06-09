package com.yxshop.Module.User.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("footprint")
public class FootprintEntity {
    @TableId
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("target_type")
    private String targetType;
    @TableField("target_id")
    private Long targetId;
    @TableField("target_snapshot")
    private String targetSnapshot;
    @TableField("view_date")
    private LocalDate viewDate;
    @TableField("view_at")
    private LocalDateTime viewAt;
}
