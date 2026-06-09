package com.yxshop.Utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 阿里云 OSS 工具类（私有桶，所有访问链接使用预签名URL）
 */
@Component
public class AliOSSUtils {

    @Autowired
    private AliOSSProperties aliOSSProperties;

    // ===== 内部创建 OSS 客户端 =====
    private OSS createOssClient() {
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(
                aliOSSProperties.getAccessKeyId(),
                aliOSSProperties.getAccessKeySecret()
        );
        return new OSSClientBuilder().build(aliOSSProperties.getEndpoint(), credentialsProvider);
    }

    /**
     * 上传文件到 OSS，返回对象键（objectKey），不生成签名 URL
     * 调用方按需通过 generatePresignedUrl() 获取可访问的 URL
     */
    public String upload(MultipartFile file) throws IOException {
        return upload(file, null);
    }

    /**
     * 上传文件到 OSS，支持可选路径前缀（用于店铺隔离）
     * prefix 示例："67832945"（shopId 后 8 位），生成 objectKey 如 "67832945/uuid.jpg"
     */
    public String upload(MultipartFile file, String prefix) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String filename = UUID.randomUUID() + ext;
        String objectKey = (prefix != null && !prefix.isBlank())
                ? prefix.replaceAll("/+$", "") + "/" + filename
                : filename;

        InputStream inputStream = file.getInputStream();
        OSS ossClient = createOssClient();
        try {
            ossClient.putObject(new PutObjectRequest(
                    aliOSSProperties.getBucketName(), objectKey, inputStream));
            return objectKey;
        } catch (OSSException | ClientException e) {
            throw new IOException("OSS 上传失败: " + e.getMessage(), e);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 为单个对象键生成预签名访问 URL（有效期 minutes 分钟）
     */
    public String generatePresignedUrl(String objectKey, int minutes) {
        OSS ossClient = createOssClient();
        try {
            Date expiration = new Date(System.currentTimeMillis() + (long) minutes * 60 * 1000);
            URL url = ossClient.generatePresignedUrl(
                    aliOSSProperties.getBucketName(), objectKey, expiration);
            return url.toString();
        } catch (Exception e) {
            return null;
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 批量生成预签名 URL（一次创建一个 OSS 客户端，减少开销）
     * 返回 objectKey → presignedUrl 的映射
     */
    public Map<String, String> generatePresignedUrls(List<String> objectKeys, int minutes) {
        Map<String, String> result = new LinkedHashMap<>();
        if (objectKeys == null || objectKeys.isEmpty()) return result;
        OSS ossClient = createOssClient();
        try {
            Date expiration = new Date(System.currentTimeMillis() + (long) minutes * 60 * 1000);
            for (String key : objectKeys) {
                try {
                    URL url = ossClient.generatePresignedUrl(
                            aliOSSProperties.getBucketName(), key, expiration);
                    result.put(key, url.toString());
                } catch (Exception e) {
                    result.put(key, null);
                }
            }
        } finally {
            ossClient.shutdown();
        }
        return result;
    }

    /**
     * 从对象键或任意格式预签名 URL 中提取 OSS 对象键
     * - 以 "/" 开头（代理路径如 /api/static/images/xxx.jpg）→ 返回空串（调用方不应生成预签名 URL）
     * - 已是裸对象键（无 "://"，无 "/" 开头）→ 直接返回
     * - 完整 URL → 解析 path 部分
     */
    public String extractObjectKey(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return "";
        // 代理路径（以 / 开头，如 /api/static/images/xxx.jpg）—— 不是 OSS 对象键，返回空串
        if (fileUrl.startsWith("/")) return "";
        // 已是裸对象键（不含协议头）
        if (!fileUrl.contains("://")) return fileUrl;
        try {
            URL url = new URL(fileUrl);
            String path = url.getPath(); // 例如 "/uuid.jpg"
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            String cleaned = fileUrl.contains("?") ? fileUrl.substring(0, fileUrl.indexOf("?")) : fileUrl;
            return cleaned.substring(cleaned.lastIndexOf('/') + 1);
        }
    }

    /**
     * 判断字符串是否为 OSS 对象键（需要生成签名 URL 才能访问）
     * 规则：
     *  - 含 "://"           → 完整 URL，不是对象键
     *  - 以 "/" 开头         → 代理路径，不是对象键
     *  - 不含 "." 且不含 "/" → 纯文本/CSS类名（如 ri-gift-fill），不是对象键
     *  - 其他               → 裸 OSS 对象键（如 uuid.jpg 或 YXshop/images/xxx.jpg）
     */
    public static boolean isObjectKey(String value) {
        if (value == null || value.isBlank()) return false;
        if (value.contains("://")) return false;
        if (value.startsWith("/")) return false;
        // 必须包含 "." 或 "/" 才算文件路径；纯 CSS 类名、颜色值等没有这两个字符
        return value.contains(".") || value.contains("/");
    }

    /**
     * 将前端回传的图片值归一化为可落库的格式：
     * - 已是裸 OSS objectKey（无 "://"）→ 原样返回
     * - 是我方 OSS 预签名 URL（包含 bucket 域名）→ 提取 objectKey
     * - 其他完整 URL（readdy.ai、CDN 等）→ 原样保留，不能破坏外链
     * - null / 空字符串 → 返回 null
     */
    public String normalizeForStorage(String value) {
        if (value == null || value.isBlank()) return null;
        if (!value.contains("://")) return value;                     // 已是 objectKey
        if (value.contains(aliOSSProperties.getBucketName())) {       // 我方 OSS URL
            return extractObjectKey(value);
        }
        return value;                                                  // 外链保留原样
    }

    /**
     * 从外部 URL 下载图片字节并上传到 OSS，返回 objectKey。
     * 下载超时 15s，最大 10 MB，仅接受 image/* 内容类型。
     */
    /** 查询 OSS 对象的实际字节大小（不下载内容，仅 HEAD 请求）；对象不存在时返回 -1 */
    public long getObjectSize(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return -1;
        OSS ossClient = createOssClient();
        try {
            com.aliyun.oss.model.ObjectMetadata meta =
                    ossClient.getObjectMetadata(aliOSSProperties.getBucketName(), objectKey);
            return meta.getContentLength();
        } catch (Exception e) {
            return -1;
        } finally {
            ossClient.shutdown();
        }
    }

    /** outFileSize[0] 会被写入实际下载字节数（可传 null 忽略） */
    public String uploadFromUrl(String externalUrl, String prefix, long[] outFileSize) throws IOException {
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(externalUrl).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        conn.connect();

        int status = conn.getResponseCode();
        if (status / 100 != 2) {
            throw new IOException("远程服务器返回 " + status);
        }

        String contentType = conn.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IOException("远程资源不是图片类型（Content-Type: " + contentType + "）");
        }

        // 从 Content-Type 或 URL 路径推断扩展名
        String ext = extFromContentType(contentType);
        if (ext == null || ext.isBlank()) {
            String path = new java.net.URL(externalUrl).getPath();
            int dot = path.lastIndexOf('.');
            ext = (dot > 0 && path.length() - dot <= 5) ? path.substring(dot) : ".jpg";
        }

        // 读取字节（限制 10 MB）
        byte[] buf = new byte[10 * 1024 * 1024 + 1];
        int totalRead = 0;
        try (InputStream in = conn.getInputStream()) {
            int n;
            while ((n = in.read(buf, totalRead, buf.length - totalRead)) > 0) {
                totalRead += n;
                if (totalRead > 10 * 1024 * 1024) {
                    throw new IOException("远程图片超过 10MB 限制");
                }
            }
        }

        String filename = UUID.randomUUID() + ext;
        String objectKey = (prefix != null && !prefix.isBlank())
                ? prefix.replaceAll("/+$", "") + "/" + filename
                : filename;

        if (outFileSize != null && outFileSize.length > 0) outFileSize[0] = totalRead;

        OSS ossClient = createOssClient();
        try {
            ossClient.putObject(new PutObjectRequest(
                    aliOSSProperties.getBucketName(), objectKey,
                    new java.io.ByteArrayInputStream(buf, 0, totalRead)));
            return objectKey;
        } catch (OSSException | ClientException e) {
            throw new IOException("OSS 上传失败: " + e.getMessage(), e);
        } finally {
            ossClient.shutdown();
        }
    }

    private String extFromContentType(String contentType) {
        if (contentType == null) return null;
        String ct = contentType.split(";")[0].trim().toLowerCase();
        switch (ct) {
            case "image/jpeg": return ".jpg";
            case "image/png":  return ".png";
            case "image/gif":  return ".gif";
            case "image/webp": return ".webp";
            case "image/bmp":  return ".bmp";
            case "image/svg+xml": return ".svg";
            default: return null;
        }
    }

    /**
     * 从 OSS 真正删除对象
     */
    public void deleteFromOss(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        OSS ossClient = createOssClient();
        try {
            ossClient.deleteObject(aliOSSProperties.getBucketName(), objectKey);
        } catch (Exception e) {
            // 记录日志，但不向调用方抛出，避免影响 DB 软删除流程
            System.err.println("OSS 删除失败 [" + objectKey + "]: " + e.getMessage());
        } finally {
            ossClient.shutdown();
        }
    }
}
