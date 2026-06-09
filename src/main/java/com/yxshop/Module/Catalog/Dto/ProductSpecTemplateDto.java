package com.yxshop.Module.Catalog.Dto;

import lombok.Data;

@Data
public class ProductSpecTemplateDto {
    private Long id;
    private String name;
    private Long categoryId;
    private Long shopId;
    private String specs;
    private Integer status;
}
