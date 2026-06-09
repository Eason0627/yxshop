package com.yxshop.Module.Marketing.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ranking_list")
public class RankingListEntity {
    @TableId
    private Long id;
    private String name;
    @TableField("category_name")
    private String categoryName;
    private String items;
    @TableField("update_time")
    private LocalDateTime updateTime;
    private Integer status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
