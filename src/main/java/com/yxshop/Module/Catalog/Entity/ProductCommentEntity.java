package com.yxshop.Module.Catalog.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_comment")
public class ProductCommentEntity {
    @TableId
    private Long id;
    @TableField("product_id")
    private Long productId;
    @TableField("user_id")
    private Long userId;
    @TableField("order_id")
    private Long orderId;
    private Integer rating;
    private String content;
    private String images;
    private String reply;
    @TableField("replied_at")
    private LocalDateTime repliedAt;
    private Integer status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
