package com.yxshop.Module.Marketing.Dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromotionItemDto {
    private Long productId;
    private Long shopId;
    private BigDecimal price;
    private Integer quantity;
}
