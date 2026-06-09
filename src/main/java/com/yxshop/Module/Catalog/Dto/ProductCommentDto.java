package com.yxshop.Module.Catalog.Dto;

import lombok.Data;

@Data
public class ProductCommentDto {
    private Long productId;
    private Long orderId;
    private Long orderItemId;
    private Integer rating;
    private String content;
    private String images;
}
