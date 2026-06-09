package com.yxshop.Module.Marketing.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon_template")
public class CouponTemplateEntity {
    @TableId
    private Long id;
    private String name;
    private String label;
    private String type;
    private BigDecimal value;
    @TableField("min_amount")
    private BigDecimal minAmount;
    private String description;
    @TableField("source_type")
    private String sourceType;
    @TableField("shop_id")
    private Long shopId;
    @TableField("scope_type")
    private String scopeType;
    @TableField("scope_ids")
    private String scopeIds;
    @TableField("total_count")
    private Integer totalCount;
    @TableField("remaining_count")
    private Integer remainingCount;
    private Integer status;
    @TableField("start_at")
    private LocalDateTime startAt;
    @TableField("end_at")
    private LocalDateTime endAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
