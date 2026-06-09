package com.yxshop.Module.Admin.Controller;

import com.yxshop.Module.Admin.Dto.AdminUserDto;
import com.yxshop.Module.Admin.Service.AdminUserService;
import com.yxshop.Utils.AliOSSProperties;
import com.yxshop.Utils.AliyunSmsService;
import com.yxshop.Utils.OssStatsService;
import com.yxshop.Utils.Result;
import org.springframework.beans.factory.annotation.Value;
import javax.annotation.PostConstruct;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AliOSSProperties aliOSSProperties;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final AliyunSmsService smsService;
    private final OssStatsService  ossStatsService;

    /** 从 .env / docker-compose 环境变量读取 SMTP 默认配置 */
    @Value("${spring.mail.host:smtp.qq.com}") private String envMailHost;
    @Value("${spring.mail.username:}")        private String envMailUsername;
    @Value("${spring.mail.password:}")        private String envMailPassword;

    public AdminUserController(AdminUserService adminUserService,
                               AliOSSProperties aliOSSProperties,
                               org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
                               AliyunSmsService smsService,
                               OssStatsService ossStatsService) {
        this.adminUserService = adminUserService;
        this.aliOSSProperties = aliOSSProperties;
        this.redisTemplate    = redisTemplate;
        this.smsService       = smsService;
        this.ossStatsService  = ossStatsService;
    }

    /**
     * 启动时：若 Redis 中尚无 SMTP 配置，将 .env 邮件变量自动填充进去。
     * 这样管理后台打开即可看到预填的 QQ 邮箱配置，无需手动再输一遍。
     */
    @PostConstruct
    public void autoInitSmtpConfig() {
        if (redisTemplate == null || envMailUsername == null || envMailUsername.isBlank()) return;
        try {
            if (redisTemplate.opsForValue().get(SMTP_CONFIG_KEY) != null) return; // 已配置，跳过
            java.util.Map<String, String> cfg = new java.util.LinkedHashMap<>();
            cfg.put("host",     envMailHost);
            cfg.put("port",     "465");
            cfg.put("username", envMailUsername);
            cfg.put("password", envMailPassword);
            redisTemplate.opsForValue().set(SMTP_CONFIG_KEY,
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(cfg),
                    java.time.Duration.ofDays(365));
        } catch (Exception e) {
            // 非致命，忽略
        }
    }

    // ── 认证 ─────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        return Result.success(adminUserService.login(
                body.get("username"), body.get("password"), request.getRemoteAddr()));
    }

    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        String token = extractToken(request);
        adminUserService.logout(currentUserId(request), token);
        return Result.success("已退出");
    }

    @GetMapping("/me")
    public Result me(HttpServletRequest request) {
        return Result.success(adminUserService.getCurrentAdmin(currentUserId(request)));
    }

    @PutMapping("/change-password")
    public Result changePassword(HttpServletRequest request, @RequestBody java.util.Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        adminUserService.changePassword(currentUserId(request), oldPassword, newPassword);
        return Result.success("密码已修改");
    }

    // ── 管理员 CRUD ───────────────────────────────────────────────────────────

    @GetMapping("/users")
    public Result list(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "20") Integer size,
                       @RequestParam(required = false) String keyword) {
        return Result.success(adminUserService.listAdmins(page, size, keyword));
    }

    @PostMapping("/users")
    public Result create(@RequestBody AdminUserDto dto) {
        return Result.success(adminUserService.createAdmin(dto));
    }

    @PutMapping("/users/{id}")
    public Result update(HttpServletRequest request,
                         @PathVariable Long id,
                         @RequestBody AdminUserDto dto) {
        dto.setId(id);
        return Result.success(adminUserService.updateAdmin(currentUserId(request), dto));
    }

    @DeleteMapping("/users/{id}")
    public Result delete(HttpServletRequest request, @PathVariable Long id) {
        adminUserService.deleteAdmin(currentUserId(request), id);
        return Result.success("删除成功");
    }

    // ── 角色 CRUD ─────────────────────────────────────────────────────────────

    @GetMapping("/roles")
    public Result roles() {
        return Result.success(adminUserService.listRoles());
    }

    @PostMapping("/roles")
    public Result createRole(@RequestBody Map<String, Object> dto) {
        return Result.success(adminUserService.createRole(dto));
    }

    @PutMapping("/roles/{id}")
    public Result updateRole(@PathVariable Long id, @RequestBody Map<String, Object> dto) {
        return Result.success(adminUserService.updateRole(id, dto));
    }

    @DeleteMapping("/roles/{id}")
    public Result deleteRole(@PathVariable Long id) {
        adminUserService.deleteRole(id);
        return Result.success("删除成功");
    }

    // ── 角色权限 ──────────────────────────────────────────────────────────────

    @GetMapping("/roles/{id}/permissions")
    public Result getRolePermissions(@PathVariable Long id) {
        return Result.success(adminUserService.getRolePermissions(id));
    }

    @PutMapping("/roles/{id}/permissions")
    public Result saveRolePermissions(@PathVariable Long id,
                                      @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> perms = (List<String>) body.get("permissions");
        adminUserService.saveRolePermissions(id, perms != null ? perms : List.of());
        return Result.success("保存成功");
    }

    // ── 菜单 ──────────────────────────────────────────────────────────────────

    @GetMapping("/menus")
    public Result menus(HttpServletRequest request) {
        return Result.success(adminUserService.listMenuTree(currentUserId(request)));
    }

    // ── 日志 ──────────────────────────────────────────────────────────────────

    @GetMapping("/login-logs")
    public Result loginLogs(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "20") Integer size,
                            @RequestParam(required = false) Long adminUserId,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String startDate,
                            @RequestParam(required = false) String endDate) {
        return Result.success(adminUserService.listLoginLogs(page, size, adminUserId, keyword, startDate, endDate));
    }

    @GetMapping("/operation-logs")
    public Result operationLogs(@RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "20") Integer pageSize,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String module,
                                @RequestParam(required = false) String action,
                                @RequestParam(required = false) String startDate,
                                @RequestParam(required = false) String endDate) {
        return Result.success(adminUserService.listOperationLogs(
                pageNum, pageSize, keyword, module, action, startDate, endDate));
    }

    // ── 系统配置 ──────────────────────────────────────────────────────────────

    @GetMapping("/system/config")
    public Result getConfig() {
        return Result.success(adminUserService.getSystemConfig());
    }

    @PostMapping("/system/config")
    public Result saveConfig(@RequestBody Map<String, Object> body) {
        String section = body.containsKey("section") ? body.get("section").toString() : "basic";
        body.remove("section");
        adminUserService.saveSystemConfig(section, body);
        return Result.success("保存成功");
    }

    // ── OSS 配置 ──────────────────────────────────────────────────────────────

    private static final String OSS_CONFIG_REDIS_KEY = "admin:oss:config:override";

    @GetMapping("/system/oss-config")
    public Result getOssConfig() {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        // 读取 Redis 覆盖值（如果有）
        String override = null;
        try { override = redisTemplate.opsForValue().get(OSS_CONFIG_REDIS_KEY); } catch (Exception ignored) {}
        java.util.Map<String, Object> overrideMap = new java.util.HashMap<>();
        if (override != null) {
            try { overrideMap = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(override, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>(){}); }
            catch (Exception ignored) {}
        }
        String endpoint    = overrideMap.containsKey("endpoint")    ? (String)overrideMap.get("endpoint")    : aliOSSProperties.getEndpoint();
        String bucketName  = overrideMap.containsKey("bucketName")  ? (String)overrideMap.get("bucketName")  : aliOSSProperties.getBucketName();
        String accessKeyId = overrideMap.containsKey("accessKeyId") ? (String)overrideMap.get("accessKeyId") : aliOSSProperties.getAccessKeyId();
        result.put("endpoint",    endpoint);
        result.put("bucketName",  bucketName);
        result.put("accessKeyId", accessKeyId);
        result.put("accessKeySecretMasked", maskSecret(aliOSSProperties.getAccessKeySecret()));
        result.put("hasOverride", !overrideMap.isEmpty());
        // 计费参考（阿里云标准存储定价，单位：元）
        result.put("billing", new java.util.LinkedHashMap<String,Object>() {{
            put("storagePerGB",  0.12);   // /GB/月
            put("getRequest",    0.01);   // /万次
            put("putRequest",    0.01);   // /千次
            put("trafficOut",    0.50);   // /GB 外网流出
            put("currency", "CNY");
            put("pricingUrl", "https://www.aliyun.com/price/product#/oss/detail");
        }});
        return Result.success(result);
    }

    @PutMapping("/system/oss-config")
    public Result saveOssConfig(@RequestBody java.util.Map<String, Object> body) {
        // 只保存非敏感覆盖（endpoint / bucketName / accessKeyId / accessKeySecret）
        java.util.Map<String, Object> toSave = new java.util.LinkedHashMap<>();
        if (body.containsKey("endpoint"))    toSave.put("endpoint",    body.get("endpoint"));
        if (body.containsKey("bucketName"))  toSave.put("bucketName",  body.get("bucketName"));
        if (body.containsKey("accessKeyId")) toSave.put("accessKeyId", body.get("accessKeyId"));
        // secret 只在非空时更新
        Object secret = body.get("accessKeySecret");
        if (secret != null && !secret.toString().isBlank() && !secret.toString().contains("*")) {
            toSave.put("accessKeySecret", secret.toString());
        }
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(toSave);
            redisTemplate.opsForValue().set(OSS_CONFIG_REDIS_KEY, json,
                    java.time.Duration.ofDays(365));
        } catch (Exception e) {
            return Result.error("保存失败: " + e.getMessage());
        }
        return Result.success("OSS 配置已保存，重启服务后完全生效");
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.length() < 8) return "****";
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }

    // ── OSS 用量与计费统计（BSS API） ─────────────────────────────────────────

    @GetMapping("/system/oss-stats")
    public Result getOssStats() {
        return Result.success(ossStatsService.getMonthlyStats());
    }

    /** 手动刷新 OSS 统计缓存 */
    @PostMapping("/system/oss-stats/refresh")
    public Result refreshOssStats() {
        ossStatsService.invalidateCache();
        return Result.success(ossStatsService.getMonthlyStats());
    }

    // ── 短信服务配置 ──────────────────────────────────────────────────────────

    private static final String SMS_CONFIG_KEY = AliyunSmsService.REDIS_CONFIG_KEY;

    @GetMapping("/system/sms-config")
    public Result getSmsConfig() {
        return Result.success(smsService.getMaskedConfig());
    }

    @PutMapping("/system/sms-config")
    public Result saveSmsConfig(@RequestBody Map<String, Object> body) {
        // AK/SK 与 OSS 共用，只存 signName / templateCode
        java.util.Map<String, String> toSave = new java.util.LinkedHashMap<>();
        copyIfPresent(body, toSave, "signName");
        copyIfPresent(body, toSave, "templateCode");
        if (!toSave.containsKey("signName")) return Result.error("短信签名（signName）不能为空");
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(toSave);
            redisTemplate.opsForValue().set(SMS_CONFIG_KEY, json, java.time.Duration.ofDays(365));
        } catch (Exception e) {
            return Result.error("保存失败: " + e.getMessage());
        }
        return Result.success("短信配置已保存，立即生效");
    }

    // ── SMTP 邮件配置 ─────────────────────────────────────────────────────────

    private static final String SMTP_CONFIG_KEY = com.yxshop.Utils.CodeUtils.SMTP_CONFIG_KEY;

    @GetMapping("/system/smtp-config")
    public Result getSmtpConfig() {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (redisTemplate != null) {
            try {
                String json = redisTemplate.opsForValue().get(SMTP_CONFIG_KEY);
                if (json != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
                    result.put("host",     String.valueOf(m.getOrDefault("host", "smtp.qq.com")));
                    result.put("port",     String.valueOf(m.getOrDefault("port", "465")));
                    result.put("username", String.valueOf(m.getOrDefault("username", "")));
                    result.put("passwordMasked", maskSecret(String.valueOf(m.getOrDefault("password", ""))));
                    result.put("configured", true);
                    result.put("source", "redis");
                    return Result.success(result);
                }
            } catch (Exception ignored) {}
        }
        // 读取 application.yml 默认值
        result.put("host",     "smtp.qq.com");
        result.put("port",     "465");
        result.put("username", "");
        result.put("passwordMasked", "");
        result.put("configured", false);
        result.put("source", "default");
        return Result.success(result);
    }

    @PutMapping("/system/smtp-config")
    public Result saveSmtpConfig(@RequestBody Map<String, Object> body) {
        java.util.Map<String, String> toSave = new java.util.LinkedHashMap<>();
        copyIfPresent(body, toSave, "host");
        copyIfPresent(body, toSave, "port");
        copyIfPresent(body, toSave, "username");
        // 密码只在非空且未脱敏时更新
        Object pwd = body.get("password");
        if (pwd != null && !pwd.toString().isBlank() && !pwd.toString().contains("*")) {
            toSave.put("password", pwd.toString());
        } else if (redisTemplate != null) {
            try {
                String existing = redisTemplate.opsForValue().get(SMTP_CONFIG_KEY);
                if (existing != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(existing, Map.class);
                    Object ep = m.get("password");
                    if (ep != null) toSave.put("password", ep.toString());
                }
            } catch (Exception ignored) {}
        }
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(toSave);
            redisTemplate.opsForValue().set(SMTP_CONFIG_KEY, json, java.time.Duration.ofDays(365));
        } catch (Exception e) {
            return Result.error("保存失败: " + e.getMessage());
        }
        return Result.success("邮件配置已保存，立即生效（无需重启）");
    }

    // ── 阿里云验证码（CAPTCHA v2）配置 ───────────────────────────────────────

    private static final String CAPTCHA_CONFIG_KEY = "admin:captcha:config";

    /**
     * CAPTCHA 配置只存 sceneId。
     * AccessKey ID/Secret 与 OSS 共用同一 RAM 账号，运行时从 OSS 配置读取，无需重复存储。
     */
    @GetMapping("/system/captcha-config")
    public Result getCaptchaConfig() {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        String sceneId = "", prefix = "";
        if (redisTemplate != null) {
            try {
                String json = redisTemplate.opsForValue().get(CAPTCHA_CONFIG_KEY);
                if (json != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
                    sceneId = String.valueOf(m.getOrDefault("sceneId", ""));
                    prefix  = String.valueOf(m.getOrDefault("prefix",  ""));
                }
            } catch (Exception ignored) {}
        }
        result.put("sceneId",    sceneId);
        result.put("prefix",     prefix);
        result.put("configured", notBlank(sceneId));
        return Result.success(result);
    }

    @PutMapping("/system/captcha-config")
    public Result saveCaptchaConfig(@RequestBody Map<String, Object> body) {
        java.util.Map<String, String> toSave = new java.util.LinkedHashMap<>();
        copyIfPresent(body, toSave, "sceneId");
        copyIfPresent(body, toSave, "prefix");
        if (!toSave.containsKey("sceneId")) return Result.error("sceneId 不能为空");
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(toSave);
            redisTemplate.opsForValue().set(CAPTCHA_CONFIG_KEY, json, java.time.Duration.ofDays(365));
        } catch (Exception e) {
            return Result.error("保存失败: " + e.getMessage());
        }
        return Result.success("验证码配置已保存，立即生效");
    }

    // ── 删除服务配置（清空 Redis key） ────────────────────────────────────────

    private static final String AMAP_CONFIG_KEY      = "admin:amap:config";
    private static final String LOGISTICS_CONFIG_KEY  = "admin:logistics:config";

    // ── 高德地图配置 ───────────────────────────────────────────────────────────
    @GetMapping("/system/amap-config")
    public Result getAmapConfig() {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (redisTemplate != null) {
            try {
                String json = redisTemplate.opsForValue().get(AMAP_CONFIG_KEY);
                if (json != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
                    result.put("webKey",      String.valueOf(m.getOrDefault("webKey",      "")));
                    result.put("securityKey", maskSecret(String.valueOf(m.getOrDefault("securityKey", ""))));
                    result.put("configured",  notBlank(String.valueOf(m.getOrDefault("webKey", ""))));
                    return Result.success(result);
                }
            } catch (Exception ignored) {}
        }
        result.put("webKey", ""); result.put("securityKey", ""); result.put("configured", false);
        return Result.success(result);
    }

    @PutMapping("/system/amap-config")
    public Result saveAmapConfig(@RequestBody Map<String, Object> body) {
        java.util.Map<String, String> toSave = new java.util.LinkedHashMap<>();
        copyIfPresent(body, toSave, "webKey");
        Object sk = body.get("securityKey");
        if (sk != null && !sk.toString().isBlank() && !sk.toString().contains("*"))
            toSave.put("securityKey", sk.toString());
        else if (redisTemplate != null) {
            try {
                String ex = redisTemplate.opsForValue().get(AMAP_CONFIG_KEY);
                if (ex != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(ex, Map.class);
                    Object ep = m.get("securityKey");
                    if (ep != null) toSave.put("securityKey", ep.toString());
                }
            } catch (Exception ignored) {}
        }
        if (!toSave.containsKey("webKey")) return Result.error("WebKey 不能为空");
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(toSave);
            redisTemplate.opsForValue().set(AMAP_CONFIG_KEY, json, java.time.Duration.ofDays(365));
        } catch (Exception e) { return Result.error("保存失败: " + e.getMessage()); }
        return Result.success("高德地图配置已保存");
    }

    // ── 快递查询 API 配置 ──────────────────────────────────────────────────────
    @GetMapping("/system/logistics-config")
    public Result getLogisticsConfig() {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (redisTemplate != null) {
            try {
                String json = redisTemplate.opsForValue().get(LOGISTICS_CONFIG_KEY);
                if (json != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
                    result.put("provider",   String.valueOf(m.getOrDefault("provider",   "kuaidi100")));
                    result.put("customer",   String.valueOf(m.getOrDefault("customer",   "")));
                    result.put("key",        maskSecret(String.valueOf(m.getOrDefault("key", ""))));
                    result.put("configured", notBlank(String.valueOf(m.getOrDefault("customer", ""))));
                    return Result.success(result);
                }
            } catch (Exception ignored) {}
        }
        result.put("provider", "kuaidi100"); result.put("customer", ""); result.put("key", ""); result.put("configured", false);
        return Result.success(result);
    }

    @PutMapping("/system/logistics-config")
    public Result saveLogisticsConfig(@RequestBody Map<String, Object> body) {
        java.util.Map<String, String> toSave = new java.util.LinkedHashMap<>();
        copyIfPresent(body, toSave, "provider");
        copyIfPresent(body, toSave, "customer");
        Object k = body.get("key");
        if (k != null && !k.toString().isBlank() && !k.toString().contains("*"))
            toSave.put("key", k.toString());
        else if (redisTemplate != null) {
            try {
                String ex = redisTemplate.opsForValue().get(LOGISTICS_CONFIG_KEY);
                if (ex != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(ex, Map.class);
                    Object ep = m.get("key"); if (ep != null) toSave.put("key", ep.toString());
                }
            } catch (Exception ignored) {}
        }
        if (!toSave.containsKey("customer")) return Result.error("Customer 不能为空");
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(toSave);
            redisTemplate.opsForValue().set(LOGISTICS_CONFIG_KEY, json, java.time.Duration.ofDays(365));
        } catch (Exception e) { return Result.error("保存失败: " + e.getMessage()); }
        return Result.success("快递查询配置已保存");
    }

    // ── 快递100 物流轨迹查询 ───────────────────────────────────────────────────
    private static final java.util.Map<String, String> CARRIER_CODE_MAP =
            new java.util.HashMap<String, String>() {{
                put("SF", "sf"); put("YTO", "yuantong"); put("ZTO", "zhongtong");
                put("YD",  "yunda"); put("STO", "shentong"); put("JD", "jd");
                put("EMS", "ems");
            }};

    @GetMapping("/system/logistics/track")
    public Result trackPackage(@RequestParam String company, @RequestParam String trackingNo) {
        if (redisTemplate == null) return Result.error("Redis 不可用");
        String cfg;
        try { cfg = redisTemplate.opsForValue().get(LOGISTICS_CONFIG_KEY); }
        catch (Exception e) { return Result.error("读取配置失败"); }
        if (cfg == null || cfg.isBlank()) return Result.error("快递查询 API 尚未配置，请先在第三方资源管理中配置快递100");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = new com.fasterxml.jackson.databind.ObjectMapper().readValue(cfg, Map.class);
            String customer   = String.valueOf(config.getOrDefault("customer", ""));
            String key        = String.valueOf(config.getOrDefault("key",      ""));
            String comCode    = CARRIER_CODE_MAP.getOrDefault(company.toUpperCase(), company.toLowerCase());

            String paramJson  = String.format("{\"com\":\"%s\",\"num\":\"%s\",\"resultv2\":\"1\"}", comCode, trackingNo);
            long   t          = System.currentTimeMillis();
            String raw        = paramJson + t + key + customer;
            String sign       = md5Upper(raw);

            org.springframework.web.client.RestTemplate rest = new org.springframework.web.client.RestTemplate();
            org.springframework.util.LinkedMultiValueMap<String, String> form = new org.springframework.util.LinkedMultiValueMap<>();
            form.add("customer", customer);
            form.add("sign",     sign);
            form.add("param",    paramJson);
            form.add("t",        String.valueOf(t));

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> entity =
                    new org.springframework.http.HttpEntity<>(form, headers);

            String response = rest.postForObject("https://poll.kuaidi100.com/poll/query.do", entity, String.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = new com.fasterxml.jackson.databind.ObjectMapper().readValue(response, Map.class);
            return Result.success(resp);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    private static String md5Upper(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString().toUpperCase();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static final java.util.Map<String, String> SVC_REDIS_KEYS =
            new java.util.HashMap<String, String>() {{
                put("alioss",     OSS_CONFIG_REDIS_KEY);
                put("email",      com.yxshop.Utils.CodeUtils.SMTP_CONFIG_KEY);
                put("sms",        com.yxshop.Utils.AliyunSmsService.REDIS_CONFIG_KEY);
                put("captcha",    "admin:captcha:config");
                put("amap",       AMAP_CONFIG_KEY);
                put("logistics",  LOGISTICS_CONFIG_KEY);
            }};

    @DeleteMapping("/system/service-config/{svcId}")
    public Result deleteServiceConfig(@PathVariable String svcId) {
        String key = SVC_REDIS_KEYS.get(svcId);
        if (key == null) return Result.error("未知服务 ID: " + svcId);
        if (redisTemplate != null) {
            try { redisTemplate.delete(key); } catch (Exception ignored) {}
        }
        return Result.success("已清除 " + svcId + " 配置");
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private void copyIfPresent(Map<String, Object> src, Map<String, String> dst, String key) {
        Object v = src.get(key);
        if (v != null && !v.toString().isBlank()) dst.put(key, v.toString());
    }

    // ── 内部 ──────────────────────────────────────────────────────────────────

    private Long currentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value == null) throw new IllegalArgumentException("请先登录");
        return Long.valueOf(String.valueOf(value));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }
}
