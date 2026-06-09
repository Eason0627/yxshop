package com.yxshop.Module.Order.Dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderCreateDto {
    private List<Long> cartItemIds;
    private List<OrderCreateItemDto> items;
    private Long addressId;
    private Long couponId;
    private String addressSnapshot;
    private String buyerRemark;
}
