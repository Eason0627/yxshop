package com.yxshop.Module.User.Vo;

import lombok.Data;

@Data
public class UserProfileVo {
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String nickname;
    private String phone;
    private String memberId;
    private String memberLevel;
    private Integer registeredDays;
    private Integer gender;
    private String birthday;
    private String bio;
    private String avatar;
}
