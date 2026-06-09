package com.yxshop.Utils;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云验证码 v2 服务
 *
 * 因 com.aliyun:captcha20230305 不在 Maven Central，改用 aliyun-java-sdk-core
 * 的 CommonRequest 泛化调用，与官方 SDK 等价（相同 endpoint + action + 签名）。
 *
 * 配置存储：Redis "admin:captcha:config"，字段 sceneId / prefix
 * AccessKey 与 OSS 共用同一 RAM 账号
 */
@Slf4j
@Service
public class CaptchaService {

    public static final String CAPTCHA_CONFIG_KEY = "admin:captcha:config";
    private static final String OSS_CFG_KEY       = "admin:oss:config:override";
    private static final String CAPTCHA_ENDPOINT  = "captcha.cn-shanghai.aliyuncs.com";
    private static final String CAPTCHA_VERSION   = "2023-03-05";
    private static final String CAPTCHA_ACTION    = "VerifyIntelligentCaptcha";

    private final AliOSSProperties ossProps;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CaptchaService(AliOSSProperties ossProps,
                          ObjectProvider<StringRedisTemplate> redisProvider) {
        this.ossProps = ossProps;
        this.redis    = redisProvider.getIfAvailable();
    }

    // ─── 配置读取 ─────────────────────────────────────────────────────────────

    public Map<String, String> loadConfig() {
        if (redis != null) {
            try {
                String json = redis.opsForValue().get(CAPTCHA_CONFIG_KEY);
                if (json != null) {
                    return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
                }
            } catch (Exception ignored) {}
        }
        return new HashMap<>();
    }

    public String getSceneId() { return loadConfig().getOrDefault("sceneId", ""); }
    public String getPrefix()  { return loadConfig().getOrDefault("prefix",  ""); }
    public boolean isEnabled() { return !getSceneId().isBlank(); }

    // ─── 验证 ─────────────────────────────────────────────────────────────────

    /**
     * 等价于官方示例中的 client.verifyIntelligentCaptcha(request)。
     * 出现任何异常时认为通过（优先保证业务可用，官方示例同款处理逻辑）。
     */
    public boolean verify(String captchaVerifyParam) {
        if (captchaVerifyParam == null || captchaVerifyParam.isBlank()) return false;
        String sceneId = getSceneId();
        if (sceneId.isBlank()) return true;

        String[] ak = loadOssAk();
        if (ak == null) {
            log.warn("CAPTCHA 验证跳过：AccessKey 未配置");
            return true;
        }

        try {
            // 初始化客户端（等价官方示例的 new Client(config)）
            DefaultProfile profile = DefaultProfile.getProfile("cn-shanghai", ak[0], ak[1]);
            IAcsClient client = new DefaultAcsClient(profile);

            // 构建请求（等价 new VerifyIntelligentCaptchaRequest()）
            CommonRequest request = new CommonRequest();
            request.setSysMethod(MethodType.POST);
            request.setSysDomain(CAPTCHA_ENDPOINT);
            request.setSysVersion(CAPTCHA_VERSION);
            request.setSysAction(CAPTCHA_ACTION);
            // 防止前端篡改场景，传入 sceneId
            request.putBodyParameter("SceneId",            sceneId);
            request.putBodyParameter("CaptchaVerifyParam", captchaVerifyParam);

            CommonResponse response = client.getCommonResponse(request);
            String body = response.getData();

            // 解析返回（等价 resp.body.result.verifyResult）
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = objectMapper.readValue(body, Map.class);
            if (!"Success".equals(resp.get("Code"))) {
                log.warn("CAPTCHA API Code={} Msg={}", resp.get("Code"), resp.get("Message"));
                return false;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) resp.get("Result");
            Boolean verifyResult = result != null ? (Boolean) result.get("VerifyResult") : null;
            log.info("CAPTCHA verifyResult={} verifyCode={}", verifyResult,
                    result != null ? result.get("VerifyCode") : null);
            return Boolean.TRUE.equals(verifyResult);

        } catch (ClientException e) {
            // 出现异常认为验证通过，优先保证业务可用（官方示例同款做法）
            log.error("CAPTCHA ClientException errCode={} msg={}", e.getErrCode(), e.getMessage());
            return true;
        } catch (Exception e) {
            log.error("CAPTCHA Exception: {}", e.getMessage());
            return true;
        }
    }

    // ─── 内部 ─────────────────────────────────────────────────────────────────

    private String[] loadOssAk() {
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
        return (aki == null || aki.isBlank() || aks == null || aks.isBlank()) ? null : new String[]{aki, aks};
    }
}
