package com.yxshop.Module.Shop.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxshop.Module.Shop.Entity.Process;
import com.yxshop.Module.User.Entity.User;
import com.yxshop.Module.Shop.Mapper.ProcessMapper;
import com.yxshop.Module.User.Mapper.UserMapper;
import com.yxshop.Module.Catalog.Entity.AppProductEntity;
import com.yxshop.Module.Catalog.Mapper.AppProductMapper;
import com.yxshop.Module.Marketing.Entity.ActivityEntity;
import com.yxshop.Module.Marketing.Entity.CouponTemplateEntity;
import com.yxshop.Module.Marketing.Mapper.ActivityMapper;
import com.yxshop.Module.Marketing.Mapper.CouponTemplateMapper;
import com.yxshop.Module.Shop.Dto.ShopDecorationDto;
import com.yxshop.Module.Shop.Dto.ShopDto;
import com.yxshop.Module.Shop.Dto.ShopQueryDto;
import com.yxshop.Module.Shop.Dto.ShopReviewDto;
import com.yxshop.Module.Shop.Entity.ShopDecorationEntity;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopDecorationMapper;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Module.Shop.Service.ShopModuleService;
import com.yxshop.Module.Shop.Vo.ShopVo;
import com.yxshop.Utils.AliOSSUtils;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Primary
@Service
public class ShopModuleServiceImpl extends ServiceImpl<ShopModuleMapper, ShopEntity> implements ShopModuleService {

    private final ProcessMapper processMapper;
    private final UserMapper userMapper;
    private final AppProductMapper appProductMapper;
    private final ActivityMapper activityMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final ShopDecorationMapper shopDecorationMapper;
    private final AliOSSUtils aliOSSUtils;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(2, 1);

    public ShopModuleServiceImpl(ProcessMapper processMapper,
                                 UserMapper userMapper,
                                 AppProductMapper appProductMapper,
                                 ActivityMapper activityMapper,
                                 CouponTemplateMapper couponTemplateMapper,
                                 ShopDecorationMapper shopDecorationMapper,
                                 AliOSSUtils aliOSSUtils) {
        this.processMapper = processMapper;
        this.userMapper = userMapper;
        this.appProductMapper = appProductMapper;
        this.activityMapper = activityMapper;
        this.couponTemplateMapper = couponTemplateMapper;
        this.shopDecorationMapper = shopDecorationMapper;
        this.aliOSSUtils = aliOSSUtils;
    }

    @Override
    public ShopVo getShopDetail(Long shopId) {
        ShopEntity shop = getById(shopId);
        if (shop == null || "Invalid".equalsIgnoreCase(shop.getStatus())) {
            throw new IllegalArgumentException("店铺不存在");
        }
        return toVo(shop);
    }

    @Override
    public ShopVo getShopHome(Long shopId) {
        ShopVo vo = getShopDetail(shopId);
        vo.setDecoration(decorationMap(findDecoration(shopId)));
        QueryWrapper<AppProductEntity> productWrapper = new QueryWrapper<>();
        productWrapper.eq("shop_id", shopId).eq("status", 1).eq("audit_status", "Approved").orderByDesc("sales").last("LIMIT 20");
        vo.setProducts(appProductMapper.selectList(productWrapper));
        QueryWrapper<ActivityEntity> activityWrapper = new QueryWrapper<>();
        activityWrapper.eq("shop_id", shopId).eq("status", 1).orderByDesc("updated_at").last("LIMIT 10");
        vo.setActivities(activityMapper.selectList(activityWrapper));
        QueryWrapper<CouponTemplateEntity> couponWrapper = new QueryWrapper<>();
        couponWrapper.eq("shop_id", shopId).eq("status", 1).orderByDesc("updated_at").last("LIMIT 10");
        vo.setCoupons(couponTemplateMapper.selectList(couponWrapper));
        return vo;
    }

    @Override
    public List<ShopVo> listPublicShops(ShopQueryDto queryDto) {
        QueryWrapper<ShopEntity> wrapper = buildQuery(queryDto);
        wrapper.eq("status", "Active");
        wrapper.orderByDesc("is_brand_shop").orderByDesc("sales").orderByDesc("rating");
        return list(wrapper).stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    public List<ShopVo> listMyShops(Long userId) {
        QueryWrapper<ShopEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("owner_user_id", userId).ne("status", "Invalid").orderByDesc("createTime");
        List<ShopEntity> shops = list(wrapper);
        return shops.stream().map(shop -> {
            ShopVo vo = toVo(shop);
            Process pending = findPendingOpenShopProcess(shop.getShopId());
            vo.setPendingProcessId(pending == null ? null : pending.getProcess_id());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopVo applyShop(Long userId, ShopDto shopDto) {
        if (hasValidShop(userId)) {
            throw new IllegalArgumentException("当前用户已有店铺或待审核申请");
        }
        requireNotBlank(shopDto.getShopName(), "店铺名称不能为空");
        ShopEntity shop = new ShopEntity();
        shop.setShopId(idWorker.nextId());
        shop.setShopName(shopDto.getShopName());
        shop.setDisplayName(firstNotBlank(shopDto.getDisplayName(), shopDto.getShopName()));
        shop.setOwnerUserId(userId);
        shop.setPhone(shopDto.getPhone());
        shop.setLocation(shopDto.getLocation());
        shop.setRegistrationDate(LocalDate.now());
        shop.setShopDescription(shopDto.getShopDescription());
        shop.setShopImage(shopDto.getShopImage());
        shop.setAvatar(shopDto.getAvatar());
        shop.setLogo(shopDto.getLogo());
        shop.setBanner(shopDto.getBanner());
        shop.setStatus("Pending");
        shop.setIsBrandShop(0);
        shop.setProductCount(0);
        shop.setFollowers(0);
        shop.setSales(0);
        shop.setCreateTime(LocalDateTime.now());
        shop.setUpdateTime(LocalDateTime.now());
        save(shop);
        Process process = createOpenShopProcess(userId, shop);
        processMapper.insert(process);
        ShopVo vo = toVo(shop);
        vo.setPendingProcessId(process.getProcess_id());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopVo updateMyShop(Long userId, Long shopId, ShopDto shopDto) {
        ShopEntity shop = getById(shopId);
        if (shop == null) {
            throw new IllegalArgumentException("店铺不存在");
        }
        if (!userId.equals(shop.getOwnerUserId())) {
            throw new IllegalArgumentException("无权操作其他用户店铺");
        }
        applyShopFields(shop, shopDto);
        shop.setUpdateTime(LocalDateTime.now());
        updateById(shop);
        return toVo(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShopVo saveDecoration(Long userId, String operatorRole, Long shopId, ShopDecorationDto decorationDto) {
        ShopEntity shop = getById(shopId);
        if (shop == null) {
            throw new IllegalArgumentException("店铺不存在");
        }
        if (!"Admin".equals(operatorRole) && !userId.equals(shop.getOwnerUserId())) {
            throw new IllegalArgumentException("无权装修其他店铺");
        }
        ShopDecorationEntity decoration = findDecoration(shopId);
        if (decoration == null) {
            decoration = new ShopDecorationEntity();
            decoration.setId(idWorker.nextId());
            decoration.setShopId(shopId);
            decoration.setCreatedAt(LocalDateTime.now());
        }
        decoration.setTheme(decorationDto.getTheme());
        decoration.setModules(decorationDto.getModules());
        decoration.setColors(decorationDto.getColors());
        decoration.setStatus(decorationDto.getStatus() == null ? 1 : decorationDto.getStatus());
        decoration.setUpdatedAt(LocalDateTime.now());
        if (shopDecorationMapper.selectById(decoration.getId()) == null) {
            shopDecorationMapper.insert(decoration);
        } else {
            shopDecorationMapper.updateById(decoration);
        }
        return getShopHome(shopId);
    }

    @Override
    public void disableShop(Long operatorId, String operatorRole, Long shopId) {
        ShopEntity shop = getById(shopId);
        if (shop == null) {
            throw new IllegalArgumentException("店铺不存在");
        }
        if (!"Admin".equals(operatorRole) && !operatorId.equals(shop.getOwnerUserId())) {
            throw new IllegalArgumentException("无权操作其他用户店铺");
        }
        ShopEntity update = new ShopEntity();
        update.setShopId(shopId);
        update.setStatus("Invalid");
        update.setUpdateTime(LocalDateTime.now());
        updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewShop(Long reviewerId, ShopReviewDto reviewDto) {
        requireNotBlank(reviewDto.getStatus(), "审核状态不能为在");
        Process process = findReviewProcess(reviewDto);
        if (process == null) {
            throw new IllegalArgumentException("审核记录不存在");
        }
        if (!"Pending".equals(process.getStatus())) {
            throw new IllegalArgumentException("该申请已审核");
        }
        String reviewStatus = normalizeReviewStatus(reviewDto.getStatus());
        process.setReviewer_id(reviewerId);
        process.setReview_date(LocalDateTime.now());
        process.setRemark(reviewDto.getRemark());
        process.setStatus(reviewStatus);
        process.setUpdate_time(LocalDateTime.now());
        processMapper.updateById(process);

        ShopEntity shop = new ShopEntity();
        shop.setShopId(process.getEffect_id());
        shop.setStatus("Approved".equals(reviewStatus) ? "Active" : "Rejected");
        shop.setUpdateTime(LocalDateTime.now());
        updateById(shop);

        if ("Approved".equals(reviewStatus)) {
            User user = new User();
            user.setId(process.getApplication_id());
            user.setRole("ShopOwner");
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }

    @Override
    public Map<String, Object> listAdminShops(ShopQueryDto queryDto) {
        ShopQueryDto query = queryDto == null ? new ShopQueryDto() : queryDto;
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();
        Page<ShopEntity> page = page(new Page<>(pageNum, pageSize), buildQuery(query).orderByDesc("createTime"));
        List<ShopVo> vos = page.getRecords().stream().map(shop -> {
            ShopVo vo = toVo(shop);
            // Real product count: count from product table (status=1 & audit_status=Approved)
            QueryWrapper<AppProductEntity> productQuery = new QueryWrapper<>();
            productQuery.eq("shop_id", shop.getShopId()).eq("status", 1).eq("audit_status", "Approved");
            vo.setProductCount((int) appProductMapper.selectCount(productQuery));
            // Owner info
            if (shop.getOwnerUserId() != null) {
                User owner = userMapper.selectById(shop.getOwnerUserId());
                if (owner != null) {
                    String ownerName = owner.getNick_name() != null && !owner.getNick_name().isEmpty()
                            ? owner.getNick_name() : owner.getUsername();
                    vo.setOwnerName(ownerName);
                }
            }
            return vo;
        }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("records", vos);
        result.put("total", page.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    private ShopDecorationEntity findDecoration(Long shopId) {
        QueryWrapper<ShopDecorationEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("shop_id", shopId).eq("status", 1).last("LIMIT 1");
        return shopDecorationMapper.selectOne(wrapper);
    }

    private Map<String, Object> decorationMap(ShopDecorationEntity decoration) {
        Map<String, Object> map = new HashMap<>();
        if (decoration == null) {
            return map;
        }
        map.put("theme", decoration.getTheme());
        map.put("modules", decoration.getModules());
        map.put("colors", decoration.getColors());
        return map;
    }

    private QueryWrapper<ShopEntity> buildQuery(ShopQueryDto queryDto) {
        ShopQueryDto query = queryDto == null ? new ShopQueryDto() : queryDto;
        QueryWrapper<ShopEntity> wrapper = new QueryWrapper<>();
        if (!isBlank(query.getKeyword())) {
            wrapper.and(item -> item.like("shop_name", query.getKeyword()).or().like("display_name", query.getKeyword()));
        }
        if (!isBlank(query.getStatus())) {
            wrapper.eq("status", query.getStatus());
        }
        if (query.getBrandShop() != null) {
            wrapper.eq("is_brand_shop", query.getBrandShop() ? 1 : 0);
        }
        if (query.getOwnerUserId() != null) {
            wrapper.eq("owner_user_id", query.getOwnerUserId());
        }
        return wrapper;
    }

    private boolean hasValidShop(Long userId) {
        QueryWrapper<ShopEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("owner_user_id", userId).ne("status", "Invalid").last("LIMIT 1");
        return getOne(wrapper) != null;
    }

    private Process createOpenShopProcess(Long userId, ShopEntity shop) {
        Process process = new Process();
        process.setProcess_id(idWorker.nextId());
        process.setProcess_type("申请开在");
        process.setApplication_id(userId);
        process.setEffect_id(shop.getShopId());
        process.setEffect_schema("shop");
        process.setApplication_date(LocalDateTime.now());
        process.setCreate_time(LocalDateTime.now());
        process.setUpdate_time(LocalDateTime.now());
        process.setStatus("Pending");
        process.setDescription("用户申请开通店铺：" + shop.getShopName());
        return process;
    }

    private Process findPendingOpenShopProcess(Long shopId) {
        QueryWrapper<Process> wrapper = new QueryWrapper<>();
        wrapper.eq("effect_schema", "shop")
                .eq("effect_id", shopId)
                .eq("process_type", "申请开在")
                .eq("status", "Pending")
                .last("LIMIT 1");
        return processMapper.selectOne(wrapper);
    }

    private Process findReviewProcess(ShopReviewDto reviewDto) {
        if (reviewDto.getProcessId() != null) {
            return processMapper.selectById(reviewDto.getProcessId());
        }
        if (reviewDto.getShopId() == null) {
            throw new IllegalArgumentException("审核记录或店铺ID不能为空");
        }
        return findPendingOpenShopProcess(reviewDto.getShopId());
    }

    private String normalizeReviewStatus(String status) {
        if ("Approved".equalsIgnoreCase(status) || "Active".equalsIgnoreCase(status) || "通过".equals(status)) {
            return "Approved";
        }
        if ("Rejected".equalsIgnoreCase(status) || "拒绝".equals(status)) {
            return "Rejected";
        }
        throw new IllegalArgumentException("审核状态仅支持 Approved/Rejected");
    }

    private void applyShopFields(ShopEntity shop, ShopDto dto) {
        if (dto.getShopName() != null) {
            shop.setShopName(dto.getShopName());
        }
        if (dto.getDisplayName() != null) {
            shop.setDisplayName(dto.getDisplayName());
        }
        if (dto.getPhone() != null) {
            shop.setPhone(dto.getPhone());
        }
        if (dto.getLocation() != null) {
            shop.setLocation(dto.getLocation());
        }
        if (dto.getShopDescription() != null) {
            shop.setShopDescription(dto.getShopDescription());
        }
        if (dto.getShopImage() != null) {
            shop.setShopImage(dto.getShopImage());
        }
        if (dto.getAvatar() != null) {
            shop.setAvatar(dto.getAvatar());
        }
        if (dto.getLogo() != null) {
            shop.setLogo(dto.getLogo());
        }
        if (dto.getBanner() != null) {
            shop.setBanner(dto.getBanner());
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public ShopVo adminUpdateShop(Long shopId, ShopDto shopDto) {
        ShopEntity shop = getById(shopId);
        if (shop == null) throw new IllegalArgumentException("店铺不存在");
        applyShopFields(shop, shopDto);
        shop.setUpdateTime(LocalDateTime.now());
        updateById(shop);
        return toVo(shop);
    }

    /** 将 objectKey 解析为 2h 预签名 URL；若已是完整 URL 则直接返回 */
    private String resolveImgUrl(String value) {
        if (value == null || value.isBlank()) return value;
        if (AliOSSUtils.isObjectKey(value)) {
            String signed = aliOSSUtils.generatePresignedUrl(value, 120);
            return signed != null ? signed : value;
        }
        return value;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public ShopVo updateShopStatus(Long shopId, String status) {
        ShopEntity shop = getById(shopId);
        if (shop == null) throw new IllegalArgumentException("店铺不存在");
        shop.setStatus(status);
        shop.setUpdateTime(LocalDateTime.now());
        updateById(shop);
        return toVo(shop);
    }

    private ShopVo toVo(ShopEntity shop) {
        ShopVo vo = new ShopVo();
        vo.setShopId(shop.getShopId());
        vo.setShopName(shop.getShopName());
        vo.setDisplayName(firstNotBlank(shop.getDisplayName(), shop.getShopName()));
        vo.setOwnerUserId(shop.getOwnerUserId());
        vo.setPhone(shop.getPhone());
        vo.setLocation(shop.getLocation());
        vo.setShopDescription(shop.getShopDescription());
        vo.setShopImage(resolveImgUrl(shop.getShopImage()));
        vo.setAvatar(resolveImgUrl(shop.getAvatar()));
        vo.setLogo(resolveImgUrl(shop.getLogo()));
        vo.setBanner(resolveImgUrl(shop.getBanner()));
        vo.setTags(shop.getTags());
        vo.setStatus(shop.getStatus());
        vo.setProductCount(defaultInt(shop.getProductCount()));
        vo.setFollowers(defaultInt(shop.getFollowers()));
        vo.setSales(defaultInt(shop.getSales()));
        vo.setRating(formatDecimal(shop.getRating()));
        vo.setServiceScore(formatDecimal(shop.getServiceScore()));
        vo.setLogisticsScore(formatDecimal(shop.getLogisticsScore()));
        vo.setQualityScore(formatDecimal(shop.getQualityScore()));
        vo.setDiscountLabel(shop.getDiscountLabel());
        vo.setBrandShop(shop.getIsBrandShop() != null && shop.getIsBrandShop() == 1);
        // 补充店主名称（用 nick_name 或 username 兜底）
        if (shop.getOwnerUserId() != null) {
            com.yxshop.Module.User.Entity.User owner = userMapper.selectById(shop.getOwnerUserId());
            if (owner != null) {
                String ownerName = owner.getNick_name() != null && !owner.getNick_name().isEmpty()
                        ? owner.getNick_name() : owner.getUsername();
                vo.setOwnerName(ownerName);
            }
        }
        return vo;
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String formatDecimal(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void requireNotBlank(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private String firstNotBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
