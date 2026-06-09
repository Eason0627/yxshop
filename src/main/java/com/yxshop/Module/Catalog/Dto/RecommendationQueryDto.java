package com.yxshop.Module.Catalog.Dto;

import lombok.Data;

@Data
public class RecommendationQueryDto {
    private String scene;
    private Long channelId;
    private Long activityId;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
