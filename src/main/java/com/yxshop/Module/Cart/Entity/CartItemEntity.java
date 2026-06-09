package com.yxshop.Module.Cart.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("cart_item")
public class CartItemEntity {
    @TableId
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("product_id")
    private Long productId;
    @TableField("shop_id")
    private Long shopId;
    @TableField("shop_name")
    private String shopName;
    @TableField("product_name")
    private String productName;
    private BigDecimal price;
    @TableField("original_price")
    private BigDecimal originalPrice;
    private String image;
    @TableField("specs_text")
    private String specsText;
    private Integer quantity;
    private Integer selected;
    private Integer stock;
    @TableField("is_invalid")
    private Integer isInvalid;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
