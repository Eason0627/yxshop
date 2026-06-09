package com.yxshop.Module.Shop.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author hym
 * @since 2024-08-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("shop")
public class Shop implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *  店铺ID，作为主键
     */
    @TableId(value = "shop_id", type = IdType.ASSIGN_ID)
    private Long shop_id;

    /**
     * 店铺名称
     */
    private String shop_name;

    /**
     * 店铺负责人的用户ID，作为外键引用User表
     */
    private Long owner_user_id;

    /**
     * 联系信息
     */
    private String phone;

    /**
     * 地址
     */
    private String location;

    /**
     * 注册日期
     */
    private LocalDate registration_date;

    /**
     * 店铺描述
     */
    private String shop_description;

    /**
     * 店铺图片的URL
     */
    private String shop_image;

    /**
     * 创建时间，自动填充
     */
    @TableField("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间，自动更新
     */
    @TableField("updateTime")
    private LocalDateTime updateTime;

    /**
     * 店铺状态
     */
    private String status;
}
