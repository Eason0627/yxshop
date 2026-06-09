package com.yxshop.Module.Cart.Dto;

import lombok.Data;

@Data
public class CartItemDto {
    private Long productId;
    private String specsText;
    private Integer quantity;
    private Integer selected;
}
