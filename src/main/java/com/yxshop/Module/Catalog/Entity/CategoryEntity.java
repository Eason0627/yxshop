package com.yxshop.Module.Catalog.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("category")
public class CategoryEntity {
    @TableId("category_id")
    private Long categoryId;
    @TableField("category_name")
    private String categoryName;
    @TableField("parent_category_id")
    private Long parentCategoryId;
    private String description;
    @TableField("image_url")
    private String imageUrl;
    @TableField("shop_id")
    private Long shopId;
    private Integer level;
    private String icon;
    private String banner;
    @TableField("product_count")
    private Integer productCount;
    private Integer sort;
    private Integer status;
    @TableField("createTime")
    private LocalDateTime createTime;
    @TableField("updateTime")
    private LocalDateTime updateTime;
}
