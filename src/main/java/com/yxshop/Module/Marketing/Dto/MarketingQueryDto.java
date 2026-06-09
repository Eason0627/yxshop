package com.yxshop.Module.Marketing.Dto;

import lombok.Data;

@Data
public class MarketingQueryDto {
    private Long shopId;
    private String type;
    private String keyword;
    private Integer status;
    private String startDate;
    private String endDate;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
