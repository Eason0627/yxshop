package com.yxshop.Module.Search.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("search_hotword")
public class SearchHotwordEntity {
    @TableId
    private Long id;
    private String keyword;
    @TableField("search_count")
    private Integer searchCount;
    private Integer weight;
    private Integer status;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
