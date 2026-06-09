package com.yxshop.Utils;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 阿里云号码验证服务短信（dypnsapi · SendSmsVerifyCode）
 *
 * 使用 aliyun-java-sdk-core CommonRequest 泛化调用，与 CaptchaService 相同模式，
 * 无需额外 SDK 依赖。验证码由阿里云生成并下发，接口返回实际验证码供后端存储校验。
 *
 * AK/SK 与 OSS 共用同一 RAM 账号，从 Redis "admin:oss:config:override" 读取。
 * SignName / TemplateCode 存储于 Redis "admin:sms:config"。
 *
 * 计费：仅成功投递收费，≤1000次/月 ¥0.06/次。
 */
@Slf4j
@Service
public class AliyunSmsService {

    public static final String REDIS_CONFIG_KEY = "admin:sms:config";
    private static final String OSS_CFG_KEY     = "admin:oss:config:override";

    private static final String PNVS_DOMAIN  = "dypnsapi.aliyuncs.com";
    private static final String PNVS_VERSION = "2017-05-25";
    private static final String PNVS_ACTION  = "SendSmsVerifyCode";

    private final AliOSSProperties    ossProps;
    private final StringRedisTemplate redis;
    private final ObjectMapper        objectMapper = new ObjectMapper();

    public AliyunSmsService(AliOSSProperties ossProps,
                            ObjectProvider<StringRedisTemplate> redisProvider) {
        this.ossProps = ossProps;
        this.redis    = redisProvider.getIfAvailable();
    }

    /** 是否已配置短信签名（AK 由 OSS 统一管理，此处只检查 signName） */
    public boolean isConfigured() {
        return notBlank(loadSmsConfig().get("signName"));
    }

    /**
     * 发送短信验证码（dypnsapi · SendSmsVerifyCode）
     * 验证码由阿里云生成并自动下发，返回实际验证码供后端存储校验。
     *
     * @param phone 11 位国内手机号
     * @return 阿里云生成的验证码（6 位数字）
     */
    public String sendVerificationCode(String phone) throws Exception {
        String[] ak = loadAk();
        if (ak == null) {
            throw new IllegalStateException("AccessKey 未配置，请先在「第三方资源 → 阿里云 OSS」中填写 AK/SK");
        }
        Map<String, String> sms = loadSmsConfig();
        if (!notBlank(sms.get("signName"))) {
            throw new IllegalStateException("短信签名未配置，请在「第三方资源 → 短信服务」中填写签名");
        }

        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", ak[0], ak[1]);
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain(PNVS_DOMAIN);
        request.setSysVersion(PNVS_VERSION);
        request.setSysAction(PNVS_ACTION);
        request.putBodyParameter("PhoneNumber",   phone);
        request.putBodyParameter("SignName",       sms.get("signName"));
        request.putBodyParameter("TemplateCode",   sms.getOrDefault("templateCode", ""));
        request.putBodyParameter("TemplateParam",  "{\"code\":\"##code##\",\"min\":\"5\"}");

        CommonResponse response = client.getCommonResponse(request);
        String body = response.getData();

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = objectMapper.readValue(body, Map.class);
        String code = String.valueOf(resp.getOrDefault("Code", ""));
        if (!"OK".equals(code)) {
            log.error("短信发送失败 phone={} code={} msg={}", phone, code, resp.get("Message"));
            throw new RuntimeException("短信发送失败: " + resp.get("Message"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) resp.get("Model");
        String verifyCode = model != null ? String.valueOf(model.get("VerifyCode")) : null;
        log.info("短信验证码已发送 phone={} bizId={}", phone,
                model != null ? model.get("BizId") : null);

        if (!notBlank(verifyCode) || "null".equals(verifyCode)) {
            throw new RuntimeException("短信发送响应异常，未获取到验证码");
        }
        return verifyCode;
    }

    /** 返回当前短信配置（供 API 展示） */
    public Map<String, Object> getMaskedConfig() {
        Map<String, String> sms = loadSmsConfig();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("signName",    sms.getOrDefault("signName", ""));
        out.put("templateCode", sms.getOrDefault("templateCode", ""));
        out.put("configured",  isConfigured());
        out.put("akConfigured", loadAk() != null);
        return out;
    }

    // ── 内部：读取配置 ─────────────────────────────────────────────────────────

    /** AK/SK：优先 OSS Redis 动态配置，回退 application.yml */
    private String[] loadAk() {
        String aki = ossProps.getAccessKeyId();
        String aks = ossProps.getAccessKeySecret();
        if (redis != null) {
            try {
                String json = redis.opsForValue().get(OSS_CFG_KEY);
                if (json != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> m = objectMapper.readValue(json, Map.class);
                    aki = m.getOrDefault("accessKeyId", aki);
                    if (m.containsKey("accessKeySecret")) aks = m.get("accessKeySecret");
                }
            } catch (Exception ignored) {}
        }
        return (notBlank(aki) && notBlank(aks)) ? new String[]{aki, aks} : null;
    }

    /** 短信专属配置（signName + templateCode）*/
    Map<String, String> loadSmsConfig() {
        if (redis != null) {
            try {
                String json = redis.opsForValue().get(REDIS_CONFIG_KEY);
                if (json != null) {
                    return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
                }
            } catch (Exception ignored) {}
        }
        return new java.util.HashMap<>();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
