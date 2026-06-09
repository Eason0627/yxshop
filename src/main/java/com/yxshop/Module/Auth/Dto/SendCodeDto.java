package com.yxshop.Module.Auth.Dto;

import lombok.Data;

@Data
public class SendCodeDto {
    private String account;
    private String email;
    private String phone;
    private String scene;
}
