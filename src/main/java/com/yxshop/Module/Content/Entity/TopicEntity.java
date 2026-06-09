package com.yxshop.Module.Content.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("topic")
public class TopicEntity {
    @TableId
    private Long id;
    private String title;
    @TableField("author_id")
    private Long authorId;
    @TableField("author_name")
    private String authorName;
    @TableField("cover_image")
    private String coverImage;
    private Integer likes;
    private Integer views;
    @TableField("content_blocks")
    private String contentBlocks;
    private Integer status;
    @TableField("published_at")
    private LocalDateTime publishedAt;
}
