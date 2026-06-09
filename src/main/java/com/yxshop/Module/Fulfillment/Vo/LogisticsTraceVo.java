package com.yxshop.Module.Fulfillment.Vo;

import lombok.Data;

@Data
public class LogisticsTraceVo {
    private Long id;
    private String status;
    private String content;
    private String location;
    private Double lat;
    private Double lng;
    private String traceTime;
}
