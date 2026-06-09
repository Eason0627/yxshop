package com.yxshop.Module.Cart.Dto;

import lombok.Data;

import java.util.List;

@Data
public class CartSelectionDto {
    private List<Long> cartItemIds;
    private Integer selected;
}
