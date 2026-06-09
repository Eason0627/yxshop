package com.yxshop.Module.Catalog.Dto;

import lombok.Data;

@Data
public class ProductReviewDto {
    private Long productId;
    private String auditStatus;
    private String auditReason;
}
