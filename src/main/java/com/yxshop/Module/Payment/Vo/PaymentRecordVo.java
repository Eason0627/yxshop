package com.yxshop.Module.Payment.Vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRecordVo {
    private Long id;
    private Long orderId;
    private String payNo;
    private BigDecimal amount;
    private String status;
}
