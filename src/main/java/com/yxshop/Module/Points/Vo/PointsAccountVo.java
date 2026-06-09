package com.yxshop.Module.Points.Vo;

import lombok.Data;

@Data
public class PointsAccountVo {
    private Long userId;
    private Integer currentPoints;
    private Integer totalEarned;
    private Integer totalSpent;
    private String expireDate;
}
