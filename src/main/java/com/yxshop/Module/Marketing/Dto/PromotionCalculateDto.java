package com.yxshop.Module.Marketing.Dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PromotionCalculateDto {
    private Long userId;
    private Long couponId;
    private List<PromotionItemDto> items = new ArrayList<>();
}
