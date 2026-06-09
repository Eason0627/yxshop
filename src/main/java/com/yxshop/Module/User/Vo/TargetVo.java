package com.yxshop.Module.User.Vo;

import lombok.Data;

@Data
public class TargetVo {
    private Long id;
    private String targetType;
    private Long targetId;
    private String targetSnapshot;
    private String createdAt;
    private String viewAt;
}
