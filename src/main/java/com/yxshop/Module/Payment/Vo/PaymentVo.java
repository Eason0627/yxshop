package com.yxshop.Module.Payment.Vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentVo {
    private Long id;
    private String paymentNo;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String payMethod;
    private String status;
    private String paidAt;
    private String createdAt;
}
