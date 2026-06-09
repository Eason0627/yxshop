package com.yxshop.Module.AfterSales.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("after_sales")
public class AfterSalesEntity {
    @TableId("asr_id")
    private Long asrId;
    @TableField("order_id")
    private Long orderId;
    @TableField("customer_id")
    private Long customerId;
    @TableField("product_id")
    private Long productId;
    @TableField("issue_type")
    private String issueType;
    @TableField("issue_description")
    private String issueDescription;
    private String status;
    @TableField("resolution_notes")
    private String resolutionNotes;
    @TableField("createTime")
    private LocalDateTime createTime;
    @TableField("updateTime")
    private LocalDateTime updateTime;
}
