package com.yxshop.Module.Auth.Controller;

import com.yxshop.Module.Auth.Dto.LoginDto;
import com.yxshop.Module.Auth.Dto.BindPhoneDto;
import com.yxshop.Module.Auth.Dto.PasswordChangeDto;
import com.yxshop.Module.Auth.Dto.PasswordResetDto;
import com.yxshop.Module.Auth.Dto.RegisterDto;
import com.yxshop.Module.Auth.Dto.SendCodeDto;
import com.yxshop.Module.Auth.Service.AuthService;
import com.yxshop.Utils.CaptchaService;
import com.yxshop.Utils.Result;
import com.yxshop.Utils.SelfSliderCaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/app/auth")
@Tag(name = "App Auth", description = "App 登录、注册、验证码和密码接口")
public class AuthController {

    private final AuthService               authService;
    private final SelfSliderCaptchaService  sliderCaptcha;
    private final CaptchaService            captchaService;

    public AuthController(AuthService authService,
                          SelfSliderCaptchaService sliderCaptcha,
                          CaptchaService captchaService) {
        this.authService    = authService;
        this.sliderCaptcha  = sliderCaptcha;
        this.captchaService = captchaService;
    }

    /** App 端查询阿里云验证码配置（未配置时返回 enabled=false，前端据此降级） */
    @GetMapping("/captcha-scene")
    @Operation(summary = "查询验证码场景配置")
    public Result captchaScene() {
        boolean enabled = captchaService.isEnabled();
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("enabled", enabled);
        if (enabled) {
            result.put("sceneId", captchaService.getSceneId());
            result.put("prefix",  captchaService.getPrefix());
        }
        return Result.success(result);
    }

    // ── 自研滑块验证码 ─────────────────────────────────────────────────────────

    @GetMapping("/slider/generate")
    @Operation(summary = "生成滑块验证码挑战")
    public Result generateSlider() {
        return Result.success(sliderCaptcha.generate());
    }

    @PostMapping("/slider/verify")
    @Operation(summary = "校验滑块位置，返回一次性 token")
    public Result verifySlider(@RequestBody Map<String, Object> body) {
        String captchaId = (String) body.get("captchaId");
        int x = ((Number) body.get("x")).intValue();
        String token = sliderCaptcha.verify(captchaId, x);
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return Result.success(result);
    }

    // ── 登录 ──────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "登录")
    public Result login(@RequestBody LoginDto loginDto) {
        verifySliderToken(loginDto.getCaptchaVerifyParam());
        return Result.success(authService.login(loginDto));
    }

    @PostMapping("/admin/login")
    @Operation(summary = "后台管理员登录")
    public Result adminLogin(@RequestBody LoginDto loginDto) {
        verifySliderToken(loginDto.getCaptchaVerifyParam());
        return Result.success(authService.adminLogin(loginDto));
    }

    /**
     * 校验自研滑块 token。
     * token 为空时放行（支持 API 直接调用 / 开发环境）。
     * token 非空时必须通过（防止绕过）。
     */
    private void verifySliderToken(String token) {
        if (token == null || token.isBlank()) return;
        if (!sliderCaptcha.verifyToken(token)) {
            throw new IllegalArgumentException("人机验证失败，请重新拼图");
        }
    }

    // ── 退出 / 注册 ───────────────────────────────────────────────────────────

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result logout(HttpServletRequest request) {
        authService.logout(request.getHeader("Authorization"));
        return Result.success("已退出登录");
    }

    @PostMapping("/register")
    @Operation(summary = "注册")
    public Result register(@RequestBody RegisterDto registerDto) {
        return Result.success(authService.register(registerDto));
    }

    // ── 验证码（短信/邮件）──────────────────────────────────────────────────────

    @PostMapping("/send-code")
    @Operation(summary = "发送验证码（手机短信或邮件）")
    public Result sendCode(@RequestBody SendCodeDto sendCodeDto) {
        authService.sendCode(sendCodeDto);
        return Result.success("验证码已发送");
    }

    @PostMapping("/verify-code")
    @Operation(summary = "校验验证码（注册/表单验证场景）")
    public Result verifyCode(@RequestBody Map<String, String> body) {
        String account = body.get("account");
        String code    = body.get("code");
        if (account == null || account.isBlank() || code == null || code.isBlank()) {
            return Result.error("参数缺失");
        }
        boolean valid = authService.verifyCode(account, code);
        Map<String, Object> result = new HashMap<>();
        result.put("valid", valid);
        return Result.success(result);
    }

    // ── 密码 ──────────────────────────────────────────────────────────────────

    @PutMapping("/reset-password")
    @Operation(summary = "验证码重置密码")
    public Result resetPassword(@RequestBody PasswordResetDto resetDto) {
        authService.resetPassword(resetDto);
        return Result.success("密码已重置");
    }

    @PutMapping("/change-password")
    @Operation(summary = "登录后修改密码")
    public Result changePassword(HttpServletRequest request, @RequestBody PasswordChangeDto changeDto) {
        authService.changePassword(currentUserId(request), changeDto);
        return Result.success("密码已修改");
    }

    @PutMapping("/bind-phone")
    @Operation(summary = "绑定或换绑手机号")
    public Result bindPhone(HttpServletRequest request, @RequestBody BindPhoneDto bindPhoneDto) {
        authService.bindPhone(currentUserId(request), bindPhoneDto);
        return Result.success("手机号已绑定");
    }

    @GetMapping("/check")
    @Operation(summary = "检查登录态")
    public Result check(HttpServletRequest request) {
        return Result.success(currentUserId(request));
    }

    private Long currentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value == null) throw new IllegalArgumentException("请先登录");
        return Long.valueOf(String.valueOf(value));
    }
}
