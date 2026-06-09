package com.yxshop.Module.Points.Dto;

import lombok.Data;

@Data
public class PointsExchangeDto {
    private Long goodsId;
    private Integer quantity;
    private String addressSnapshot;
}
