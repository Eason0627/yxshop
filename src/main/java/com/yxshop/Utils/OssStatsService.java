package com.yxshop.Utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.BucketStat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * OSS 用量与费用统计服务
 *
 * 数据来源：
 *  1. OSS getBucketStat → 存储量 / 对象数（真实 API，与阿里云 OSS 实时同步）
 *  2. 费用估算 = 存储 GB × ¥0.12（标准存储单价）
 *
 * 备注：BSS 真实账单 API（aliyun-java-sdk-bssopenapi）不在 Maven Central，
 *       如需接入须添加阿里云私有 Maven 仓库。当前以本地估算代替。
 *
 * 结果缓存到 Redis 1 小时，减少 OSS API 调用。
 */
@Slf4j
@Service
public class OssStatsService {

    private static final String CACHE_KEY     = "admin:oss:stats:monthly";
    private static final String OSS_CFG_KEY   = "admin:oss:config:override";
    private static final long   CACHE_HOURS   = 1;

    // 阿里云 OSS 标准存储定价（元/GB/月）
    private static final double PRICE_STORAGE_PER_GB   = 0.12;
    private static final double PRICE_GET_PER_TEN_THOUSAND = 0.01;
    private static final double PRICE_PUT_PER_THOUSAND  = 0.01;
    private static final double PRICE_TRAFFIC_OUT_PER_GB = 0.50;

    private final AliOSSProperties ossProps;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OssStatsService(AliOSSProperties ossProps,
                           ObjectProvider<StringRedisTemplate> redisProvider) {
        this.ossProps = ossProps;
        this.redis    = redisProvider.getIfAvailable();
    }

    // ─── 公共接口 ─────────────────────────────────────────────────────────────

    public Map<String, Object> getMonthlyStats() {
        // 1. 读缓存
        if (redis != null) {
            try {
                String cached = redis.opsForValue().get(CACHE_KEY);
                if (cached != null) {
                    return objectMapper.readValue(cached, new TypeReference<Map<String, Object>>() {});
                }
            } catch (Exception ignored) {}
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        OssCredentials cred = loadCredentials();

        // 2. 查询 OSS 真实存储量
        fetchStorageStats(stats, cred);

        // 3. 费用估算（基于存储量）
        buildBillingEstimate(stats);

        // 4. 元信息
        stats.put("billMonth", YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        stats.put("billingNote", "存储费用基于 ¥0.12/GB/月估算，不含请求费与流量费。如需真实账单请在阿里云控制台查看。");
        stats.put("pricingUrl", "https://www.aliyun.com/price/product#/oss/detail");

        // 5. 写缓存
        cacheStats(stats);
        return stats;
    }

    /** 清除缓存（OSS 配置改变后调用） */
    public void invalidateCache() {
        if (redis != null) {
            try { redis.delete(CACHE_KEY); } catch (Exception ignored) {}
        }
    }

    // ─── OSS 存储统计 ─────────────────────────────────────────────────────────

    private void fetchStorageStats(Map<String, Object> stats, OssCredentials cred) {
        if (!cred.isValid()) {
            stats.put("storageOk", false);
            stats.put("storageError", "OSS 未配置（Endpoint / AccessKey / Bucket 为空）");
            return;
        }
        try {
            OSS client = new OSSClientBuilder()
                    .build(cred.endpoint, cred.accessKeyId, cred.accessKeySecret);
            BucketStat bs = client.getBucketStat(cred.bucketName);
            client.shutdown();

            long storageBytes = bs.getStorageSize();
            long objectCount  = bs.getObjectCount();
            double storageGB  = storageBytes / (1024.0 * 1024.0 * 1024.0);

            stats.put("storageOk",    true);
            stats.put("storageBytes", storageBytes);
            stats.put("storageGB",    Math.round(storageGB * 1000.0) / 1000.0);
            stats.put("objectCount",  objectCount);
            log.info("OSS stats: {:.3f}GB, {} objects", storageGB, objectCount);
        } catch (Exception e) {
            log.warn("OSS getBucketStat 失败: {}", e.getMessage());
            stats.put("storageOk",    false);
            stats.put("storageError", e.getMessage());
        }
    }

    private void buildBillingEstimate(Map<String, Object> stats) {
        Object gbObj = stats.get("storageGB");
        if (gbObj instanceof Number) {
            double gb = ((Number) gbObj).doubleValue();
            double estimatedCost = Math.round(gb * PRICE_STORAGE_PER_GB * 100.0) / 100.0;
            stats.put("estimatedStorageCost", estimatedCost);
            // billOk=false 表示是估算而非真实账单
            stats.put("billOk",       false);
            stats.put("billEstimated", estimatedCost);
        } else {
            stats.put("billOk",        false);
            stats.put("billEstimated", null);
        }
        // 定价参考
        Map<String, Object> pricing = new LinkedHashMap<>();
        pricing.put("storagePerGB",        PRICE_STORAGE_PER_GB);
        pricing.put("getRequestPer10k",    PRICE_GET_PER_TEN_THOUSAND);
        pricing.put("putRequestPer1k",     PRICE_PUT_PER_THOUSAND);
        pricing.put("trafficOutPerGB",     PRICE_TRAFFIC_OUT_PER_GB);
        stats.put("pricing", pricing);
    }

    // ─── 内部工具 ─────────────────────────────────────────────────────────────

    private OssCredentials loadCredentials() {
        if (redis != null) {
            try {
                String json = redis.opsForValue().get(OSS_CFG_KEY);
                if (json != null) {
                    Map<String, String> m = objectMapper.readValue(json,
                            new TypeReference<Map<String, String>>() {});
                    return new OssCredentials(
                            m.getOrDefault("endpoint",    ossProps.getEndpoint()),
                            m.getOrDefault("accessKeyId", ossProps.getAccessKeyId()),
                            m.containsKey("accessKeySecret") ? m.get("accessKeySecret") : ossProps.getAccessKeySecret(),
                            m.getOrDefault("bucketName",  ossProps.getBucketName())
                    );
                }
            } catch (Exception ignored) {}
        }
        return new OssCredentials(ossProps.getEndpoint(), ossProps.getAccessKeyId(),
                ossProps.getAccessKeySecret(), ossProps.getBucketName());
    }

    private void cacheStats(Map<String, Object> stats) {
        if (redis == null) return;
        try {
            redis.opsForValue().set(CACHE_KEY,
                    objectMapper.writeValueAsString(stats), CACHE_HOURS, TimeUnit.HOURS);
        } catch (Exception ignored) {}
    }

    private static class OssCredentials {
        final String endpoint, accessKeyId, accessKeySecret, bucketName;
        OssCredentials(String ep, String aki, String aks, String bn) {
            endpoint = ep; accessKeyId = aki; accessKeySecret = aks; bucketName = bn;
        }
        boolean isValid() {
            return nb(endpoint) && nb(accessKeyId) && nb(accessKeySecret) && nb(bucketName);
        }
        private static boolean nb(String s) { return s != null && !s.isBlank(); }
    }
}
