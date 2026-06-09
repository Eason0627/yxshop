package com.yxshop.Module.User.Entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {// 收货地址

    @JsonProperty("address_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JSONField(serializeUsing = ToStringSerializer.class)
    private Long address_id; // 收货地址ID

    @JsonProperty("user_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JSONField(serializeUsing = ToStringSerializer.class)
    private Long user_id; // 用户ID

    private String recipient_name; // 收件人姓名
    private String street_address; // 街道地址
    private String city; // 城市
    private String state_province; // 州或省份
    private String postal_code; // 邮政编码
    private String country; // 国家
    private String phone; // 联系电话
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Boolean is_default; // 是否为默认收货地址
}