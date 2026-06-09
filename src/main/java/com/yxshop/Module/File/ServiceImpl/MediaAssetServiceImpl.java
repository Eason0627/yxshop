package com.yxshop.Module.File.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxshop.Module.File.Entity.MediaAssetEntity;
import com.yxshop.Module.File.Mapper.MediaAssetMapper;
import com.yxshop.Module.File.Service.MediaAssetService;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Utils.AliOSSUtils;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MediaAssetServiceImpl implements MediaAssetService {

    private final MediaAssetMapper mediaAssetMapper;
    private final AliOSSUtils aliOSSUtils;
    private final ShopModuleMapper shopModuleMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(31, 1);

    public MediaAssetServiceImpl(MediaAssetMapper mediaAssetMapper, AliOSSUtils aliOSSUtils,
                                 ShopModuleMapper shopModuleMapper) {
        this.mediaAssetMapper = mediaAssetMapper;
        this.aliOSSUtils = aliOSSUtils;
        this.shopModuleMapper = shopModuleMapper;
    }

    /** 将 shopId 后 8 位作为 OSS 路径前缀（方案 B） */
    private String shopPrefix(Long shopId) {
        return String.format("%08d", Math.abs(shopId % 100_000_000L));
    }

    @Override
    public Object upload(MultipartFile file, Long uploadedBy, String role, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        try {
            // ShopOwner：查出本店 shopId，用后 8 位作为 OSS 路径前缀
            Long shopId = null;
            String prefix = null;
            if ("ShopOwner".equals(role) && uploadedBy != null) {
                QueryWrapper<ShopEntity> qw = new QueryWrapper<>();
                qw.eq("owner_user_id", uploadedBy).eq("status", "Active").last("LIMIT 1");
                ShopEntity shop = shopModuleMapper.selectOne(qw);
                if (shop != null) {
                    shopId = shop.getShopId();
                    prefix = shopPrefix(shopId);
                }
            }
            String objectKey = aliOSSUtils.upload(file, prefix);

            MediaAssetEntity asset = new MediaAssetEntity();
            asset.setId(idWorker.nextId());
            asset.setFileName(file.getOriginalFilename());
            asset.setFileUrl(objectKey);
            asset.setFileExt(getExtension(file.getOriginalFilename()));
            asset.setFileSize(file.getSize());
            asset.setMimeType(file.getContentType());
            asset.setBizType(bizType != null ? bizType : "common");
            asset.setUploadedBy(uploadedBy);
            asset.setShopId(shopId);
            asset.setStatus(1);
            asset.setCreatedAt(LocalDateTime.now());
            mediaAssetMapper.insert(asset);

            // 为刚上传的图片生成 2 小时预签名 URL 供前端立即展示
            String freshUrl = aliOSSUtils.generatePresignedUrl(objectKey, 120);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", asset.getId());
            result.put("objectKey", objectKey);
            result.put("url", freshUrl != null ? freshUrl : objectKey);
            result.put("fileName", asset.getFileName());
            result.put("fileSize", asset.getFileSize());
            return result;
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public Object getById(Long id) {
        MediaAssetEntity asset = mediaAssetMapper.selectById(id);
        if (asset == null) throw new RuntimeException("资源不存在");
        String objectKey = aliOSSUtils.extractObjectKey(asset.getFileUrl());
        String freshUrl = aliOSSUtils.generatePresignedUrl(objectKey, 120);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", asset.getId());
        data.put("fileName", asset.getFileName());
        data.put("objectKey", objectKey);
        data.put("fileUrl", freshUrl != null ? freshUrl : asset.getFileUrl());
        data.put("fileSize", asset.getFileSize());
        data.put("mimeType", asset.getMimeType());
        data.put("bizType", asset.getBizType());
        data.put("createdAt", asset.getCreatedAt());
        return data;
    }

    @Override
    public Object list(Integer page, Integer size, String bizType, String keyword, Long shopId) {
        LambdaQueryWrapper<MediaAssetEntity> wrapper = new LambdaQueryWrapper<MediaAssetEntity>()
                .eq(MediaAssetEntity::getStatus, 1)
                .orderByDesc(MediaAssetEntity::getCreatedAt);
        if (bizType != null && !bizType.trim().isEmpty()) {
            wrapper.eq(MediaAssetEntity::getBizType, bizType);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(MediaAssetEntity::getFileName, keyword.trim());
        }
        if (shopId != null) {
            wrapper.eq(MediaAssetEntity::getShopId, shopId);
        }

        Page<MediaAssetEntity> result = mediaAssetMapper.selectPage(
                new Page<>(safePage(page), safeSize(size)), wrapper);
        List<MediaAssetEntity> entities = result.getRecords();

        // 批量生成 2 小时预签名 URL（只用于选图器预览，不会被持久化到业务表）
        List<String> objectKeys = new ArrayList<>();
        for (MediaAssetEntity e : entities) {
            objectKeys.add(aliOSSUtils.extractObjectKey(e.getFileUrl()));
        }
        Map<String, String> presignedMap = aliOSSUtils.generatePresignedUrls(objectKeys, 120);

        List<Map<String, Object>> records = new ArrayList<>();
        for (MediaAssetEntity asset : entities) {
            String key = aliOSSUtils.extractObjectKey(asset.getFileUrl());
            String freshUrl = presignedMap.getOrDefault(key, asset.getFileUrl());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", asset.getId());
            item.put("fileName", asset.getFileName());
            item.put("objectKey", key);          // 存入 objectKey 供前端持久化使用
            item.put("fileUrl", freshUrl);        // 2h 签名 URL 仅用于选图器预览显示
            item.put("fileSize", asset.getFileSize());
            item.put("mimeType", asset.getMimeType());
            item.put("bizType", asset.getBizType());
            item.put("createdAt", asset.getCreatedAt());
            records.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return data;
    }

    @Override
    public Object importFromUrl(String url, Long uploadedBy, String role, String bizType) {
        if (url == null || url.isBlank()) throw new RuntimeException("URL 不能为空");
        // 简单防止 SSRF：只允许 http/https
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new RuntimeException("仅支持 http/https URL");
        }
        try {
            Long shopId = null;
            String prefix = null;
            if ("ShopOwner".equals(role) && uploadedBy != null) {
                QueryWrapper<ShopEntity> qw = new QueryWrapper<>();
                qw.eq("owner_user_id", uploadedBy).eq("status", "Active").last("LIMIT 1");
                ShopEntity shop = shopModuleMapper.selectOne(qw);
                if (shop != null) {
                    shopId = shop.getShopId();
                    prefix = shopPrefix(shopId);
                }
            }
            long[] outFileSize = {0L};
            String objectKey = aliOSSUtils.uploadFromUrl(url, prefix, outFileSize);

            // 文件名从 URL 路径末段取
            String fileName = url.contains("/")
                    ? url.substring(url.lastIndexOf('/') + 1)
                    : "imported.jpg";
            if (fileName.contains("?")) fileName = fileName.substring(0, fileName.indexOf('?'));
            if (fileName.isBlank()) fileName = "imported.jpg";

            MediaAssetEntity asset = new MediaAssetEntity();
            asset.setId(idWorker.nextId());
            asset.setFileName(fileName);
            asset.setFileUrl(objectKey);
            asset.setFileExt(objectKey.contains(".") ? objectKey.substring(objectKey.lastIndexOf('.') + 1) : "");
            asset.setFileSize(outFileSize[0]);
            asset.setMimeType("image/*");
            asset.setBizType(bizType != null ? bizType : "common");
            asset.setUploadedBy(uploadedBy);
            asset.setShopId(shopId);
            asset.setStatus(1);
            asset.setCreatedAt(LocalDateTime.now());
            mediaAssetMapper.insert(asset);

            String freshUrl = aliOSSUtils.generatePresignedUrl(objectKey, 120);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", asset.getId());
            result.put("objectKey", objectKey);
            result.put("url", freshUrl != null ? freshUrl : objectKey);
            result.put("fileName", fileName);
            result.put("fileSize", outFileSize[0]);
            return result;
        } catch (java.io.IOException e) {
            throw new RuntimeException("URL 图片导入失败: " + e.getMessage());
        }
    }

    @Override
    public Object delete(Long id, Long operatorId) {
        MediaAssetEntity asset = mediaAssetMapper.selectById(id);
        if (asset == null) throw new RuntimeException("资源不存在");
        // 先从 OSS 删除真实文件
        String objectKey = aliOSSUtils.extractObjectKey(asset.getFileUrl());
        aliOSSUtils.deleteFromOss(objectKey);
        // 再软删除 DB 记录
        asset.setStatus(0);
        mediaAssetMapper.updateById(asset);
        return "已删除";
    }

    @Override
    public Object refreshZeroSizes() {
        LambdaQueryWrapper<MediaAssetEntity> wrapper = new LambdaQueryWrapper<MediaAssetEntity>()
                .eq(MediaAssetEntity::getFileSize, 0L)
                .eq(MediaAssetEntity::getStatus, 1);
        List<MediaAssetEntity> zeroSizeAssets = mediaAssetMapper.selectList(wrapper);
        int updated = 0;
        for (MediaAssetEntity asset : zeroSizeAssets) {
            String objectKey = aliOSSUtils.extractObjectKey(asset.getFileUrl());
            if (objectKey == null || objectKey.isBlank()) continue;
            long size = aliOSSUtils.getObjectSize(objectKey);
            if (size > 0) {
                asset.setFileSize(size);
                mediaAssetMapper.updateById(asset);
                updated++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checked", zeroSizeAssets.size());
        result.put("updated", updated);
        return result;
    }

    @Override
    public Object listShopFolders() {
        // 查询所有有图片的 shop_id（包括 null = 平台公共资源）
        List<Map<String, Object>> rows = mediaAssetMapper.selectMaps(
                new QueryWrapper<MediaAssetEntity>()
                        .select("shop_id, COUNT(*) AS image_count")
                        .eq("status", 1)
                        .groupBy("shop_id")
                        .orderByAsc("shop_id"));

        List<Map<String, Object>> folders = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object shopIdObj = row.get("shop_id");
            Long shopId = shopIdObj == null ? null : Long.valueOf(String.valueOf(shopIdObj));
            int count = shopIdObj == null ? 0 : ((Number) row.getOrDefault("image_count", 0)).intValue();

            Map<String, Object> folder = new LinkedHashMap<>();
            folder.put("shopId", shopId);
            folder.put("imageCount", count);
            if (shopId == null) {
                folder.put("shopName", "公共资源");
                folder.put("displayName", "公共资源");
            } else {
                ShopEntity shop = shopModuleMapper.selectById(shopId);
                folder.put("shopName", shop != null ? shop.getShopName() : "未知店铺");
                folder.put("displayName", shop != null ?
                        (shop.getDisplayName() != null ? shop.getDisplayName() : shop.getShopName()) : "未知店铺");
            }
            folders.add(folder);
        }
        return folders;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private long safePage(Integer page) { return page == null || page < 1 ? 1 : page; }
    private long safeSize(Integer size) { return size == null || size < 1 ? 20 : Math.min(size, 100); }
}
