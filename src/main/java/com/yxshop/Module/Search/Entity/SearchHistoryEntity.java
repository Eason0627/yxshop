package com.yxshop.Module.Search.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("search_history")
public class SearchHistoryEntity {
    @TableId
    private Long id;
    @TableField("user_id")
    private Long userId;
    private String keyword;
    @TableField("result_count")
    private Integer resultCount;
    @TableField("searched_at")
    private LocalDateTime searchedAt;
}
