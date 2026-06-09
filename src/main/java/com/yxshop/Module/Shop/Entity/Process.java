package com.yxshop.Module.Shop.Entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @author hym
 * @since 2024-08-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("process")
public class Process implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审核表id
     */
    @TableId(value = "process_id", type = IdType.ASSIGN_ID)
    private Long process_id;

    /**
     * 审核类型: '申请开店','注销店铺','签约品牌','退货退款','店铺提现'，‘用户提现’，‘保证金退款’
     */
    private String process_type;



    /**
     * 申请人id
     */
    private Long application_id;

    /**
     * 影响对象id
     */
    private Long effect_id;

    /**
     * 影响数据表名
     */
    private String effect_schema;

    /**
     * 审核人id
     */
    private Long reviewer_id;

    /**
     * 审核提交时间
     */
    private LocalDateTime application_date;

    /**
     * 审核通过时间
     */
    private LocalDateTime review_date;

    /**
     * 审核意见
     */
    private String remark;

    /**
     * 审核状态
     */
    private String status;//0:未审核，1：审核通过，2：审核不通过

    private String description;//审核描述

    /**
     * 创建时间
     */
    private LocalDateTime create_time;

    /**
     * 更新时间
     */
    private LocalDateTime update_time;


}
