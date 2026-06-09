package com.yxshop.Module.Warehouse.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("warehouse")
public class WarehouseEntity {
    @TableId
    private Long id;
    @TableField("shop_id")
    private Long shopId;
    private String name;
    private String contact;
    private String phone;
    private String province;
    private String city;
    private String district;
    @TableField("detail_address")
    private String detailAddress;
    @TableField("is_default")
    private Integer isDefault;
    private String status;
    private Double lng;
    private Double lat;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
