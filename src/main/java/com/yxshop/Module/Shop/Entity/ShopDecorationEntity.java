package com.yxshop.Module.Shop.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("shop_decoration")
public class ShopDecorationEntity {
    @TableId
    private Long id;
    @TableField("shop_id")
    private Long shopId;
    private String theme;
    private String modules;
    private String colors;
    private Integer status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
