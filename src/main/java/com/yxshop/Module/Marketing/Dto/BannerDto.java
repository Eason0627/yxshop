package com.yxshop.Module.Marketing.Dto;

import lombok.Data;

@Data
public class BannerDto {
    private Long id;
    private String title;
    private String image;
    private String linkType;
    private String linkTarget;
    private Integer sort;
    private Integer status;
}
