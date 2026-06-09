package com.yxshop.Module.Refund.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_record")
public class RefundRecordEntity {
    @TableId
    private Long id;
    @TableField("refund_no")
    private String refundNo;
    @TableField("after_sales_id")
    private Long afterSalesId;
    @TableField("order_id")
    private Long orderId;
    @TableField("payment_id")
    private Long paymentId;
    @TableField("user_id")
    private Long userId;
    private BigDecimal amount;
    private String status;
    private String reason;
    @TableField("callback_payload")
    private String callbackPayload;
    @TableField("refunded_at")
    private LocalDateTime refundedAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
