package com.yxshop.Module.Marketing.Dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ActivityDto {
    private Long id;
    private String type;
    private String title;
    private String subtitle;
    private String tag;
    private String tagColor;
    private String tagBg;
    private String image;
    private String discountText;
    private Long shopId;
    private String scopeType;
    private String productIds;
    private String productSnapshot;
    private Integer stackable;
    private Integer priority;
    private BigDecimal maxDiscount;
    private Integer status;
    /** ISO datetime string, e.g. "2024-06-01 00:00:00" or "2024-06-01T00:00:00" */
    private String startTime;
    private String endTime;
}
