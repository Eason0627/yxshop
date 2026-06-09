package com.yxshop.Module.Shop.Dto;

import lombok.Data;

@Data
public class ShopReviewDto {
    private Long processId;
    private Long shopId;
    private String status;
    private String remark;
}
