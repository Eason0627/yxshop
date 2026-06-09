package com.yxshop.Config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yxshop.Module.Admin.Entity.AdminUserEntity;
import com.yxshop.Module.Admin.Mapper.AdminUserMapper;
import com.yxshop.Module.Auth.Entity.AuthTokenEntity;
import com.yxshop.Module.User.Entity.User;
import com.yxshop.Module.User.Mapper.UserMapper;
import com.yxshop.Module.User.Entity.UserProfileEntity;
import com.yxshop.Module.User.Mapper.UserProfileMapper;
import com.yxshop.Utils.SnowflakeIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 种子数据初始化器（在 DataInitializer 之后运行）
 * 负责创建新版 admin_user 管理员、app 测试用户
 */
@Slf4j
@Component
@Order(2)
public class SeedDataInitializer implements CommandLineRunner {

    private final AdminUserMapper adminUserMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(2, 1);

    @Value("${yxshop.init.admin.username:admin}")
    private String adminUsername;

    @Value("${yxshop.init.admin.password:Admin@123456}")
    private String adminPassword;

    @Value("${yxshop.init.admin.email:admin@yxshop.local}")
    private String adminEmail;

    public SeedDataInitializer(AdminUserMapper adminUserMapper,
                               UserMapper userMapper,
                               UserProfileMapper userProfileMapper) {
        this.adminUserMapper = adminUserMapper;
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureAdminUser();
        ensureAppTestUsers();
    }

    private void ensureAdminUser() {
        AdminUserEntity existing = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUserEntity>().eq(AdminUserEntity::getUsername, adminUsername));
        if (existing != null) {
            String newHash = BCrypt.hashpw(adminPassword, BCrypt.gensalt());
            if (existing.getPasswordHash() == null || !existing.getPasswordHash().startsWith("$2a$")) {
                existing.setPasswordHash(newHash);
                adminUserMapper.updateById(existing);
                log.info("已更新管理员密码哈希: {}", adminUsername);
            } else {
                log.info("管理员账号已存在: {}", adminUsername);
            }
            return;
        }

        AdminUserEntity admin = new AdminUserEntity();
        admin.setId(idWorker.nextId());
        admin.setUsername(adminUsername);
        admin.setPasswordHash(BCrypt.hashpw(adminPassword, BCrypt.gensalt()));
        admin.setRealName("系统管理员");
        admin.setNickname("Admin");
        admin.setEmail(adminEmail);
        admin.setStatus(1);
        admin.setIsSuperAdmin(1);
        adminUserMapper.insert(admin);
        log.info("初始化后台管理员账号: {} / {}", adminUsername, adminPassword);
    }

    private void ensureAppTestUsers() {
        createAppUserIfAbsent("testuser", "Test@123", "test@yxshop.local", "13800000001",
                "Customer", "测试用户");
        createAppUserIfAbsent("shop_digital", "Test@123", "digital@yxshop.local", "13800000006",
                "ShopOwner", "数码精品员");
        createAppUserIfAbsent("shop_food", "Test@123", "food@yxshop.local", "13800000007",
                "ShopOwner", "生鲜食品员");
        log.info("测试用户初始化完成 (密码均为: Test@123)");
    }

    private void createAppUserIfAbsent(String username, String password, String email,
                                        String phone, String role, String nickname) {
        User existing = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("username", username).last("limit 1"));
        if (existing != null) {
            return;
        }

        Long userId = idWorker.nextId();
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(role);
        user.setNick_name(nickname);
        user.setStatus("Active");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        // 同步创建 user_profile
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(userId);
        profile.setNickname(nickname);
        profile.setPhone(phone);
        profile.setRegisteredAt(LocalDateTime.now());
        profile.setGender(0);
        profile.setStatus(1);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        userProfileMapper.insert(profile);
    }
}
