package com.yxshop.Module.Admin.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yxshop.Module.Admin.Dto.AdminUserDto;
import com.yxshop.Module.Admin.Entity.AdminLoginLogEntity;
import com.yxshop.Module.Admin.Entity.AdminMenuEntity;
import com.yxshop.Module.Admin.Entity.AdminRoleEntity;
import com.yxshop.Module.Admin.Entity.AdminUserEntity;
import com.yxshop.Module.Admin.Entity.OperationLog;
import com.yxshop.Module.Admin.Mapper.AdminLoginLogMapper;
import com.yxshop.Module.Admin.Mapper.AdminMenuMapper;
import com.yxshop.Module.Admin.Mapper.AdminRoleMapper;
import com.yxshop.Module.Admin.Mapper.AdminUserMapper;
import com.yxshop.Module.Admin.Mapper.OPerationLogMapper;
import com.yxshop.Module.Admin.Service.AdminUserService;
import com.yxshop.Module.Admin.Vo.AdminUserVo;
import com.yxshop.Utils.JwtUtils;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AdminMenuMapper adminMenuMapper;
    private final AdminLoginLogMapper adminLoginLogMapper;
    private final OPerationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(30, 1);

    @Autowired
    @Lazy
    private StringRedisTemplate stringRedisTemplate;

    private static final String SYS_CONFIG_KEY = "admin:system:config";

    public AdminUserServiceImpl(AdminUserMapper adminUserMapper,
                                AdminRoleMapper adminRoleMapper,
                                AdminMenuMapper adminMenuMapper,
                                AdminLoginLogMapper adminLoginLogMapper,
                                OPerationLogMapper operationLogMapper) {
        this.adminUserMapper = adminUserMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.adminMenuMapper = adminMenuMapper;
        this.adminLoginLogMapper = adminLoginLogMapper;
        this.operationLogMapper = operationLogMapper;
    }

    // ── 登录 / 登出 ───────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> login(String username, String password, String ip) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new RuntimeException("用户名和密码不能为空");
        }
        AdminUserEntity admin = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUserEntity>().eq(AdminUserEntity::getUsername, username));
        if (admin == null) {
            recordLoginLog(null, username, ip, 0, "用户名不存在");
            throw new RuntimeException("用户名或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            recordLoginLog(admin.getId(), username, ip, 0, "账号已被禁用");
            throw new RuntimeException("账号已被禁用");
        }
        if (!BCrypt.checkpw(password, admin.getPasswordHash())) {
            String stored = admin.getPasswordHash();
            if (stored != null && !stored.startsWith("$2a$") && stored.equals(password)) {
                admin.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
                adminUserMapper.updateById(admin);
            } else {
                recordLoginLog(admin.getId(), username, ip, 0, "密码错误");
                throw new RuntimeException("用户名或密码错误");
            }
        }
        String role = admin.getIsSuperAdmin() != null && admin.getIsSuperAdmin() == 1 ? "Admin" : "Staff";
        String token = JwtUtils.getSecret(admin.getId(), admin.getUsername(), role);
        if (stringRedisTemplate != null) {
            stringRedisTemplate.opsForValue().set("admin:token:" + admin.getId(), token, Duration.ofHours(24));
        }
        admin.setLastLoginAt(LocalDateTime.now());
        adminUserMapper.updateById(admin);
        recordLoginLog(admin.getId(), username, ip, 1, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("userId", admin.getId());
        result.put("username", admin.getUsername());
        result.put("realName", admin.getRealName());
        result.put("role", role);
        return result;
    }

    @Override
    public void logout(Long adminUserId, String token) {
        if (stringRedisTemplate != null && token != null) {
            stringRedisTemplate.opsForValue().set("admin:blacklist:" + token, "1", Duration.ofHours(24));
            stringRedisTemplate.delete("admin:token:" + adminUserId);
        }
    }

    @Override
    public AdminUserVo getCurrentAdmin(Long adminUserId) {
        AdminUserEntity admin = adminUserMapper.selectById(adminUserId);
        if (admin == null) throw new RuntimeException("管理员不存在");
        return toVo(admin);
    }

    @Override
    public void changePassword(Long adminId, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(oldPassword)) throw new IllegalArgumentException("请输入当前密码");
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6)
            throw new IllegalArgumentException("新密码不能少于 6 位");
        AdminUserEntity admin = adminUserMapper.selectById(adminId);
        if (admin == null) throw new RuntimeException("管理员不存在");
        if (!BCrypt.checkpw(oldPassword, admin.getPasswordHash())) {
            throw new IllegalArgumentException("当前密码错误");
        }
        admin.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        adminUserMapper.updateById(admin);
    }

    // ── 管理员 CRUD ───────────────────────────────────────────────────────────

    @Override
    public Object listAdmins(Integer page, Integer size, String keyword) {
        LambdaQueryWrapper<AdminUserEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(AdminUserEntity::getUsername, keyword)
                    .or().like(AdminUserEntity::getRealName, keyword)
                    .or().like(AdminUserEntity::getEmail, keyword));
        }
        wrapper.orderByDesc(AdminUserEntity::getId);
        Page<AdminUserEntity> result = adminUserMapper.selectPage(new Page<>(safePage(page), safeSize(size)), wrapper);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", result.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVo createAdmin(AdminUserDto dto) {
        if (!StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            throw new RuntimeException("用户名和密码不能为空");
        }
        AdminUserEntity existing = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUserEntity>().eq(AdminUserEntity::getUsername, dto.getUsername()));
        if (existing != null) throw new RuntimeException("用户名已存在");
        AdminUserEntity admin = new AdminUserEntity();
        admin.setId(idWorker.nextId());
        admin.setUsername(dto.getUsername());
        admin.setPasswordHash(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        admin.setRealName(dto.getRealName());
        admin.setMobile(dto.getMobile());
        admin.setEmail(dto.getEmail());
        admin.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        admin.setIsSuperAdmin(dto.getIsSuperAdmin() != null ? dto.getIsSuperAdmin() : 0);
        adminUserMapper.insert(admin);
        return toVo(admin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVo updateAdmin(Long operatorId, AdminUserDto dto) {
        if (dto.getId() == null) throw new RuntimeException("管理员ID不能为空");
        AdminUserEntity admin = adminUserMapper.selectById(dto.getId());
        if (admin == null) throw new RuntimeException("管理员不存在");
        if (StringUtils.hasText(dto.getPassword())) {
            admin.setPasswordHash(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        }
        if (dto.getRealName() != null) admin.setRealName(dto.getRealName());
        if (dto.getMobile() != null) admin.setMobile(dto.getMobile());
        if (dto.getEmail() != null) admin.setEmail(dto.getEmail());
        if (dto.getStatus() != null) admin.setStatus(dto.getStatus());
        if (dto.getIsSuperAdmin() != null) admin.setIsSuperAdmin(dto.getIsSuperAdmin());
        adminUserMapper.updateById(admin);
        return toVo(admin);
    }

    @Override
    public void deleteAdmin(Long operatorId, Long targetId) {
        AdminUserEntity admin = adminUserMapper.selectById(targetId);
        if (admin == null) throw new RuntimeException("管理员不存在");
        if (admin.getIsSuperAdmin() != null && admin.getIsSuperAdmin() == 1) {
            throw new RuntimeException("超级管理员不能删除");
        }
        adminUserMapper.deleteById(targetId);
    }

    // ── 角色 CRUD ─────────────────────────────────────────────────────────────

    @Override
    public Object listRoles() {
        List<AdminRoleEntity> roles = adminRoleMapper.selectList(
                new LambdaQueryWrapper<AdminRoleEntity>().orderByAsc(AdminRoleEntity::getSort));
        return roles.stream().map(this::roleToMap).collect(Collectors.toList());
    }

    @Override
    public Object createRole(Map<String, Object> dto) {
        AdminRoleEntity role = new AdminRoleEntity();
        role.setId(idWorker.nextId());
        role.setRoleCode("ROLE_" + role.getId());
        role.setRoleName(strVal(dto, "name"));
        role.setStatus(intVal(dto, "status", 1));
        role.setSort(intVal(dto, "sort", 0));
        // 把 icon/description 存入 remark JSON
        role.setRemark(extraJson(dto));
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        adminRoleMapper.insert(role);
        return roleToMap(role);
    }

    @Override
    public Object updateRole(Long id, Map<String, Object> dto) {
        AdminRoleEntity role = adminRoleMapper.selectById(id);
        if (role == null) throw new RuntimeException("角色不存在");
        if (dto.containsKey("name")) role.setRoleName(strVal(dto, "name"));
        if (dto.containsKey("status")) role.setStatus(intVal(dto, "status", role.getStatus()));
        if (dto.containsKey("sort")) role.setSort(intVal(dto, "sort", role.getSort()));
        // 合并 icon/description 到 remark JSON
        Map<String, Object> extra = parseRemark(role.getRemark());
        if (dto.containsKey("icon")) extra.put("icon", dto.get("icon"));
        if (dto.containsKey("description")) extra.put("description", dto.get("description"));
        role.setRemark(toJson(extra));
        role.setUpdatedAt(LocalDateTime.now());
        adminRoleMapper.updateById(role);
        return roleToMap(role);
    }

    @Override
    public void deleteRole(Long id) {
        AdminRoleEntity role = adminRoleMapper.selectById(id);
        if (role == null) throw new RuntimeException("角色不存在");
        adminRoleMapper.deleteById(id);
    }

    // ── 角色权限 ──────────────────────────────────────────────────────────────

    @Override
    public List<String> getRolePermissions(Long roleId) {
        AdminRoleEntity role = adminRoleMapper.selectById(roleId);
        if (role == null) return Collections.emptyList();
        Map<String, Object> extra = parseRemark(role.getRemark());
        Object perms = extra.get("permissions");
        if (perms instanceof List) {
            return ((List<?>) perms).stream().map(Object::toString).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public void saveRolePermissions(Long roleId, List<String> permissions) {
        AdminRoleEntity role = adminRoleMapper.selectById(roleId);
        if (role == null) throw new RuntimeException("角色不存在");
        Map<String, Object> extra = parseRemark(role.getRemark());
        extra.put("permissions", permissions);
        role.setRemark(toJson(extra));
        role.setUpdatedAt(LocalDateTime.now());
        adminRoleMapper.updateById(role);
    }

    // ── 菜单树 ────────────────────────────────────────────────────────────────

    @Override
    public Object listMenuTree(Long adminUserId) {
        AdminUserEntity admin = adminUserMapper.selectById(adminUserId);
        if (admin == null) return new ArrayList<>();
        List<AdminMenuEntity> allMenus = adminMenuMapper.selectList(
                new LambdaQueryWrapper<AdminMenuEntity>()
                        .eq(AdminMenuEntity::getStatus, 1)
                        .eq(AdminMenuEntity::getIsVisible, 1)
                        .orderByAsc(AdminMenuEntity::getSort));
        return buildMenuTree(allMenus, 0L);
    }

    // ── 登录日志 ──────────────────────────────────────────────────────────────

    @Override
    public Object listLoginLogs(Integer page, Integer size, Long adminUserId,
                                String keyword, String startDate, String endDate) {
        QueryWrapper<AdminLoginLogEntity> wrapper = new QueryWrapper<>();
        if (adminUserId != null) wrapper.eq("admin_user_id", adminUserId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("username", keyword).or().like("login_ip", keyword));
        }
        if (StringUtils.hasText(startDate)) wrapper.ge("login_at", startDate + " 00:00:00");
        if (StringUtils.hasText(endDate))   wrapper.le("login_at", endDate   + " 23:59:59");
        wrapper.orderByDesc("login_at");
        Page<AdminLoginLogEntity> result = adminLoginLogMapper.selectPage(
                new Page<>(safePage(page), safeSize(size)), wrapper);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return data;
    }

    // ── 操作日志 ──────────────────────────────────────────────────────────────

    @Override
    public Object listOperationLogs(Integer page, Integer size, String keyword,
                                    String module, String action,
                                    String startDate, String endDate) {
        QueryWrapper<OperationLog> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("operation", keyword).or().like("request_url", keyword));
        }
        if (StringUtils.hasText(module))    wrapper.like("request_url", "/" + module + "/");
        if (StringUtils.hasText(action)) {
            switch (action) {
                case "create": wrapper.eq("method", "POST"); break;
                case "update": wrapper.eq("method", "PUT"); break;
                case "delete": wrapper.eq("method", "DELETE"); break;
                case "query":  wrapper.eq("method", "GET"); break;
            }
        }
        if (StringUtils.hasText(startDate)) wrapper.ge("create_time", startDate + " 00:00:00");
        if (StringUtils.hasText(endDate))   wrapper.le("create_time", endDate   + " 23:59:59");
        wrapper.orderByDesc("create_time");
        Page<OperationLog> result = operationLogMapper.selectPage(
                new Page<>(safePage(page), safeSize(size)), wrapper);

        // 补充管理员用户名
        Map<Long, String> nameCache = new HashMap<>();
        List<Map<String, Object>> records = result.getRecords().stream().map(log -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", log.getId());
            row.put("adminId", log.getUser_id());
            row.put("adminName", nameCache.computeIfAbsent(log.getUser_id(), uid -> {
                AdminUserEntity u = adminUserMapper.selectById(uid);
                return u != null ? (StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername()) : "—";
            }));
            row.put("operation", log.getOperation());
            row.put("requestPath", log.getRequest_url());
            row.put("method", log.getMethod());
            row.put("ip", log.getIp());
            row.put("params", log.getParams());
            row.put("success", !"ERROR".equalsIgnoreCase(log.getResult_type()));
            row.put("costTime", log.getCost_time());
            row.put("createTime", log.getCreate_time() != null ? log.getCreate_time().toString().replace("T", " ") : null);
            // 推断 module/action
            row.put("module", inferModule(log.getRequest_url()));
            row.put("action", inferAction(log.getMethod()));
            row.put("description", buildDescription(log));
            return row;
        }).collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        return data;
    }

    // ── 系统配置 ──────────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> getSystemConfig() {
        if (stringRedisTemplate != null) {
            String json = stringRedisTemplate.opsForValue().get(SYS_CONFIG_KEY);
            if (StringUtils.hasText(json)) {
                try {
                    return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                } catch (Exception ignored) {}
            }
        }
        return defaultSystemConfig();
    }

    @Override
    public void saveSystemConfig(String section, Map<String, Object> data) {
        Map<String, Object> config = getSystemConfig();
        config.put(section, data);
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.opsForValue().set(SYS_CONFIG_KEY,
                        objectMapper.writeValueAsString(config),
                        Duration.ofDays(365));
            } catch (Exception ignored) {}
        }
    }

    // ── 内部工具 ──────────────────────────────────────────────────────────────

    private Map<String, Object> defaultSystemConfig() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("basic", new LinkedHashMap<String, Object>() {{
            put("platformName", "YXShop 优选商城");
            put("platformLogo", "");
            put("platformDesc", "精选全球好物，品质生活首选");
            put("contactEmail", "support@yxshop.local");
            put("servicePhone", "400-888-8888");
        }});
        m.put("auth", new LinkedHashMap<String, Object>() {{
            put("allowPhoneRegister", true);
            put("allowEmailRegister", true);
            put("allowWechatLogin", true);
            put("newUserNeedReview", false);
            put("loginLockCount", 5);
            put("tokenExpireHours", 72);
        }});
        m.put("shop", new LinkedHashMap<String, Object>() {{
            put("openApply", true);
            put("needReview", true);
            put("maxShopsPerUser", 3);
            put("defaultProductAudit", "Pending");
        }});
        m.put("content", new LinkedHashMap<String, Object>() {{
            put("postNeedReview", true);
            put("reviewNeedReview", false);
            put("dailyPostLimit", 20);
        }});
        m.put("notify", new LinkedHashMap<String, Object>() {{
            put("orderStatusNotify", true);
            put("couponExpireNotify", true);
            put("pointsChangeNotify", true);
            put("remindDaysBefore", 3);
        }});
        return m;
    }

    private Map<String, Object> roleToMap(AdminRoleEntity role) {
        Map<String, Object> extra = parseRemark(role.getRemark());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", role.getId());
        m.put("roleCode", role.getRoleCode());
        m.put("name", role.getRoleName());
        m.put("description", extra.getOrDefault("description", ""));
        m.put("icon", extra.getOrDefault("icon", "ri-shield-user-line"));
        m.put("color", extra.getOrDefault("color", "#FFF4E6"));
        m.put("status", role.getStatus());
        m.put("sort", role.getSort());
        return m;
    }

    private Map<String, Object> parseRemark(String remark) {
        if (!StringUtils.hasText(remark)) return new HashMap<>();
        try {
            return new HashMap<>(objectMapper.readValue(remark, new TypeReference<Map<String, Object>>() {}));
        } catch (Exception e) {
            Map<String, Object> m = new HashMap<>();
            m.put("description", remark);
            return m;
        }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "{}"; }
    }

    private String extraJson(Map<String, Object> dto) {
        Map<String, Object> extra = new HashMap<>();
        if (dto.containsKey("description")) extra.put("description", dto.get("description"));
        if (dto.containsKey("icon"))        extra.put("icon",        dto.get("icon"));
        if (dto.containsKey("color"))       extra.put("color",       dto.get("color"));
        return toJson(extra);
    }

    private String strVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    private int intVal(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v == null) return def;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }

    private String inferModule(String url) {
        if (url == null) return "other";
        if (url.contains("/product"))  return "product";
        if (url.contains("/order"))    return "order";
        if (url.contains("/shop"))     return "shop";
        if (url.contains("/user"))     return "user";
        if (url.contains("/marketing")) return "marketing";
        if (url.contains("/admin"))    return "system";
        if (url.contains("/auth"))     return "auth";
        return "other";
    }

    private String inferAction(String method) {
        if (method == null) return "query";
        switch (method.toUpperCase()) {
            case "POST":   return "create";
            case "PUT":    return "update";
            case "DELETE": return "delete";
            default:       return "query";
        }
    }

    private String buildDescription(OperationLog log) {
        if (StringUtils.hasText(log.getOperation())) return log.getOperation();
        String method = log.getMethod() != null ? log.getMethod().toUpperCase() : "";
        String url = log.getRequest_url() != null ? log.getRequest_url() : "";
        return method + " " + url;
    }

    private void recordLoginLog(Long adminUserId, String username, String ip, int status, String reason) {
        AdminLoginLogEntity log = new AdminLoginLogEntity();
        log.setId(idWorker.nextId());
        log.setAdminUserId(adminUserId != null ? adminUserId : 0L);
        log.setUsername(username);
        log.setLoginIp(ip);
        log.setStatus(status);
        log.setFailureReason(reason);
        log.setLoginAt(LocalDateTime.now());
        adminLoginLogMapper.insert(log);
    }

    private List<Map<String, Object>> buildMenuTree(List<AdminMenuEntity> allMenus, Long parentId) {
        List<Map<String, Object>> tree = new ArrayList<>();
        for (AdminMenuEntity menu : allMenus) {
            if (menu.getParentId().equals(parentId)) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", menu.getId());
                node.put("menuName", menu.getMenuName());
                node.put("menuCode", menu.getMenuCode());
                node.put("menuType", menu.getMenuType());
                node.put("routePath", menu.getRoutePath());
                node.put("icon", menu.getIcon());
                node.put("sort", menu.getSort());
                List<Map<String, Object>> children = buildMenuTree(allMenus, menu.getId());
                if (!children.isEmpty()) node.put("children", children);
                tree.add(node);
            }
        }
        return tree;
    }

    private AdminUserVo toVo(AdminUserEntity entity) {
        AdminUserVo vo = new AdminUserVo();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setRealName(entity.getRealName());
        vo.setNickname(entity.getNickname());
        vo.setMobile(entity.getMobile());
        vo.setEmail(entity.getEmail());
        vo.setStatus(entity.getStatus());
        vo.setIsSuperAdmin(entity.getIsSuperAdmin());
        vo.setLastLoginAt(entity.getLastLoginAt());
        return vo;
    }

    private long safePage(Integer p) { return p == null || p < 1 ? 1 : p; }
    private long safeSize(Integer s) { return s == null || s < 1 ? 20 : Math.min(s, 100); }
}
