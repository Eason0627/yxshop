package com.yxshop.Module.File.Entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_media_asset")
public class MediaAssetEntity {
    @TableId
    private Long id;
    @TableField("biz_type")
    private String bizType;
    @TableField("file_name")
    private String fileName;
    @TableField("file_url")
    private String fileUrl;
    @TableField("file_ext")
    private String fileExt;
    @TableField("file_size")
    private Long fileSize;
    @TableField("mime_type")
    private String mimeType;
    @TableField("uploaded_by")
    private Long uploadedBy;
    @TableField("shop_id")
    private Long shopId;
    private Integer status;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
