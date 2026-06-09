package com.yxshop.Module.User.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("address")
public class AddressEntity {
    @TableId("address_id")
    private Long addressId;
    @TableField("user_id")
    private Long userId;
    @TableField("recipient_name")
    private String recipientName;
    @TableField("receiver_name")
    private String receiverName;
    private String phone;
    @TableField("state_province")
    private String stateProvince;
    private String province;
    private String city;
    private String district;
    @TableField("street_address")
    private String streetAddress;
    private String detail;
    @TableField("full_address")
    private String fullAddress;
    @TableField("postal_code")
    private String postalCode;
    private String country;
    private BigDecimal longitude;
    private BigDecimal latitude;
    @TableField("is_default")
    private Boolean defaultAddress;
    private Integer status;
    @TableField("createTime")
    private LocalDateTime createTime;
    @TableField("updateTime")
    private LocalDateTime updateTime;
}
