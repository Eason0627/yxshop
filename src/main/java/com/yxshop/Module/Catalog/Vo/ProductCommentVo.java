package com.yxshop.Module.Catalog.Vo;

import lombok.Data;

@Data
public class ProductCommentVo {
    private Long id;
    private Long productId;
    private Long userId;
    private Long orderId;
    private Integer rating;
    private String content;
    private String images;
    private String createdAt;
}
