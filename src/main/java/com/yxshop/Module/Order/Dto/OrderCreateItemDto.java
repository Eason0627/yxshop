package com.yxshop.Module.Order.Dto;

import lombok.Data;

@Data
public class OrderCreateItemDto {
    private Long productId;
    private String specsText;
    private Integer quantity;
}
