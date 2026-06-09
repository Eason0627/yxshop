package com.yxshop.Module.File.Service;

import org.springframework.web.multipart.MultipartFile;

public interface MediaAssetService {
    /** 上传文件并记录（role="ShopOwner" 时自动查 shopId 并加路径前缀）*/
    Object upload(MultipartFile file, Long uploadedBy, String role, String bizType);

    /** 获取资源信息 */
    Object getById(Long id);

    /** 资源列表（ShopOwner 传 shopId 则只返回本店资源）*/
    Object list(Integer page, Integer size, String bizType, String keyword, Long shopId);

    /** 从外部 URL 下载图片并上传到 OSS，记录资源 */
    Object importFromUrl(String url, Long uploadedBy, String role, String bizType);

    /** 修正所有 fileSize=0 的记录，从 OSS HEAD 请求获取真实大小并写回 DB */
    Object refreshZeroSizes();

    /** 删除资源 */
    Object delete(Long id, Long operatorId);

    /** Admin 专属：统计各店铺图片数量，用于文件夹目录展示 */
    Object listShopFolders();
}
