package com.yxshop.Module.Shop.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@TableName("shop")
public class ShopEntity {
    @TableId("shop_id")
    private Long shopId;
    @TableField("shop_name")
    private String shopName;
    @TableField("display_name")
    private String displayName;
    @TableField("owner_user_id")
    private Long ownerUserId;
    private String phone;
    private String location;
    @TableField("registration_date")
    private LocalDate registrationDate;
    @TableField("shop_description")
    private String shopDescription;
    @TableField("shop_image")
    private String shopImage;
    private String avatar;
    private String logo;
    private String banner;
    private String tags;
    private BigDecimal rating;
    @TableField("product_count")
    private Integer productCount;
    private Integer followers;
    private Integer sales;
    @TableField("service_score")
    private BigDecimal serviceScore;
    @TableField("logistics_score")
    private BigDecimal logisticsScore;
    @TableField("quality_score")
    private BigDecimal qualityScore;
    @TableField("discount_label")
    private String discountLabel;
    @TableField("is_brand_shop")
    private Integer isBrandShop;
    private String status;
    @TableField("createTime")
    private LocalDateTime createTime;
    @TableField("updateTime")
    private LocalDateTime updateTime;
}
