package com.yxshop.Module.Order.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItemEntity {
    @TableId
    private Long id;
    @TableField("order_id")
    private Long orderId;
    @TableField("product_id")
    private Long productId;
    @TableField("shop_id")
    private Long shopId;
    @TableField("product_name")
    private String productName;
    @TableField("product_image")
    private String productImage;
    @TableField("specs_text")
    private String specsText;
    private BigDecimal price;
    private Integer quantity;
    @TableField("refund_quantity")
    private Integer refundQuantity;
    @TableField("item_status")
    private String itemStatus;
    @TableField("after_sales_status")
    private String afterSalesStatus;
    @TableField("review_status")
    private String reviewStatus;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
