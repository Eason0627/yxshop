package com.yxshop.Module.Catalog.Dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AppProductDto {
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
}
