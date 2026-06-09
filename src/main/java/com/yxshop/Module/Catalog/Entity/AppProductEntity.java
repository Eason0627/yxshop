package com.yxshop.Module.Catalog.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class AppProductEntity {
    @TableId
    private Long id;
    private String name;
    @TableField("product_code")
    private String productCode;
    private String subtitle;
    @TableField("category_id")
    private Long categoryId;
    @TableField("shop_id")
    private Long shopId;
    @TableField("brand_shop_id")
    private Long brandShopId;
    private BigDecimal price;
    @TableField("original_price")
    private BigDecimal originalPrice;
    private Integer stock;
    private java.math.BigDecimal weight;
    @TableField("shipping_type")
    private String shippingType;
    @TableField("shipping_fee")
    private BigDecimal shippingFee;
    @TableField("ship_from")
    private String shipFrom;
    @TableField("warehouse_id")
    private Long warehouseId;
    @TableField("shipping_days")
    private Integer shippingDays;
    @TableField("purchase_limit")
    private Integer purchaseLimit;
    private Integer moq;
    @TableField("video_url")
    private String videoUrl;
    private Integer sales;
    private BigDecimal rating;
    private Integer likes;
    @TableField("main_image")
    private String mainImage;
    private String images;
    private String tag;
    @TableField("tag_color")
    private String tagColor;
    @TableField("tag_bg")
    private String tagBg;
    private String description;
    private String features;
    private String specs;
    private Integer status;
    @TableField("audit_status")
    private String auditStatus;
    @TableField("audit_reason")
    private String auditReason;
    @TableField("reviewed_by")
    private Long reviewedBy;
    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
