package com.yxshop.Module.Refund.Dto;

import lombok.Data;

@Data
public class RefundCallbackDto {
    private Long refundId;
    private String refundNo;
    private String status;
    private String payload;
}
