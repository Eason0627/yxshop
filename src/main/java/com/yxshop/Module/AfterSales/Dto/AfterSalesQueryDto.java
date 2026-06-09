package com.yxshop.Module.AfterSales.Dto;

import lombok.Data;

@Data
public class AfterSalesQueryDto {
    private Integer pageNum;
    private Integer pageSize;
    private String status;
    private String type;
    private Long shopId;
}
