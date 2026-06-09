package com.yxshop.Config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yxshop.Module.Shop.Entity.Shop;
import com.yxshop.Module.User.Entity.User;
import com.yxshop.Module.Shop.Mapper.ShopMapper;
import com.yxshop.Module.User.Mapper.UserMapper;
import com.yxshop.Utils.SnowflakeIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final ShopMapper shopMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(1, 1);

    @Value("${yxshop.init.admin.username:admin}")
    private String adminUsername;

    @Value("${yxshop.init.admin.password:Admin@123456}")
    private String adminPassword;

    @Value("${yxshop.init.admin.email:admin@yxshop.local}")
    private String adminEmail;

    @Value("${yxshop.init.platform-shop.name:平台自营店}")
    private String platformShopName;

    public DataInitializer(UserMapper userMapper, ShopMapper shopMapper) {
        this.userMapper = userMapper;
        this.shopMapper = shopMapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User admin = ensureAdminUser();
        ensurePlatformShop(admin.getId());
    }

    private User ensureAdminUser() {
        User existing = userMapper.selectOne(new QueryWrapper<User>()
                .eq("username", adminUsername)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }

        User admin = new User();
        admin.setId(idWorker.nextId());
        admin.setUsername(adminUsername);
        admin.setNick_name("平台管理员");
        admin.setEmail(adminEmail);
        admin.setPassword(BCrypt.hashpw(adminPassword, BCrypt.gensalt()));
        admin.setRole("Admin");
        admin.setStatus("Active");
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        userMapper.insert(admin);
        log.info("初始化默认管理员账号：{}", adminUsername);
        return admin;
    }

    private void ensurePlatformShop(Long adminUserId) {
        Shop existing = shopMapper.selectOne(new QueryWrapper<Shop>()
                .eq("shop_name", platformShopName)
                .last("limit 1"));
        if (existing != null) {
            return;
        }

        Shop shop = new Shop();
        shop.setShop_id(idWorker.nextId());
        shop.setShop_name(platformShopName);
        shop.setOwner_user_id(adminUserId);
        shop.setPhone("00000000000");
        shop.setLocation("平台默认店铺");
        shop.setRegistration_date(LocalDate.now());
        shop.setShop_description("平台初始化自营店铺，用于默认商品、测试和运营数据员");
        shop.setShop_image("");
        shop.setStatus("Active");
        shop.setCreateTime(LocalDateTime.now());
        shop.setUpdateTime(LocalDateTime.now());
        shopMapper.insert(shop);
        log.info("初始化平台店铺：{}", platformShopName);
    }
}
