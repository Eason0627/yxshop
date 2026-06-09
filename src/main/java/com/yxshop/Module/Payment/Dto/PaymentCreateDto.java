package com.yxshop.Module.Payment.Dto;

import lombok.Data;

@Data
public class PaymentCreateDto {
    private Long orderId;
    private String payMethod;
}
