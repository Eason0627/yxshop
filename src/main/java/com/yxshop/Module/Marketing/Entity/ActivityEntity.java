package com.yxshop.Module.Marketing.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity")
public class ActivityEntity {
    @TableId
    private Long id;
    private String type;
    private String title;
    private String subtitle;
    private String tag;
    @TableField("tag_color")
    private String tagColor;
    @TableField("tag_bg")
    private String tagBg;
    private String image;
    @TableField("discount_text")
    private String discountText;
    @TableField("hot_percent")
    private Integer hotPercent;
    private Integer participants;
    @TableField("shop_id")
    private Long shopId;
    @TableField("scope_type")
    private String scopeType;
    @TableField("product_ids")
    private String productIds;
    @TableField("product_snapshot")
    private String productSnapshot;
    private Integer stackable;
    private Integer priority;
    @TableField("max_discount")
    private java.math.BigDecimal maxDiscount;
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
