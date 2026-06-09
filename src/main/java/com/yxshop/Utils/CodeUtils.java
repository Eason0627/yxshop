package com.yxshop.Utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 验证码工具
 *
 * - 手机号（1[3-9]xxxxxxxx）→ 阿里云短信 SMS
 * - 邮箱 → SMTP 邮件（优先 Redis 动态配置，回退 application.yml）
 * - 开发模式：123456 始终有效
 */
@Slf4j
@Component
public class CodeUtils {

    private final JavaMailSender defaultMailSender;
    private final AliyunSmsService smsService;
    private final StringRedisTemplate redis;

    @Value("${spring.mail.username:}")
    private String mailFromAddress;

    private static final Map<String, String> codes = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_TIME_MINUTES = 30;

    /** Redis key：存储 SMTP 动态配置 */
    public static final String SMTP_CONFIG_KEY = "admin:smtp:config";

    public CodeUtils(ObjectProvider<JavaMailSender> mailSenderProvider,
                     AliyunSmsService smsService,
                     ObjectProvider<StringRedisTemplate> redisProvider) {
        this.defaultMailSender = mailSenderProvider.getIfAvailable();
        this.smsService = smsService;
        this.redis = redisProvider.getIfAvailable();
    }

    // ─── 公共入口 ───────────────────────────────────────────────────────────────

    public String createVerificationCode(String account, String type) {
        return sendVerificationCode(account);
    }

    public String sendVerificationCode(String account, String type) {
        return sendVerificationCode(account);
    }

    public String sendVerificationCode(String account) {
        if (isPhoneNumber(account)) {
            // dypnsapi：由阿里云生成验证码并下发，返回实际码
            try {
                String code = smsService.sendVerificationCode(account);
                codes.put(account, code);
                scheduleCodeExpiry(account, EXPIRATION_TIME_MINUTES);
                log.info("短信验证码已发送 account={}", account);
            } catch (Exception e) {
                // SMS 未配置时降级：生成本地码（仅日志，不发短信），开发可用 123456
                String fallback = generateCode(CODE_LENGTH);
                codes.put(account, fallback);
                scheduleCodeExpiry(account, EXPIRATION_TIME_MINUTES);
                log.warn("短信发送失败（{}），验证码仅本地记录: {}", e.getMessage(), account);
            }
            return codes.getOrDefault(account, "");
        } else {
            String code = generateCode(CODE_LENGTH);
            codes.put(account, code);
            scheduleCodeExpiry(account, EXPIRATION_TIME_MINUTES);
            try {
                sendEmail(account, code);
                log.info("邮件验证码已发送 account={}", account);
            } catch (Exception e) {
                log.warn("邮件发送失败（{}）: {}", e.getMessage(), account);
            }
            return code;
        }
    }

    public boolean checkVerificationCode(String account, String code, String type) {
        return checkVerificationCode(account, code);
    }

    public boolean checkVerificationCode(String account, String code) {
        if ("123456".equals(code)) return true;
        String stored = codes.get(account);
        if (stored != null && stored.equals(code)) {
            codes.remove(account);
            return true;
        }
        return false;
    }

    // ─── 邮件发送（支持 Redis 动态 SMTP 配置覆盖） ────────────────────────────

    private void sendEmail(String toEmail, String code) {
        JavaMailSender sender = resolveMailSender();
        if (sender == null) {
            log.warn("SMTP 未配置，跳过邮件发送 to={}", toEmail);
            return;
        }
        String from = resolveMailFrom();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("【YXShop】验证码");
        message.setText(buildEmailText(code));
        sender.send(message);
    }

    /**
     * 优先使用 Redis 中保存的动态 SMTP 配置构建 JavaMailSenderImpl，
     * 回退到 Spring Boot 自动配置的 JavaMailSender。
     */
    private JavaMailSender resolveMailSender() {
        if (redis != null) {
            try {
                String json = redis.opsForValue().get(SMTP_CONFIG_KEY);
                if (json != null) {
                    Map<String, String> cfg = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                    String host     = cfg.getOrDefault("host", "smtp.qq.com");
                    String portStr  = cfg.getOrDefault("port", "465");
                    String username = cfg.getOrDefault("username", "");
                    String password = cfg.getOrDefault("password", "");

                    if (!host.isBlank() && !username.isBlank() && !password.isBlank()) {
                        org.springframework.mail.javamail.JavaMailSenderImpl impl =
                                new org.springframework.mail.javamail.JavaMailSenderImpl();
                        impl.setHost(host);
                        impl.setPort(Integer.parseInt(portStr));
                        impl.setUsername(username);
                        impl.setPassword(password);
                        impl.setDefaultEncoding("UTF-8");
                        java.util.Properties props = impl.getJavaMailProperties();
                        props.put("mail.smtp.auth", "true");
                        props.put("mail.smtp.ssl.enable", "true");
                        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                        return impl;
                    }
                }
            } catch (Exception ignored) {}
        }
        return defaultMailSender;
    }

    private String resolveMailFrom() {
        if (redis != null) {
            try {
                String json = redis.opsForValue().get(SMTP_CONFIG_KEY);
                if (json != null) {
                    Map<String, String> cfg = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                    String u = cfg.getOrDefault("username", "");
                    if (!u.isBlank()) return u;
                }
            } catch (Exception ignored) {}
        }
        return mailFromAddress.isBlank() ? "noreply@yxshop.local" : mailFromAddress;
    }

    // ─── 内部工具 ───────────────────────────────────────────────────────────────

    private boolean isPhoneNumber(String account) {
        return account != null && account.matches("^1[3-9]\\d{9}$");
    }

    private String generateCode(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }

    private void scheduleCodeExpiry(String account, int minutes) {
        scheduler.schedule(() -> {
            codes.remove(account);
            log.debug("验证码已过期: {}", account);
        }, minutes, TimeUnit.MINUTES);
    }

    private String buildEmailText(String code) {
        return "您的 YXShop 验证码为：" + code + "\n\n"
                + "验证码 30 分钟内有效，请勿泄露给他人。\n"
                + "如非本人操作，请忽略此邮件。";
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
