package com.yxshop.Module.Fulfillment.Dto;

import lombok.Data;

@Data
public class TraceDto {
    private Long orderId;
    private String status;
    private String content;
    private String location;
    /** 纬度（高德坐标系），地图选点时由前端提供 */
    private Double lat;
    /** 经度（高德坐标系） */
    private Double lng;
    /** 可选：自定义轨迹发生时间（ISO-8601，如 "2024-06-01T14:30:00"）。
     *  不传则默认取当前服务器时间。 */
    private String traceTime;
}
