package com.yxshop.Module.User.Dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileDto {
    private Long userId;
    private String nickname;
    private String phone;
    private Integer gender;
    private LocalDate birthday;
    private String bio;
    private String avatar;
}
