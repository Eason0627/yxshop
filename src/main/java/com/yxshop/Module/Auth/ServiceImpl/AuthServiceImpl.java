package com.yxshop.Module.Auth.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.yxshop.Module.User.Entity.User;
import com.yxshop.Module.User.Mapper.UserMapper;
import com.yxshop.Module.Auth.Dto.LoginDto;
import com.yxshop.Module.Auth.Dto.BindPhoneDto;
import com.yxshop.Module.Auth.Dto.PasswordChangeDto;
import com.yxshop.Module.Auth.Dto.PasswordResetDto;
import com.yxshop.Module.Auth.Dto.RegisterDto;
import com.yxshop.Module.Admin.Entity.AdminUserEntity;
import com.yxshop.Module.Admin.Mapper.AdminUserMapper;
import com.yxshop.Module.Auth.Dto.SendCodeDto;
import com.yxshop.Module.Auth.Service.AuthService;
import com.yxshop.Module.Auth.Vo.LoginVo;
import com.yxshop.Module.Points.Entity.PointsAccountEntity;
import com.yxshop.Module.Points.Service.PointsAccountService;
import com.yxshop.Module.User.Entity.UserProfileEntity;
import com.yxshop.Module.User.Service.UserProfileService;
import com.yxshop.Utils.CodeUtils;
import com.yxshop.Utils.JwtUtils;
import com.yxshop.Utils.SnowflakeIdWorker;
import io.jsonwebtoken.Claims;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_BLACKLIST_PREFIX = "yxshop:auth:blacklist:";

    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;
    private final CodeUtils codeUtils;
    private final UserProfileService userProfileService;
    private final PointsAccountService pointsAccountService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(1, 1);

    public AuthServiceImpl(UserMapper userMapper,
                           AdminUserMapper adminUserMapper,
                           CodeUtils codeUtils,
                           UserProfileService userProfileService,
                           PointsAccountService pointsAccountService,
                           ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider) {
        this.userMapper = userMapper;
        this.adminUserMapper = adminUserMapper;
        this.codeUtils = codeUtils;
        this.userProfileService = userProfileService;
        this.pointsAccountService = pointsAccountService;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVo login(LoginDto loginDto) {
        requireNotBlank(loginDto.getAccount(), "账号不能为空");
        User user = findUserByAccount(loginDto.getAccount());
        if (!isBlank(loginDto.getCode())) {
            // 验证码登录
            String scene = firstNotBlank(loginDto.getScene(), "login");
            if (!codeUtils.checkVerificationCode(loginDto.getAccount(), loginDto.getCode(), scene)) {
                throw new IllegalArgumentException("验证码错误或已过期");
            }
            // 用户不存在时自动注册（手机号验证码登录）
            if (user == null) {
                user = autoRegisterByPhone(loginDto.getAccount());
            }
        } else {
            if (user == null) {
                throw new IllegalArgumentException("账号或密码错误");
            }
            requireNotBlank(loginDto.getPassword(), "密码不能为空");
            if (!matchPassword(loginDto.getPassword(), user.getPassword())) {
                throw new IllegalArgumentException("账号或密码错误");
            }
        }
        if ("Invalid".equalsIgnoreCase(user.getStatus()) || "Inactive".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("账号已停用");
        }
        userProfileService.ensureProfile(user);
        ensurePointsAccount(user.getId());
        return buildLoginVo(user);
    }

    /** 手机号验证码登录时自动注册新用户 */
    private User autoRegisterByPhone(String phone) {
        User user = new User();
        user.setId(idWorker.nextId());
        user.setUsername(phone);
        user.setPhone(phone);
        user.setNick_name("用户" + phone.substring(phone.length() - 4));
        // 生成随机占位密码（用户无法直接使用，仅满足 NOT NULL 约束）
        user.setPassword(BCrypt.hashpw(java.util.UUID.randomUUID().toString(), BCrypt.gensalt()));
        user.setRole("Customer");
        user.setStatus("Active");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.addUser(user);
        return user;
    }

    @Override
    public LoginVo adminLogin(LoginDto loginDto) {
        requireNotBlank(loginDto.getAccount(), "管理员账号不能为空");
        requireNotBlank(loginDto.getPassword(), "管理员密码不能为空");
        AdminUserEntity admin = findAdminByAccount(loginDto.getAccount());
        if (admin == null || !matchPassword(loginDto.getPassword(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("管理员账号或密码错误");
        }
        if (!Integer.valueOf(1).equals(admin.getStatus())) {
            throw new IllegalArgumentException("管理员账号已停用");
        }
        admin.setLastLoginAt(LocalDateTime.now());
        adminUserMapper.updateById(admin);
        return buildAdminLoginVo(admin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVo register(RegisterDto registerDto) {
        requireNotBlank(registerDto.getEmail(), "邮箱不能为空");
        requireNotBlank(registerDto.getPassword(), "密码不能为空");
        requireNotBlank(registerDto.getCode(), "验证码不能为空");
        if (!codeUtils.checkVerificationCode(registerDto.getEmail(), registerDto.getCode(), registerDto.getScene())) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        String username = firstNotBlank(registerDto.getUsername(), registerDto.getEmail());
        if (findUserByAccount(username) != null || findUserByAccount(registerDto.getEmail()) != null) {
            throw new IllegalArgumentException("账号已存在");
        }
        User user = new User();
        user.setId(idWorker.nextId());
        user.setUsername(username);
        user.setNick_name(firstNotBlank(registerDto.getNickname(), username));
        user.setEmail(registerDto.getEmail());
        user.setPhone(registerDto.getPhone());
        user.setPassword(BCrypt.hashpw(registerDto.getPassword(), BCrypt.gensalt()));
        user.setRole("Customer");
        user.setStatus("Active");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.addUser(user);
        userProfileService.ensureProfile(user);
        ensurePointsAccount(user.getId());
        return buildLoginVo(user);
    }

    @Override
    public void sendCode(SendCodeDto sendCodeDto) {
        String account = firstNotBlank(sendCodeDto.getAccount(), firstNotBlank(sendCodeDto.getEmail(), sendCodeDto.getPhone()));
        requireNotBlank(account, "账号不能为空");
        String scene = firstNotBlank(sendCodeDto.getScene(), "login");
        // 统一走 sendVerificationCode：邮箱发邮件，手机号发短信（SMS 未配置时静默降级）
        codeUtils.sendVerificationCode(account, scene);
    }

    @Override
    public boolean verifyCode(String account, String code) {
        return codeUtils.checkVerificationCode(account, code);
    }

    @Override
    public void resetPassword(PasswordResetDto resetDto) {
        String account = resetDto.resolveAccount();
        requireNotBlank(account, "手机号或邮箱不能为空");
        requireNotBlank(resetDto.getCode(), "验证码不能为空");
        requireNotBlank(resetDto.getNewPassword(), "新密码不能为空");
        if (!codeUtils.checkVerificationCode(account, resetDto.getCode(), resetDto.getScene())) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        User user = findUserByAccount(account);
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        updatePassword(user.getId(), resetDto.getNewPassword());
    }

    @Override
    public void changePassword(Long userId, PasswordChangeDto changeDto) {
        requireNotBlank(changeDto.getOldPassword(), "旧密码不能为空");
        requireNotBlank(changeDto.getNewPassword(), "新密码不能为空");
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!matchPassword(changeDto.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }
        updatePassword(userId, changeDto.getNewPassword());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindPhone(Long userId, BindPhoneDto bindPhoneDto) {
        requireNotBlank(bindPhoneDto.getPhone(), "手机号不能为空");
        requireNotBlank(bindPhoneDto.getCode(), "验证码不能为空");
        String scene = firstNotBlank(bindPhoneDto.getScene(), "bindPhone");
        if (!codeUtils.checkVerificationCode(bindPhoneDto.getPhone(), bindPhoneDto.getCode(), scene)) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        QueryWrapper<User> existedWrapper = new QueryWrapper<>();
        existedWrapper.eq("phone", bindPhoneDto.getPhone())
                .ne("id", userId)
                .last("LIMIT 1");
        if (userMapper.selectOne(existedWrapper) != null) {
            throw new IllegalArgumentException("手机号已被绑定");
        }
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", userId)
                .set("phone", bindPhoneDto.getPhone())
                .set("updateTime", LocalDateTime.now());
        userMapper.update(null, wrapper);

        UserProfileEntity profile = userProfileService.getById(userId);
        if (profile != null) {
            profile.setPhone(bindPhoneDto.getPhone());
            profile.setUpdatedAt(LocalDateTime.now());
            userProfileService.updateById(profile);
        }
    }

    @Override
    public void logout(String authorizationHeader) {
        String token = normalizeToken(authorizationHeader);
        requireNotBlank(token, "token不能为空");
        Claims claims = JwtUtils.checkToken(token);
        Date expiration = claims == null ? null : claims.getExpiration();
        if (expiration == null) {
            throw new IllegalArgumentException("无效token");
        }
        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis > 0 && redisTemplate != null) {
            redisTemplate.opsForValue().set(tokenBlacklistKey(token), "1", ttlMillis, TimeUnit.MILLISECONDS);
        }
    }

    private LoginVo buildLoginVo(User user) {
        LoginVo vo = new LoginVo();
        vo.setToken(JwtUtils.getSecret(user));
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setUserType(resolveUserType(user.getRole()));
        return vo;
    }

    private LoginVo buildAdminLoginVo(AdminUserEntity admin) {
        LoginVo vo = new LoginVo();
        vo.setToken(JwtUtils.getSecret(admin.getId(), admin.getUsername(), "Admin"));
        vo.setUserId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setRole("Admin");
        vo.setUserType("admin");
        return vo;
    }

    private User findUserByAccount(String account) {
        if (isBlank(account)) {
            return null;
        }
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", account)
                .or()
                .eq("email", account)
                .or()
                .eq("phone", account)
                .last("LIMIT 1");
        return userMapper.selectOne(wrapper);
    }

    private AdminUserEntity findAdminByAccount(String account) {
        if (isBlank(account)) {
            return null;
        }
        QueryWrapper<AdminUserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("username", account)
                .or()
                .eq("email", account)
                .or()
                .eq("mobile", account)
                .last("LIMIT 1");
        return adminUserMapper.selectOne(wrapper);
    }

    private boolean matchPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return BCrypt.checkpw(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    private void updatePassword(Long userId, String newPassword) {
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", userId)
                .set("password", BCrypt.hashpw(newPassword, BCrypt.gensalt()))
                .set("updateTime", LocalDateTime.now());
        userMapper.update(null, wrapper);
    }

    private void ensurePointsAccount(Long userId) {
        if (pointsAccountService.getById(userId) != null) {
            return;
        }
        PointsAccountEntity account = new PointsAccountEntity();
        account.setUserId(userId);
        account.setCurrentPoints(0);
        account.setTotalEarned(0);
        account.setTotalSpent(0);
        account.setExpireDate(LocalDate.now().plusYears(1));
        account.setUpdatedAt(LocalDateTime.now());
        pointsAccountService.save(account);
    }

    private String resolveUserType(String role) {
        if ("Admin".equals(role)) {
            return "admin";
        }
        if ("ShopOwner".equals(role)) {
            return "shop_owner";
        }
        return "customer";
    }

    private void requireNotBlank(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private String firstNotBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private String normalizeToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        String token = authorizationHeader.trim();
        if (token.startsWith("Bearer ")) {
            return token.substring(7).trim();
        }
        return token;
    }

    private String tokenBlacklistKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + Integer.toHexString(token.hashCode());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
