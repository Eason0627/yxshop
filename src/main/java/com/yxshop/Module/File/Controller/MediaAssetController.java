package com.yxshop.Module.File.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yxshop.Module.File.Service.MediaAssetService;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Utils.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class MediaAssetController {
    private final MediaAssetService mediaAssetService;
    private final ShopModuleMapper shopModuleMapper;

    public MediaAssetController(MediaAssetService mediaAssetService, ShopModuleMapper shopModuleMapper) {
        this.mediaAssetService = mediaAssetService;
        this.shopModuleMapper = shopModuleMapper;
    }

    @PostMapping("/upload")
    public Result upload(HttpServletRequest request,
                         @RequestParam("file") MultipartFile file,
                         @RequestParam(value = "bizType", required = false) String bizType) {
        Long uploadedBy = getCurrentUserIdOrNull(request);
        String role = getCurrentRoleOrNull(request);
        return Result.success(mediaAssetService.upload(file, uploadedBy, role, bizType));
    }

    @PostMapping("/refresh-sizes")
    public Result refreshSizes() {
        return Result.success(mediaAssetService.refreshZeroSizes());
    }

    @PostMapping("/import-url")
    public Result importFromUrl(HttpServletRequest request,
                                @RequestBody Map<String, String> body) {
        String url = body.get("url");
        String bizType = body.getOrDefault("bizType", "common");
        Long uploadedBy = getCurrentUserIdOrNull(request);
        String role = getCurrentRoleOrNull(request);
        return Result.success(mediaAssetService.importFromUrl(url, uploadedBy, role, bizType));
    }

    @GetMapping("/{id}")
    public Result get(@PathVariable Long id) {
        return Result.success(mediaAssetService.getById(id));
    }

    @GetMapping
    public Result list(HttpServletRequest request,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "20") Integer size,
                       @RequestParam(required = false) String bizType,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long shopId) {
        String role = getCurrentRoleOrNull(request);
        if ("ShopOwner".equals(role)) {
            // ShopOwner 只看本店资源，shopId 参数忽略
            shopId = resolveShopIdByUser(getCurrentUserIdOrNull(request));
        }
        // Admin 可通过 shopId 参数筛选某店铺图片（null 表示全部）
        return Result.success(mediaAssetService.list(page, size, bizType, keyword, shopId));
    }

    /**
     * Admin 专属：返回所有有图片的店铺列表（文件夹目录）
     * 返回格式：[{ shopId, shopName, imageCount }]
     */
    @GetMapping("/shops")
    public Result listShops(HttpServletRequest request) {
        if (!"Admin".equals(getCurrentRoleOrNull(request))) {
            return Result.error("无权限");
        }
        return Result.success(mediaAssetService.listShopFolders());
    }

    @DeleteMapping("/{id}")
    public Result delete(HttpServletRequest request,
                         @PathVariable Long id) {
        Long operatorId = getCurrentUserIdOrNull(request);
        return Result.success(mediaAssetService.delete(id, operatorId));
    }

    private Long getCurrentUserIdOrNull(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private String getCurrentRoleOrNull(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserRole");
        return value == null ? null : String.valueOf(value);
    }

    private Long resolveShopIdByUser(Long userId) {
        if (userId == null) return null;
        QueryWrapper<ShopEntity> qw = new QueryWrapper<>();
        qw.eq("owner_user_id", userId).eq("status", "Active").last("LIMIT 1");
        ShopEntity shop = shopModuleMapper.selectOne(qw);
        return shop == null ? null : shop.getShopId();
    }
}
