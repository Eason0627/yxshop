package com.yxshop.Module.Catalog.Dto;

import lombok.Data;

@Data
public class CategoryDto {
    private Long categoryId;       // 编辑时传入
    private String categoryName;
    private Long parentCategoryId; // null 表示顶级分类
    private String description;
    private String imageUrl;
    private String icon;
    private String banner;
    private Integer sort;
    private Integer status;        // 1上架 0下架
}
