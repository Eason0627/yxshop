package com.yxshop.Module.Points.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yxshop.Module.Points.Entity.CheckinCampaignEntity;
import com.yxshop.Module.Points.Entity.CheckinRecordEntity;
import com.yxshop.Module.Points.Entity.PointsAccountEntity;
import com.yxshop.Module.Points.Entity.PointsGoodsEntity;
import com.yxshop.Module.Points.Entity.PointsRecordEntity;
import com.yxshop.Module.Points.Mapper.CheckinCampaignMapper;
import com.yxshop.Module.Points.Mapper.CheckinRecordMapper;
import com.yxshop.Module.Points.Mapper.PointsAccountMapper;
import com.yxshop.Module.Points.Mapper.PointsGoodsMapper;
import com.yxshop.Module.Points.Mapper.PointsRecordMapper;
import com.yxshop.Module.User.Entity.User;
import com.yxshop.Module.User.Mapper.UserMapper;
import com.yxshop.Utils.Result;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin-only endpoints for the Points Center.
 * All mutating operations require Admin or ShopOwner role.
 */
@RestController
@RequestMapping("/app/points")
public class PointsAdminController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PointsGoodsMapper goodsMapper;
    private final PointsRecordMapper recordMapper;
    private final PointsAccountMapper accountMapper;
    private final CheckinRecordMapper checkinRecordMapper;
    private final CheckinCampaignMapper campaignMapper;
    private final UserMapper userMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(25, 2);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PointsAdminController(PointsGoodsMapper goodsMapper,
                                  PointsRecordMapper recordMapper,
                                  PointsAccountMapper accountMapper,
                                  CheckinRecordMapper checkinRecordMapper,
                                  CheckinCampaignMapper campaignMapper,
                                  UserMapper userMapper) {
        this.goodsMapper = goodsMapper;
        this.recordMapper = recordMapper;
        this.accountMapper = accountMapper;
        this.checkinRecordMapper = checkinRecordMapper;
        this.campaignMapper = campaignMapper;
        this.userMapper = userMapper;
    }

    // ══════════════════════════════════════════════════════════════════
    //  积分商品管理
    // ══════════════════════════════════════════════════════════════════

    /** Admin list — all goods with optional keyword/status filter */
    @GetMapping("/goods/admin")
    public Result adminListGoods(@RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "20") Integer pageSize,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Integer status,
                                  HttpServletRequest request) {
        requireAdmin(request);
        LambdaQueryWrapper<PointsGoodsEntity> wrapper = new LambdaQueryWrapper<PointsGoodsEntity>()
                .orderByDesc(PointsGoodsEntity::getCreatedAt);
        if (status != null) wrapper.eq(PointsGoodsEntity::getStatus, status);
        if (keyword != null && !keyword.trim().isEmpty())
            wrapper.like(PointsGoodsEntity::getName, keyword.trim());

        Page<PointsGoodsEntity> page = goodsMapper.selectPage(new Page<>(safePage(pageNum), safeSize(pageSize)), wrapper);
        List<Map<String, Object>> records = page.getRecords().stream()
                .map(this::toGoodsMap).collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", page.getTotal());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        return Result.success(result);
    }

    /** Create new points goods */
    @PostMapping("/goods")
    @Transactional(rollbackFor = Exception.class)
    public Result createGoods(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        PointsGoodsEntity goods = new PointsGoodsEntity();
        goods.setId(idWorker.nextId());
        goods.setName(str(body, "name"));
        goods.setImage(str(body, "image"));
        goods.setDescription(str(body, "description"));
        goods.setPointsPrice(integer(body, "pointsCost", integer(body, "pointsPrice", 100)));
        goods.setStock(integer(body, "stock", 0));
        goods.setStatus(integer(body, "status", 1));
        goods.setCreatedAt(LocalDateTime.now());
        goods.setUpdatedAt(LocalDateTime.now());
        goodsMapper.insert(goods);
        return Result.success(toGoodsMap(goods));
    }

    /** Update points goods */
    @PutMapping("/goods/{goodsId}")
    @Transactional(rollbackFor = Exception.class)
    public Result updateGoods(@PathVariable Long goodsId,
                               @RequestBody Map<String, Object> body,
                               HttpServletRequest request) {
        requireAdmin(request);
        PointsGoodsEntity goods = goodsMapper.selectById(goodsId);
        if (goods == null) throw new RuntimeException("积分商品不存在");
        if (body.containsKey("name"))        goods.setName(str(body, "name"));
        if (body.containsKey("image"))       goods.setImage(str(body, "image"));
        if (body.containsKey("description")) goods.setDescription(str(body, "description"));
        if (body.containsKey("pointsCost"))  goods.setPointsPrice(integer(body, "pointsCost", goods.getPointsPrice()));
        if (body.containsKey("pointsPrice")) goods.setPointsPrice(integer(body, "pointsPrice", goods.getPointsPrice()));
        if (body.containsKey("stock"))       goods.setStock(integer(body, "stock", goods.getStock()));
        if (body.containsKey("status"))      goods.setStatus(integer(body, "status", goods.getStatus()));
        goods.setUpdatedAt(LocalDateTime.now());
        goodsMapper.updateById(goods);
        return Result.success(toGoodsMap(goods));
    }

    /** Toggle goods on/off-shelf */
    @PutMapping("/goods/{goodsId}/status")
    public Result updateGoodsStatus(@PathVariable Long goodsId,
                                     @RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
        requireAdmin(request);
        PointsGoodsEntity goods = goodsMapper.selectById(goodsId);
        if (goods == null) throw new RuntimeException("积分商品不存在");
        goods.setStatus(integer(body, "status", goods.getStatus()));
        goods.setUpdatedAt(LocalDateTime.now());
        goodsMapper.updateById(goods);
        return Result.success(toGoodsMap(goods));
    }

    // ══════════════════════════════════════════════════════════════════
    //  签到活动配置
    // ══════════════════════════════════════════════════════════════════

    /** Save checkin config (weekly rules + milestones) */
    @PutMapping("/checkin/config")
    @Transactional(rollbackFor = Exception.class)
    public Result saveCheckinConfig(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        CheckinCampaignEntity campaign = loadOrCreateActiveCampaign();
        // Update milestones
        Object milestonesObj = body.get("milestones");
        if (milestonesObj != null) {
            campaign.setMilestones(toJson(milestonesObj));
        }
        // Build weekly rules from dailyPoints if provided
        Object weeklyRulesObj = body.get("weeklyRules");
        if (weeklyRulesObj != null) {
            campaign.setWeeklyRules(toJson(weeklyRulesObj));
        } else if (body.containsKey("dailyPoints")) {
            int base = integer(body, "dailyPoints", 5);
            List<Map<String, Object>> rules = new ArrayList<>();
            int[] pts = { base, base, base + 1, base + 1, base + 2, base + 3, base + 3 };
            for (int i = 1; i <= 7; i++) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("dayOfWeek", i);
                r.put("points", pts[i - 1]);
                rules.add(r);
            }
            campaign.setWeeklyRules(toJson(rules));
        }
        campaign.setUpdatedAt(LocalDateTime.now());
        if (campaign.getId() == null) {
            campaign.setId(idWorker.nextId());
            campaign.setCreatedAt(LocalDateTime.now());
            campaignMapper.insert(campaign);
        } else {
            campaignMapper.updateById(campaign);
        }
        return Result.success("配置已保存");
    }

    /** Save milestones only */
    @PutMapping("/checkin/milestones")
    @Transactional(rollbackFor = Exception.class)
    public Result saveCheckinMilestones(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        CheckinCampaignEntity campaign = loadOrCreateActiveCampaign();
        Object milestonesObj = body.get("milestones");
        if (milestonesObj != null) campaign.setMilestones(toJson(milestonesObj));
        campaign.setUpdatedAt(LocalDateTime.now());
        if (campaign.getId() == null) {
            campaign.setId(idWorker.nextId());
            campaign.setCreatedAt(LocalDateTime.now());
            campaignMapper.insert(campaign);
        } else {
            campaignMapper.updateById(campaign);
        }
        return Result.success("里程碑已保存");
    }

    /** Checkin statistics for a given date */
    @GetMapping("/checkin/stats")
    public Result checkinStats(@RequestParam(required = false) String date, HttpServletRequest request) {
        requireAdmin(request);
        LocalDate targetDate = date == null ? LocalDate.now() : LocalDate.parse(date);
        List<CheckinRecordEntity> todayRecords = checkinRecordMapper.selectList(
                new LambdaQueryWrapper<CheckinRecordEntity>()
                        .eq(CheckinRecordEntity::getCheckinDate, targetDate));

        long todayCount  = todayRecords.size();
        int  todayPoints = todayRecords.stream().mapToInt(r -> r.getPoints() == null ? 0 : r.getPoints()).sum();
        long totalCount  = checkinRecordMapper.selectCount(null);
        long consecutiveCount = todayRecords.stream()
                .filter(r -> r.getContinuousDays() != null && r.getContinuousDays() >= 7).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayCount", todayCount);
        result.put("todayPoints", todayPoints);
        result.put("totalCount", totalCount);
        result.put("consecutiveCount", consecutiveCount);
        return Result.success(result);
    }

    /** Admin checkin records */
    @GetMapping("/checkin/records")
    public Result checkinRecords(@RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "20") Integer pageSize,
                                  @RequestParam(required = false) String date,
                                  HttpServletRequest request) {
        requireAdmin(request);
        LambdaQueryWrapper<CheckinRecordEntity> wrapper = new LambdaQueryWrapper<CheckinRecordEntity>()
                .orderByDesc(CheckinRecordEntity::getCheckinDate);
        if (date != null && !date.trim().isEmpty()) {
            wrapper.eq(CheckinRecordEntity::getCheckinDate, LocalDate.parse(date));
        }
        Page<CheckinRecordEntity> page = checkinRecordMapper.selectPage(new Page<>(safePage(pageNum), safeSize(pageSize)), wrapper);

        List<Map<String, Object>> records = page.getRecords().stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("userId", r.getUserId());
            User user = r.getUserId() == null ? null : userMapper.getUserById(r.getUserId());
            map.put("username", user == null ? String.valueOf(r.getUserId()) : notBlank(user.getNick_name(), user.getUsername()));
            map.put("userAvatar", user == null ? null : user.getAvatar());
            map.put("checkinDate", r.getCheckinDate());
            map.put("continuousDays", r.getContinuousDays());
            map.put("points", r.getPoints());
            map.put("checkinTime", r.getCreatedAt() != null ? r.getCreatedAt().format(DT_FMT) : "");
            map.put("createTime", r.getCreatedAt() != null ? r.getCreatedAt().format(DT_FMT) : "");
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", page.getTotal());
        return Result.success(result);
    }

    // ══════════════════════════════════════════════════════════════════
    //  积分流水管理
    // ══════════════════════════════════════════════════════════════════

    /** Admin view all points records across all users */
    @GetMapping("/records/admin")
    public Result adminRecords(@RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "20") Integer pageSize,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String type,
                                @RequestParam(required = false) String startDate,
                                @RequestParam(required = false) String endDate,
                                HttpServletRequest request) {
        requireAdmin(request);
        LambdaQueryWrapper<PointsRecordEntity> wrapper = new LambdaQueryWrapper<PointsRecordEntity>()
                .orderByDesc(PointsRecordEntity::getCreatedAt);
        if (type != null && !type.trim().isEmpty()) {
            // Map admin UI type values to backend biz_type (case-insensitive via LOWER())
            if ("checkin".equals(type)) {
                wrapper.apply("LOWER(biz_type) = 'checkin'");
            } else if ("redeem".equals(type)) {
                wrapper.apply("LOWER(biz_type) IN ('pointsorder','redeem','exchange')");
            } else if ("purchase".equals(type)) {
                wrapper.apply("LOWER(biz_type) IN ('order','purchase','payment')");
            } else if ("admin".equals(type)) {
                wrapper.apply("LOWER(biz_type) IN ('adminadjust','admin')");
            }
        }
        if (startDate != null && !startDate.trim().isEmpty())
            wrapper.ge(PointsRecordEntity::getCreatedAt, LocalDate.parse(startDate).atStartOfDay());
        if (endDate != null && !endDate.trim().isEmpty())
            wrapper.le(PointsRecordEntity::getCreatedAt, LocalDate.parse(endDate).atTime(23, 59, 59));

        Page<PointsRecordEntity> page = recordMapper.selectPage(new Page<>(safePage(pageNum), safeSize(pageSize)), wrapper);

        List<Map<String, Object>> records = page.getRecords().stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("userId", r.getUserId());
            User user = r.getUserId() == null ? null : userMapper.getUserById(r.getUserId());
            map.put("username", user == null ? String.valueOf(r.getUserId()) : notBlank(user.getNick_name(), user.getUsername()));
            map.put("userAvatar", user == null ? null : user.getAvatar());
            map.put("type", mapBizType(r.getBizType()));
            map.put("points", r.getChangeType() != null && r.getChangeType().equalsIgnoreCase("Earn") ? r.getPoints() : -r.getPoints());
            map.put("balance", r.getAfterPoints());
            map.put("remark", r.getDescription());
            map.put("description", r.getDescription());
            map.put("createTime", r.getCreatedAt() != null ? r.getCreatedAt().format(DT_FMT) : "");
            return map;
        }).collect(Collectors.toList());

        // Filter by keyword (userId/username) — do in-memory since JOIN is not available
        if (keyword != null && !keyword.trim().isEmpty()) {
            final String kw = keyword.trim().toLowerCase();
            records = records.stream()
                    .filter(r -> String.valueOf(r.get("userId")).contains(kw)
                            || String.valueOf(r.get("username")).toLowerCase().contains(kw))
                    .collect(Collectors.toList());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", page.getTotal());
        return Result.success(result);
    }

    /** Overall points statistics */
    @GetMapping("/stats")
    public Result pointsStats(HttpServletRequest request) {
        requireAdmin(request);
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd   = today.atTime(23, 59, 59);

        List<PointsRecordEntity> todayRecords = recordMapper.selectList(
                new LambdaQueryWrapper<PointsRecordEntity>()
                        .ge(PointsRecordEntity::getCreatedAt, todayStart)
                        .le(PointsRecordEntity::getCreatedAt, todayEnd));

        long todayIssued   = todayRecords.stream().filter(r -> "Earn".equalsIgnoreCase(r.getChangeType())).mapToLong(r -> r.getPoints() == null ? 0 : r.getPoints()).sum();
        long todayConsumed = todayRecords.stream().filter(r -> "Spend".equalsIgnoreCase(r.getChangeType())).mapToLong(r -> r.getPoints() == null ? 0 : r.getPoints()).sum();

        List<PointsAccountEntity> accounts = accountMapper.selectList(null);
        long totalBalance = accounts.stream().mapToLong(a -> a.getCurrentPoints() == null ? 0 : a.getCurrentPoints()).sum();
        long userCount    = accounts.size();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayIssued", todayIssued);
        result.put("todayConsumed", todayConsumed);
        result.put("totalBalance", totalBalance);
        result.put("userCount", userCount);
        return Result.success(result);
    }

    /** Admin manually adjust a user's points */
    @PostMapping("/admin/adjust")
    @Transactional(rollbackFor = Exception.class)
    public Result adminAdjust(@RequestAttribute("currentUserId") Object currentUserId,
                               @RequestBody Map<String, Object> body,
                               HttpServletRequest request) {
        requireAdmin(request);
        Object userIdObj = body.get("userId");
        Object pointsObj = body.get("points");
        String remark    = str(body, "remark");
        if (userIdObj == null) throw new RuntimeException("userId不能为空");
        if (pointsObj == null) throw new RuntimeException("积分变动值不能为空");
        Long targetUserId = Long.parseLong(String.valueOf(userIdObj));
        int  delta        = integer(body, "points", 0);
        if (delta == 0) throw new RuntimeException("积分变动不能为0");

        PointsAccountEntity account = accountMapper.selectById(targetUserId);
        if (account == null) {
            account = new PointsAccountEntity();
            account.setUserId(targetUserId);
            account.setCurrentPoints(0);
            account.setTotalEarned(0);
            account.setTotalSpent(0);
            account.setExpireDate(LocalDate.now().plusYears(1));
            account.setUpdatedAt(LocalDateTime.now());
            accountMapper.insert(account);
        }

        int before = account.getCurrentPoints() == null ? 0 : account.getCurrentPoints();
        int after  = Math.max(0, before + delta);
        account.setCurrentPoints(after);
        if (delta > 0) account.setTotalEarned((account.getTotalEarned() == null ? 0 : account.getTotalEarned()) + delta);
        else account.setTotalSpent((account.getTotalSpent() == null ? 0 : account.getTotalSpent()) + Math.abs(delta));
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(account);

        PointsRecordEntity record = new PointsRecordEntity();
        record.setId(idWorker.nextId());
        record.setUserId(targetUserId);
        record.setChangeType(delta > 0 ? "Earn" : "Spend");
        record.setPoints(Math.abs(delta));
        record.setBeforePoints(before);
        record.setAfterPoints(after);
        record.setBizType("AdminAdjust");
        record.setBizId(Long.parseLong(String.valueOf(currentUserId)));
        record.setDescription(remark != null && !remark.isEmpty() ? remark : "管理员调整积分");
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", targetUserId);
        result.put("beforePoints", before);
        result.put("afterPoints", after);
        result.put("delta", delta);
        return Result.success(result);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void requireAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("currentUserRole");
        String r = role == null ? "" : role.toString();
        if (!"Admin".equals(r) && !"ShopOwner".equals(r)) {
            throw new RuntimeException("仅管理员可执行此操作");
        }
    }

    private Map<String, Object> toGoodsMap(PointsGoodsEntity g) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", g.getId());
        map.put("name", g.getName());
        map.put("image", g.getImage());
        map.put("description", g.getDescription());
        map.put("pointsPrice", g.getPointsPrice());
        map.put("pointsCost", g.getPointsPrice()); // alias for admin UI
        map.put("stock", g.getStock());
        map.put("status", g.getStatus());
        map.put("createdAt", g.getCreatedAt() != null ? g.getCreatedAt().format(DT_FMT) : "");
        map.put("updatedAt", g.getUpdatedAt() != null ? g.getUpdatedAt().format(DT_FMT) : "");
        return map;
    }

    private CheckinCampaignEntity loadOrCreateActiveCampaign() {
        CheckinCampaignEntity c = campaignMapper.selectOne(
                new LambdaQueryWrapper<CheckinCampaignEntity>()
                        .eq(CheckinCampaignEntity::getStatus, 1)
                        .orderByDesc(CheckinCampaignEntity::getStartAt)
                        .last("LIMIT 1"));
        if (c != null) return c;
        c = new CheckinCampaignEntity();
        c.setName("默认签到活动");
        c.setStatus(1);
        c.setStartAt(LocalDateTime.now());
        return c;
    }

    private String mapBizType(String bizType) {
        if (bizType == null) return "other";
        String bt = bizType.toLowerCase();
        if (bt.equals("checkin") || bt.contains("signin")) return "checkin";
        if (bt.equals("pointsorder") || bt.contains("redeem") || bt.contains("exchange")) return "redeem";
        if (bt.equals("order") || bt.equals("purchase") || bt.contains("payment")) return "purchase";
        if (bt.contains("admin") || bt.contains("adjust")) return "admin";
        if (bt.contains("expire")) return "expire";
        return "other";
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "[]"; }
    }

    private String notBlank(String a, String b) {
        return (a != null && !a.trim().isEmpty()) ? a : b;
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private int integer(Map<String, Object> m, String key, int defaultVal) {
        Object v = m.get(key);
        if (v == null) return defaultVal;
        try { return ((Number) v).intValue(); } catch (Exception e) {
            try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e2) { return defaultVal; }
        }
    }

    private long safePage(Integer p) { return p == null || p < 1 ? 1 : p; }
    private long safeSize(Integer s) { return s == null || s < 1 ? 20 : Math.min(s, 100); }
}
