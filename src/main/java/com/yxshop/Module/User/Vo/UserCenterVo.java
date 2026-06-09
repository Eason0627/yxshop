package com.yxshop.Module.User.Vo;

import lombok.Data;

@Data
public class UserCenterVo {
    private UserProfileVo profile;
    private Integer couponCount;
    private Integer currentPoints;
    private Integer favoriteCount;
    private Integer footprintCount;
    private Integer pendingPayCount;
    private Integer pendingShipCount;
    private Integer pendingReceiveCount;
    private Integer pendingReviewCount;
}
