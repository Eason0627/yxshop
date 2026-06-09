package com.yxshop.Module.Payment.Dto;

import lombok.Data;

@Data
public class PaymentCallbackDto {
    private String paymentNo;
    private Long orderId;
    private String status;
    private String payload;
}
