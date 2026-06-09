package com.yxshop.Module.Admin.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {

    private Integer id;//主键，用于唯一标识每条日志。

    private Long user_id;//执行操作的用户ID。
    private String operation;//操作描述，如“更新用户信息”、“删除订单”等。
    private String request_url;//请求的URL。
    private String method;//HTTP请求方法（GET, POST, PUT, DELETE等）
    private String params;//请求参数，可以是JSON格式
    private String ip;//用户IP地址。
    private String result_type;//返回值
    private String result_value;//返回值
    private LocalDateTime create_time;//日志创建时间。
    private Long cost_time;

}
