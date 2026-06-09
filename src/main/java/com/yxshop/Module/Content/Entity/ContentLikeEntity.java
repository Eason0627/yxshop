package com.yxshop.Module.Content.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_like")
public class ContentLikeEntity {
    @TableId
    private Long id;
    @TableField("post_id")
    private Long postId;
    @TableField("user_id")
    private Long userId;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
