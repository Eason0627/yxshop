package com.yxshop.Module.Payment.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_record")
public class PaymentRecordEntity {
    @TableId
    private Long id;
    @TableField("payment_no")
    private String paymentNo;
    @TableField("order_id")
    private Long orderId;
    @TableField("user_id")
    private Long userId;
    private BigDecimal amount;
    @TableField("pay_method")
    private String payMethod;
    private String status;
    @TableField("callback_payload")
    private String callbackPayload;
    @TableField("paid_at")
    private LocalDateTime paidAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
