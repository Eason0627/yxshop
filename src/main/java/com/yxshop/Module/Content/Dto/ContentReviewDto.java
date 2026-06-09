package com.yxshop.Module.Content.Dto;

import lombok.Data;

@Data
public class ContentReviewDto {
    private Long postId;
    private String auditStatus;
    private Integer status;
}
