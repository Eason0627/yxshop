package com.yxshop.Module.Content.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_post")
public class ContentPostEntity {
    @TableId
    private Long id;
    @TableField("user_id")
    private Long userId;
    private String title;
    private String content;
    @TableField("cover_image")
    private String coverImage;
    private String images;
    @TableField("video_url")
    private String videoUrl;
    private String tags;
    private String music;
    @TableField("topic_id")
    private Long topicId;
    @TableField("product_id")
    private Long productId;
    @TableField("product_name")
    private String productName;
    @TableField("product_price")
    private java.math.BigDecimal productPrice;
    @TableField("product_image")
    private String productImage;
    private String type;
    @TableField("audit_status")
    private String auditStatus;
    private Integer status;
    @TableField("like_count")
    private Integer likeCount;
    @TableField("comment_count")
    private Integer commentCount;
    @TableField("share_count")
    private Integer shareCount;
    @TableField("view_count")
    private Integer viewCount;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
