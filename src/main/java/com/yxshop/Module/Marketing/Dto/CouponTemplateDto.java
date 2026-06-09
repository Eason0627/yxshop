package com.yxshop.Module.Marketing.Dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponTemplateDto {
    private Long id;
    private String name;
    private String label;
    private String type;
    private BigDecimal value;
    private BigDecimal minAmount;
    private String description;
    private String sourceType;
    private Long shopId;
    private String scopeType;
    private String scopeIds;
    private Integer totalCount;
    private Integer remainingCount;
    private Integer limitPerUser;
    private Integer validDays;
    private String startTime;
    private String endTime;
    private Integer status;
}
