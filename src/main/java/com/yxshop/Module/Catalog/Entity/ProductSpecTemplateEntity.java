package com.yxshop.Module.Catalog.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_spec_template")
public class ProductSpecTemplateEntity {
    @TableId
    private Long id;
    private String name;
    @TableField("category_id")
    private Long categoryId;
    @TableField("shop_id")
    private Long shopId;
    private String specs;
    private Integer status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
