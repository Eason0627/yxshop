package com.yxshop.Module.Catalog.Vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class AppProductVo {
    private Long id;
    private String name;
    private String productCode;
    private String subtitle;
    private Long categoryId;
    private Long shopId;
    private Long brandShopId;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private BigDecimal weight;
    private String shippingType;
    private BigDecimal shippingFee;
    private String shipFrom;
    private Long warehouseId;
    private Integer shippingDays;
    private Integer purchaseLimit;
    private Integer moq;
    private String videoUrl;
    private Integer sales;
    private BigDecimal rating;
    private Integer likes;
    private String mainImage;
    private String images;
    private String tag;
    private String tagColor;
    private String tagBg;
    private String description;
    private String features;
    private String specs;
    private Integer status;
    private String auditStatus;
    private String auditReason;
    private Integer commentCount;
    private Integer availableStock;
    private Boolean favorited;
    private List<ProductCommentVo> comments;
    private Map<String, Object> promotions;
    private String shopName;
    private String shopLogo;
}
