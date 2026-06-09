package com.yxshop.Module.Shop.Vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ShopVo {
    private Long shopId;
    private String shopName;
    private String displayName;
    private Long ownerUserId;
    private String ownerName;
    private String phone;
    private String location;
    private String shopDescription;
    private String shopImage;
    private String avatar;
    private String logo;
    private String banner;
    private String tags;
    private String status;
    private Integer productCount;
    private Integer followers;
    private Integer sales;
    private String rating;
    private String serviceScore;
    private String logisticsScore;
    private String qualityScore;
    private String discountLabel;
    private Boolean brandShop;
    private Long pendingProcessId;
    private Map<String, Object> decoration;
    private List<?> products;
    private List<?> activities;
    private List<?> coupons;
}
