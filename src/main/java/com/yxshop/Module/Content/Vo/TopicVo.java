package com.yxshop.Module.Content.Vo;

import lombok.Data;

@Data
public class TopicVo {
    private Long id;
    private String title;
    private String name;          // alias for title (frontend expects 'name')
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String coverImage;
    private String image;         // alias for coverImage (frontend expects 'image')
    private String description;   // not in entity — returned as null
    private Boolean isHot;        // not in entity — returned as false
    private Integer sort;         // not in entity — returned as 0
    private Integer postCount;    // computed or 0
    private Integer likes;
    private Integer views;
    private Integer viewCount;    // alias for views
    private String contentBlocks;
    private Integer status;
    private java.time.LocalDateTime publishedAt;
}
