package com.yxshop.Module.Warehouse.Vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarehouseVo {
    private Long id;
    private Long shopId;
    private String name;
    private String contact;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    /** 完整地址（省市区+详细，便于前端展示） */
    private String fullAddress;
    private Integer isDefault;
    private String status;
    private Double lng;
    private Double lat;
    private LocalDateTime createdAt;
}
