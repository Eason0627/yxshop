package com.yxshop.Module.Order.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_info")
public class OrderEntity {
    @TableId("order_id")
    private Long orderId;
    @TableField("order_number")
    private String orderNumber;
    @TableField("customer_id")
    private Long customerId;
    @TableField("shop_id")
    private Long shopId;
    @TableField("shop_name")
    private String shopName;
    @TableField("goods_amount")
    private BigDecimal goodsAmount;
    @TableField("order_total")
    private BigDecimal orderTotal;
    @TableField("order_status")
    private String orderStatus;
    @TableField("payment_status")
    private String paymentStatus;
    @TableField("fulfillment_status")
    private String fulfillmentStatus;
    @TableField("after_sales_status")
    private String afterSalesStatus;
    @TableField("address_id")
    private Long addressId;
    @TableField("coupon_id")
    private Long couponId;
    @TableField("coupon_amount")
    private BigDecimal couponAmount;
    @TableField("activity_discount")
    private BigDecimal activityDiscount;
    @TableField("discount_amount")
    private BigDecimal discountAmount;
    @TableField("items_snapshot")
    private String itemsSnapshot;
    @TableField("address_snapshot")
    private String addressSnapshot;
    @TableField("buyer_remark")
    private String buyerRemark;
    @TableField("admin_remark")
    private String adminRemark;
    @TableField("paid_at")
    private LocalDateTime paidAt;
    @TableField("cancelled_at")
    private LocalDateTime cancelledAt;
    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;
    @TableField("shipping_company")
    private String shippingCompany;
    @TableField("tracking_no")
    private String trackingNo;
    @TableField("shipped_at")
    private LocalDateTime shippedAt;
    @TableField("shipping_remark")
    private String shippingRemark;
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
    @TableField("createTime")
    private LocalDateTime createTime;
    @TableField("updateTime")
    private LocalDateTime updateTime;
}
