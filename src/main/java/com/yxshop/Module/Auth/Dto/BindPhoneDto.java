package com.yxshop.Module.Auth.Dto;

import lombok.Data;

@Data
public class BindPhoneDto {
    private String phone;
    private String code;
    private String scene;
}
