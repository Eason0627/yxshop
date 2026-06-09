package com.yxshop.Module.Content.Vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContentPostVo {
    private Long id;
    private Long userId;
    private String authorName;
    private String authorAvatar;
    // Frontend field aliases
    private String username;
    private String userAvatar;
    private String title;
    private String content;
    private String coverImage;
    private List<String> images;
    private String videoUrl;
    private String tags;
    private String music;
    private Long topicId;
    private String topicTitle;
    private String topicName; // alias for topicTitle
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private String productImage;
    private String type;
    private String auditStatus;
    private Integer status;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer viewCount;
    private Boolean liked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createTime; // alias for createdAt
}
