package com.yxshop.Module.Content.Vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentCommentVo {
    private Long id;
    private Long postId;
    private Long userId;
    private String authorName;
    private String authorAvatar;
    private Long parentId;
    private String content;
    private Integer likeCount;
    private Integer status;
    private LocalDateTime createdAt;
}
