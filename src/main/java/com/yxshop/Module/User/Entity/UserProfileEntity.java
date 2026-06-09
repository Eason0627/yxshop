package com.yxshop.Module.User.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_profile")
public class UserProfileEntity {
    @TableId("user_id")
    private Long userId;
    private String nickname;
    private String phone;
    @TableField("member_id")
    private String memberId;
    @TableField("member_level")
    private String memberLevel;
    @TableField("registered_at")
    private LocalDateTime registeredAt;
    private Integer gender;
    private LocalDate birthday;
    private String bio;
    private String avatar;
    private Integer status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
