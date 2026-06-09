package com.yxshop.Module.User.Dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;

/**
 * @author : hym
 * @date : 2024/8/16 13:03
 * @Version: 1.0
 */

@Data
public class LoginParam { // 登录需要传递 Params参数
    private String username; // 用户名

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String phone; // 手机号码

    @Email
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "电子邮件格式不正确")
    private String email; // 电子邮件

    private String password; // 密码

    private String code; // 登录验证码
}
