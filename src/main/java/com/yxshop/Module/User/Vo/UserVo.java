package com.yxshop.Module.User.Vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author : hym
 * @date : 2024/8/16 13:23
 * @Version: 1.0
 */

@Data
public class UserVo {
    private Long id; // 用户ID
    private String username; // 用户名
    private String nick_name; // 昵称
    private String real_name; // 真实姓名
    private String email; // 电子邮件，校验邮箱格式
    private String phone; // 手机号码，校验格式
    private String wechat_id; // 微信号
    private String avatar; // 头像
    private Date birth; // 生日
    private String gender;//性别 Male, Female, Other;
    private Long default_address_id;//地址id
    private String status; // 用户状态，Active, Inactive, Invalid;
    private String role; // 用户身份 Admin, ShopOwner, Customer;
    private Boolean is_verified; // 是否实名
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}
