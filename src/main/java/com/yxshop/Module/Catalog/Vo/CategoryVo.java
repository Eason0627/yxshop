package com.yxshop.Module.Catalog.Vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryVo {
    private Long categoryId;
    private String categoryName;
    private Long parentCategoryId;
    private String description;
    private String imageUrl;
    private String icon;
    private String banner;
    private Integer level;
    private Integer productCount;
    private Integer sort;
    private Integer status;
    private List<CategoryVo> children = new ArrayList<>();
}
