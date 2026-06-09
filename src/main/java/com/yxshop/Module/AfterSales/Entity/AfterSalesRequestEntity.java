package com.yxshop.Module.AfterSales.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("after_sales_request")
public class AfterSalesRequestEntity {
    @TableId
    private Long id;
    @TableField("order_id")
    private Long orderId;
    @TableField("order_item_id")
    private Long orderItemId;
    @TableField("user_id")
    private Long userId;
    @TableField("shop_id")
    private Long shopId;
    @TableField("product_id")
    private Long productId;
    private String type;
    private Integer quantity;
    private BigDecimal amount;
    private String reason;
    private String description;
    private String images;
    @TableField("return_carrier_name")
    private String returnCarrierName;
    @TableField("return_tracking_no")
    private String returnTrackingNo;
    @TableField("exchange_order_id")
    private Long exchangeOrderId;
    @TableField("refund_id")
    private Long refundId;
    private String status;
    @TableField("reviewer_id")
    private Long reviewerId;
    @TableField("review_remark")
    private String reviewRemark;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
