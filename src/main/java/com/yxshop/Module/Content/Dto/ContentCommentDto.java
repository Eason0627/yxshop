package com.yxshop.Module.Content.Dto;

import lombok.Data;

@Data
public class ContentCommentDto {
    private Long postId;
    private Long parentId;
    private String content;
}
