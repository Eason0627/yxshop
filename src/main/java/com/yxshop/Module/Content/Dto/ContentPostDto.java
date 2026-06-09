package com.yxshop.Module.Content.Dto;

import lombok.Data;

@Data
public class ContentPostDto {
    private String title;
    private String content;
    private String coverImage;
    private String images;
    private String videoUrl;
    private String tags;
    private String music;
    private Long topicId;
    private Long productId;
    private String productName;
    private java.math.BigDecimal productPrice;
    private String productImage;
    private String type;
}
