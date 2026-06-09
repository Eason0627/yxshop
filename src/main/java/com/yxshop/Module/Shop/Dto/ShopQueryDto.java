package com.yxshop.Module.Shop.Dto;

import lombok.Data;

@Data
public class ShopQueryDto {
    private String keyword;
    private String status;
    private Boolean brandShop;
    private Long ownerUserId;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
