package com.yxshop.Module.User.Vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddressVo {
    private Long addressId;
    private String receiverName;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private String fullAddress;
    private String postalCode;
    private String country;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Boolean defaultAddress;
}
