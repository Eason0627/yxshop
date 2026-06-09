package com.yxshop.Module.Marketing.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_coupon")
public class UserCouponEntity {
    @TableId
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("coupon_template_id")
    private Long couponTemplateId;
    private String name;
    private String type;
    private String label;
    @TableField("min_amount")
    private BigDecimal minAmount;
    private BigDecimal value;
    private String description;
    private Integer status;
    @TableField("locked_order_id")
    private Long lockedOrderId;
    @TableField("used_order_id")
    private Long usedOrderId;
    @TableField("received_at")
    private LocalDateTime receivedAt;
    @TableField("locked_at")
    private LocalDateTime lockedAt;
    @TableField("used_at")
    private LocalDateTime usedAt;
    @TableField("start_at")
    private LocalDateTime startAt;
    @TableField("end_at")
    private LocalDateTime endAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
