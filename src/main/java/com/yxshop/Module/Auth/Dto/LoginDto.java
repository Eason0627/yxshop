package com.yxshop.Module.Auth.Dto;

import lombok.Data;

@Data
public class LoginDto {
    private String account;
    private String password;
    private String code;
    private String scene;
    /** 阿里云 CAPTCHA v2 验证参数（前端拼图完成后传入） */
    private String captchaVerifyParam;
}
