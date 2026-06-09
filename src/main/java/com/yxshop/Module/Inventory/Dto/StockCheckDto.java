package com.yxshop.Module.Inventory.Dto;

import lombok.Data;

@Data
public class StockCheckDto {
    private Long productId;
    private Integer quantity;
}
