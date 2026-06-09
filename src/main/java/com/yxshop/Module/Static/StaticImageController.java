package com.yxshop.Module.Static;

import com.yxshop.Utils.AliOSSUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * App 静态图片代理：为 OSS 私有桶中的 YXshop/images/ 目录生成预签名 URL 并重定向。
 *
 * 浏览器访问 /static/images/{filename}  →  302 → OSS 预签名 URL（60 分钟有效）
 *
 * 前端 Nginx 将 /api/static/ 代理到此端点（路径透传）。
 * 无需鉴权（已在 MyInterceptor 中加入公开路径白名单）。
 */
@RestController
@RequestMapping("/static/images")
public class StaticImageController {

    private static final String OSS_PREFIX  = "YXshop/images/";
    private static final int    PRESIGN_MIN = 60;  // 预签名有效期（分钟）
    // 浏览器缓存时间略短于预签名有效期，避免用过期 URL 渲染
    private static final int    CACHE_SEC   = 3300; // 55 分钟

    private final AliOSSUtils aliOSSUtils;

    public StaticImageController(AliOSSUtils aliOSSUtils) {
        this.aliOSSUtils = aliOSSUtils;
    }

    @GetMapping("/{filename:.+}")
    public void redirect(@PathVariable String filename,
                         HttpServletResponse response) throws IOException {
        String objectKey = OSS_PREFIX + filename;
        String presignedUrl = aliOSSUtils.generatePresignedUrl(objectKey, PRESIGN_MIN);

        if (presignedUrl == null || presignedUrl.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "图片不存在");
            return;
        }

        // 让浏览器缓存这次重定向 55 分钟（比预签名时效短 5 分钟，留余量）
        response.setHeader("Cache-Control", "public, max-age=" + CACHE_SEC);
        response.sendRedirect(presignedUrl);
    }
}
