package com.yxshop.Module.Marketing.Dto;

import lombok.Data;

@Data
public class ChannelDto {
    private Long id;
    private String name;
    private String icon;
    private String iconColor;
    private String bgColor;
    private String path;
    private Integer sort;
    private Integer status;
}
