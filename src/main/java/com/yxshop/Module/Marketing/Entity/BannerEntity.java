package com.yxshop.Module.Marketing.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("banner")
public class BannerEntity {
    @TableId
    private Long id;
    private String title;
    private String image;
    @TableField("link_type")
    private String linkType;
    @TableField("link_target")
    private String linkTarget;
    private Integer sort;
    private Integer status;
    @TableField("start_at")
    private LocalDateTime startAt;
    @TableField("end_at")
    private LocalDateTime endAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
