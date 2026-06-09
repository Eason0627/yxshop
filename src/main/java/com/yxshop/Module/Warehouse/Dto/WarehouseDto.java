package com.yxshop.Module.Warehouse.Dto;

import lombok.Data;

@Data
public class WarehouseDto {
    private Long id;
    private String name;
    private String contact;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Integer isDefault;
    private Double lng;
    private Double lat;
}
