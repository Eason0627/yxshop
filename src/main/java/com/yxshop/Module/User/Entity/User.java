package com.yxshop.Module.User.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author : hym
 * @date : 2024/7/15 10:29
 * @Version: 1.0
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id; // 用户ID
    private String username; // 用户名
    private String nick_name; // 昵称
    private String password; // 密码
    private String email; // 电子邮件，校验邮箱格式
    private String phone; // 手机号码，校验格式
    private String role; // 用户身份 Admin, ShopOwner, Customer;
    private String ID_card; // 身份证号码
    private String real_name; // 真实姓名
    private String wechat_id; // 微信号
    private Boolean is_verified; // 是否实名
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createTime; // 创建时间
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updateTime; // 更新时间
    private String status; // 用户状态，Active, Inactive, Invalid;
    private String avatar; // 头像
    private Date birth; // 生日
    private String gender;//性别 Male, Female, Other;
    private Long default_address;//地址id
}
