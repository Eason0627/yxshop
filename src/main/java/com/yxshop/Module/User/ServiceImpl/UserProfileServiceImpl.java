package com.yxshop.Module.User.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.User.Entity.User;
import com.yxshop.Module.User.Mapper.UserMapper;
import com.yxshop.Module.Marketing.Entity.UserCouponEntity;
import com.yxshop.Module.Marketing.Mapper.UserCouponMapper;
import com.yxshop.Module.Order.Entity.OrderEntity;
import com.yxshop.Module.Order.Mapper.OrderModuleMapper;
import com.yxshop.Module.Points.Entity.PointsAccountEntity;
import com.yxshop.Module.Points.Mapper.PointsAccountMapper;
import com.yxshop.Module.Catalog.Entity.ProductCommentEntity;
import com.yxshop.Module.Catalog.Mapper.ProductCommentMapper;
import com.yxshop.Module.User.Dto.AddressDto;
import com.yxshop.Module.User.Dto.TargetDto;
import com.yxshop.Module.User.Dto.UserProfileDto;
import com.yxshop.Module.User.Entity.AddressEntity;
import com.yxshop.Module.User.Entity.FavoriteEntity;
import com.yxshop.Module.User.Entity.FootprintEntity;
import com.yxshop.Module.User.Entity.UserProfileEntity;
import com.yxshop.Module.User.Mapper.AddressModuleMapper;
import com.yxshop.Module.User.Mapper.FavoriteMapper;
import com.yxshop.Module.User.Mapper.FootprintMapper;
import com.yxshop.Module.User.Mapper.UserProfileMapper;
import com.yxshop.Module.User.Service.UserProfileService;
import com.yxshop.Module.User.Vo.AddressVo;
import com.yxshop.Module.User.Vo.TargetVo;
import com.yxshop.Module.User.Vo.UserCenterVo;
import com.yxshop.Module.User.Vo.UserProfileVo;
import com.yxshop.Utils.AliOSSUtils;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Primary
@Service
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfileEntity> implements UserProfileService {

    private final UserMapper userMapper;
    private final AddressModuleMapper addressMapper;
    private final FavoriteMapper favoriteMapper;
    private final FootprintMapper footprintMapper;
    private final PointsAccountMapper pointsAccountMapper;
    private final OrderModuleMapper orderMapper;
    private final UserCouponMapper userCouponMapper;
    private final ProductCommentMapper commentMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(5, 1);
    @Autowired(required = false) private AliOSSUtils aliOSSUtils;

    public UserProfileServiceImpl(UserMapper userMapper,
                                  AddressModuleMapper addressMapper,
                                  FavoriteMapper favoriteMapper,
                                  FootprintMapper footprintMapper,
                                  PointsAccountMapper pointsAccountMapper,
                                  OrderModuleMapper orderMapper,
                                  UserCouponMapper userCouponMapper,
                                  ProductCommentMapper commentMapper) {
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
        this.favoriteMapper = favoriteMapper;
        this.footprintMapper = footprintMapper;
        this.pointsAccountMapper = pointsAccountMapper;
        this.orderMapper = orderMapper;
        this.userCouponMapper = userCouponMapper;
        this.commentMapper = commentMapper;
    }

    @Override
    public UserProfileVo getCurrentProfile(Long userId) {
        UserProfileEntity profile = getById(userId);
        if (profile == null) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            ensureProfile(user);
            profile = getById(userId);
        }
        return toVo(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVo updateCurrentProfile(Long userId, UserProfileDto profileDto) {
        UserProfileEntity profile = getById(userId);
        if (profile == null) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            ensureProfile(user);
            profile = getById(userId);
        }
        if (profileDto.getNickname() != null) {
            profile.setNickname(profileDto.getNickname());
        }
        if (profileDto.getPhone() != null) {
            profile.setPhone(profileDto.getPhone());
        }
        if (profileDto.getGender() != null) {
            profile.setGender(profileDto.getGender());
        }
        if (profileDto.getBirthday() != null) {
            profile.setBirthday(profileDto.getBirthday());
        }
        if (profileDto.getBio() != null) {
            profile.setBio(profileDto.getBio());
        }
        if (profileDto.getAvatar() != null) {
            profile.setAvatar(profileDto.getAvatar());
        }
        profile.setUpdatedAt(LocalDateTime.now());
        updateById(profile);
        syncLegacyUser(userId, profileDto);
        return toVo(profile);
    }

    @Override
    public void ensureProfile(User user) {
        if (user == null || user.getId() == null || getById(user.getId()) != null) {
            return;
        }
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(user.getId());
        profile.setNickname(firstNotBlank(user.getNick_name(), user.getUsername()));
        profile.setPhone(user.getPhone());
        profile.setMemberId(String.valueOf(user.getId()));
        profile.setMemberLevel("普通会员");
        profile.setRegisteredAt(user.getCreateTime() == null ? LocalDateTime.now() : user.getCreateTime());
        profile.setGender(parseGender(user.getGender()));
        if (user.getBirth() != null) {
            profile.setBirthday(user.getBirth().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
        profile.setAvatar(user.getAvatar());
        profile.setStatus(1);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        save(profile);
    }

    @Override
    public UserCenterVo getUserCenter(Long userId) {
        UserCenterVo vo = new UserCenterVo();
        vo.setProfile(getCurrentProfile(userId));
        vo.setCouponCount(countCoupons(userId));
        PointsAccountEntity points = pointsAccountMapper.selectById(userId);
        vo.setCurrentPoints(points == null || points.getCurrentPoints() == null ? 0 : points.getCurrentPoints());
        vo.setFavoriteCount(countFavorites(userId));
        vo.setFootprintCount(countFootprints(userId));
        vo.setPendingPayCount(countOrders(userId, "PendingPay"));
        vo.setPendingShipCount(countPendingShip(userId));   // 包含 Paid + PendingShipment（积分换货订单）
        vo.setPendingReceiveCount(countOrders(userId, "Shipped"));
        vo.setPendingReviewCount(countPendingReview(userId));
        return vo;
    }

    @Override
    public List<AddressVo> listAddresses(Long userId) {
        QueryWrapper<AddressEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).ne("status", 0).orderByDesc("is_default").orderByDesc("updateTime");
        return addressMapper.selectList(wrapper).stream().map(this::toAddressVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVo addAddress(Long userId, AddressDto addressDto) {
        AddressEntity address = new AddressEntity();
        address.setAddressId(idWorker.nextId());
        address.setUserId(userId);
        applyAddress(address, addressDto);
        address.setStatus(1);
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(address.getDefaultAddress())) {
            clearDefaultAddress(userId);
        }
        addressMapper.insert(address);
        return toAddressVo(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVo updateAddress(Long userId, Long addressId, AddressDto addressDto) {
        AddressEntity address = getOwnedAddress(userId, addressId);
        applyAddress(address, addressDto);
        address.setUpdateTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(address.getDefaultAddress())) {
            clearDefaultAddress(userId);
        }
        addressMapper.updateById(address);
        return toAddressVo(address);
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        AddressEntity address = getOwnedAddress(userId, addressId);
        address.setStatus(0);
        address.setUpdateTime(LocalDateTime.now());
        addressMapper.updateById(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(Long userId, Long addressId) {
        AddressEntity address = getOwnedAddress(userId, addressId);
        clearDefaultAddress(userId);
        address.setDefaultAddress(true);
        address.setUpdateTime(LocalDateTime.now());
        addressMapper.updateById(address);
    }

    @Override
    public List<TargetVo> listFavorites(Long userId, String targetType) {
        QueryWrapper<FavoriteEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (!isBlank(targetType)) {
            wrapper.eq("target_type", targetType);
        }
        wrapper.orderByDesc("created_at");
        return favoriteMapper.selectList(wrapper).stream().map(this::toFavoriteVo).collect(Collectors.toList());
    }

    @Override
    public void addFavorite(Long userId, TargetDto targetDto) {
        requireTarget(targetDto);
        if (isFavorite(userId, normalizeTargetType(targetDto.getTargetType()), targetDto.getTargetId())) {
            return;
        }
        FavoriteEntity favorite = new FavoriteEntity();
        favorite.setId(idWorker.nextId());
        favorite.setUserId(userId);
        favorite.setTargetType(normalizeTargetType(targetDto.getTargetType()));
        favorite.setTargetId(targetDto.getTargetId());
        favorite.setTargetSnapshot(targetDto.getTargetSnapshot());
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(favorite);
    }

    @Override
    public void deleteFavorite(Long userId, String targetType, Long targetId) {
        QueryWrapper<FavoriteEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("target_type", normalizeTargetType(targetType)).eq("target_id", targetId);
        favoriteMapper.delete(wrapper);
    }

    @Override
    public Boolean isFavorite(Long userId, String targetType, Long targetId) {
        QueryWrapper<FavoriteEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("target_type", normalizeTargetType(targetType)).eq("target_id", targetId).last("LIMIT 1");
        return favoriteMapper.selectOne(wrapper) != null;
    }

    @Override
    public List<TargetVo> listFootprints(Long userId) {
        QueryWrapper<FootprintEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("view_at");
        return footprintMapper.selectList(wrapper).stream().map(this::toFootprintVo).collect(Collectors.toList());
    }

    @Override
    public void addFootprint(Long userId, TargetDto targetDto) {
        requireTarget(targetDto);
        String targetType = normalizeTargetType(targetDto.getTargetType());
        QueryWrapper<FootprintEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("target_type", targetType).eq("target_id", targetDto.getTargetId()).last("LIMIT 1");
        FootprintEntity footprint = footprintMapper.selectOne(wrapper);
        boolean exists = footprint != null;
        if (footprint == null) {
            footprint = new FootprintEntity();
            footprint.setId(idWorker.nextId());
            footprint.setUserId(userId);
            footprint.setTargetType(targetType);
            footprint.setTargetId(targetDto.getTargetId());
        }
        footprint.setTargetSnapshot(normalizeSnapshotImages(targetDto.getTargetSnapshot()));
        footprint.setViewDate(LocalDate.now());
        footprint.setViewAt(LocalDateTime.now());
        if (exists) {
            footprintMapper.updateById(footprint);
            return;
        }
        footprintMapper.insert(footprint);
    }

    @Override
    public void clearFootprints(Long userId) {
        QueryWrapper<FootprintEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        footprintMapper.delete(wrapper);
    }

    // ===== Admin: user management =====

    @Override
    public Map<String, Object> listUsersAdmin(Integer pageNum, Integer pageSize, String keyword, String role,
                                               Integer status, String startDate, String endDate,
                                               Integer pointsMin, Integer pointsMax) {
        int page = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 20 : pageSize;

        // Points pre-filter: collect qualifying user IDs from points_account
        List<Long> pointsUserIds = null;
        if (pointsMin != null || pointsMax != null) {
            QueryWrapper<PointsAccountEntity> pq = new QueryWrapper<>();
            if (pointsMin != null) pq.ge("current_points", pointsMin);
            if (pointsMax != null) pq.le("current_points", pointsMax);
            List<PointsAccountEntity> pas = pointsAccountMapper.selectList(pq);
            pointsUserIds = pas.stream()
                    .map(PointsAccountEntity::getUserId)
                    .collect(Collectors.toList());
            if (pointsUserIds.isEmpty()) {
                // No users match the points criteria — return empty result
                Map<String, Object> empty = new HashMap<>();
                empty.put("records", new java.util.ArrayList<>());
                empty.put("total", 0L);
                empty.put("pageNum", page);
                empty.put("pageSize", size);
                return empty;
            }
        }

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (!isBlank(keyword)) {
            String kw = "%" + keyword.trim() + "%";
            wrapper.and(w -> w.like("username", kw).or().like("nick_name", kw)
                    .or().like("phone", kw).or().like("email", kw));
        }
        if (!isBlank(role)) {
            // Frontend may send "Customer" or "User" for regular users
            String dbRole = "User".equals(role) ? "Customer" : role;
            wrapper.eq("role", dbRole);
        }
        if (status != null) {
            wrapper.eq("status", status == 1 ? "Active" : "Inactive");
        }
        if (!isBlank(startDate)) {
            wrapper.ge("createTime", startDate + " 00:00:00");
        }
        if (!isBlank(endDate)) {
            wrapper.le("createTime", endDate + " 23:59:59");
        }
        if (pointsUserIds != null) {
            wrapper.in("id", pointsUserIds);
        }
        wrapper.orderByDesc("createTime");

        IPage<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);

        List<Map<String, Object>> records = result.getRecords().stream().map(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", u.getId());
            row.put("username", u.getUsername());
            row.put("nickname", u.getNick_name() != null ? u.getNick_name() : u.getUsername());
            // normalize role: "Customer" becomes "Customer" (frontend roleText handles it)
            row.put("role", u.getRole());
            // status: Active→1, others→0
            row.put("status", "Active".equals(u.getStatus()) ? 1 : 0);
            row.put("phone", u.getPhone());
            row.put("email", u.getEmail());
            row.put("avatar", u.getAvatar());
            row.put("createTime", u.getCreateTime() == null ? null : u.getCreateTime().toString());
            // points from points_account
            PointsAccountEntity pa = pointsAccountMapper.selectById(u.getId());
            row.put("points", pa != null ? pa.getCurrentPoints() : 0);
            // order count
            QueryWrapper<OrderEntity> oq = new QueryWrapper<>();
            oq.eq("customer_id", u.getId());
            row.put("orderCount", orderMapper.selectCount(oq));
            return row;
        }).collect(Collectors.toList());

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("records", records);
        resultMap.put("total", result.getTotal());
        resultMap.put("pageNum", page);
        resultMap.put("pageSize", size);
        return resultMap;
    }

    @Override
    public Map<String, Object> getUserAdmin(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new IllegalArgumentException("用户不存在");
        Map<String, Object> row = new HashMap<>();
        row.put("id", u.getId());
        row.put("username", u.getUsername());
        row.put("nickname", u.getNick_name() != null ? u.getNick_name() : u.getUsername());
        row.put("role", u.getRole());
        row.put("status", "Active".equals(u.getStatus()) ? 1 : 0);
        row.put("phone", u.getPhone());
        row.put("email", u.getEmail());
        row.put("avatar", u.getAvatar());
        row.put("gender", u.getGender());
        row.put("createTime", u.getCreateTime() == null ? null : u.getCreateTime().toString());
        PointsAccountEntity pa = pointsAccountMapper.selectById(userId);
        row.put("points", pa != null ? pa.getCurrentPoints() : 0);
        // order count
        QueryWrapper<OrderEntity> ow = new QueryWrapper<>();
        ow.eq("customer_id", userId);
        row.put("orderCount", orderMapper.selectCount(ow));
        // review count
        QueryWrapper<ProductCommentEntity> cq = new QueryWrapper<>();
        cq.eq("user_id", userId);
        row.put("reviewCount", commentMapper.selectCount(cq));
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createUserAdmin(java.util.Map<String, String> data) {
        String username = data.get("username");
        String password = data.get("password");
        if (isBlank(username)) throw new IllegalArgumentException("用户名不能为空");
        if (isBlank(password)) throw new IllegalArgumentException("密码不能为空");
        if (password.length() < 6) throw new IllegalArgumentException("密码不能少于6位");

        // Check username uniqueness
        QueryWrapper<User> dupCheck = new QueryWrapper<>();
        dupCheck.eq("username", username.trim()).last("LIMIT 1");
        if (userMapper.selectOne(dupCheck) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        // Check phone/email uniqueness if provided
        String phone = data.get("phone");
        String email = data.get("email");
        if (!isBlank(phone)) {
            QueryWrapper<User> phoneCheck = new QueryWrapper<>();
            phoneCheck.eq("phone", phone.trim()).last("LIMIT 1");
            if (userMapper.selectOne(phoneCheck) != null) {
                throw new IllegalArgumentException("手机号已被注册");
            }
        }
        if (!isBlank(email)) {
            QueryWrapper<User> emailCheck = new QueryWrapper<>();
            emailCheck.eq("email", email.trim()).last("LIMIT 1");
            if (userMapper.selectOne(emailCheck) != null) {
                throw new IllegalArgumentException("邮箱已被注册");
            }
        }

        String role = data.getOrDefault("role", "Customer");
        if (!java.util.Arrays.asList("Customer", "ShopOwner", "ShopStaff", "Admin").contains(role)) {
            role = "Customer";
        }

        User user = new User();
        user.setId(idWorker.nextId());
        user.setUsername(username.trim());
        user.setNick_name(isBlank(data.get("nickname")) ? username.trim() : data.get("nickname").trim());
        user.setPhone(isBlank(phone) ? null : phone.trim());
        user.setEmail(isBlank(email) ? null : email.trim());
        user.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt()));
        user.setRole(role);
        user.setStatus("Active");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.addUser(user);
        ensureProfile(user);
        // Initialize points account
        PointsAccountEntity pa = pointsAccountMapper.selectById(user.getId());
        if (pa == null) {
            pa = new PointsAccountEntity();
            pa.setUserId(user.getId());
            pa.setCurrentPoints(0);
            pa.setTotalEarned(0);
            pa.setTotalSpent(0);
            pa.setUpdatedAt(LocalDateTime.now());
            pointsAccountMapper.insert(pa);
        }

        Map<String, Object> row = new HashMap<>();
        row.put("id", user.getId());
        row.put("username", user.getUsername());
        row.put("nickname", user.getNick_name());
        row.put("phone", user.getPhone());
        row.put("email", user.getEmail());
        row.put("role", user.getRole());
        row.put("status", 1);
        row.put("points", 0);
        row.put("orderCount", 0);
        row.put("createTime", user.getCreateTime().toString());
        return row;
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new IllegalArgumentException("用户不存在");
        u.setStatus(status == 1 ? "Active" : "Inactive");
        u.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(u);
    }

    @Override
    public void resetUserPassword(Long userId, String newPassword) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new IllegalArgumentException("用户不存在");
        u.setPassword(org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt()));
        u.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(u);
    }

    private void syncLegacyUser(Long userId, UserProfileDto profileDto) {
        User user = new User();
        user.setId(userId);
        user.setNick_name(profileDto.getNickname());
        user.setPhone(profileDto.getPhone());
        user.setAvatar(profileDto.getAvatar());
        if (profileDto.getBirthday() != null) {
            user.setBirth(Date.from(profileDto.getBirthday().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        if (profileDto.getGender() != null) {
            user.setGender(formatGender(profileDto.getGender()));
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateUser(user);
    }

    private AddressEntity getOwnedAddress(Long userId, Long addressId) {
        AddressEntity address = addressMapper.selectById(addressId);
        if (address == null || !userId.equals(address.getUserId()) || Integer.valueOf(0).equals(address.getStatus())) {
            throw new IllegalArgumentException("地址不存在");
        }
        return address;
    }

    private void clearDefaultAddress(Long userId) {
        UpdateWrapper<AddressEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId).set("is_default", 0);
        addressMapper.update(null, wrapper);
    }

    private void applyAddress(AddressEntity address, AddressDto dto) {
        if (dto.getReceiverName() != null) {
            address.setReceiverName(dto.getReceiverName());
            address.setRecipientName(dto.getReceiverName());
        }
        if (dto.getPhone() != null) address.setPhone(dto.getPhone());
        if (dto.getProvince() != null) {
            address.setProvince(dto.getProvince());
            address.setStateProvince(dto.getProvince());
        }
        if (dto.getCity() != null) address.setCity(dto.getCity());
        if (dto.getDistrict() != null) address.setDistrict(dto.getDistrict());
        if (dto.getDetail() != null) {
            address.setDetail(dto.getDetail());
            address.setStreetAddress(dto.getDetail());
        }
        if (dto.getFullAddress() != null) address.setFullAddress(dto.getFullAddress());
        if (dto.getPostalCode() != null) address.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) address.setCountry(dto.getCountry());
        if (dto.getLongitude() != null) address.setLongitude(dto.getLongitude());
        if (dto.getLatitude() != null) address.setLatitude(dto.getLatitude());
        if (dto.getDefaultAddress() != null) address.setDefaultAddress(dto.getDefaultAddress());
    }

    private AddressVo toAddressVo(AddressEntity address) {
        AddressVo vo = new AddressVo();
        vo.setAddressId(address.getAddressId());
        vo.setReceiverName(firstNotBlank(address.getReceiverName(), address.getRecipientName()));
        vo.setPhone(address.getPhone());
        vo.setProvince(firstNotBlank(address.getProvince(), address.getStateProvince()));
        vo.setCity(address.getCity());
        vo.setDistrict(address.getDistrict());
        vo.setDetail(firstNotBlank(address.getDetail(), address.getStreetAddress()));
        vo.setFullAddress(address.getFullAddress());
        vo.setPostalCode(address.getPostalCode());
        vo.setCountry(address.getCountry());
        vo.setLongitude(address.getLongitude());
        vo.setLatitude(address.getLatitude());
        vo.setDefaultAddress(Boolean.TRUE.equals(address.getDefaultAddress()));
        return vo;
    }

    private TargetVo toFavoriteVo(FavoriteEntity favorite) {
        TargetVo vo = new TargetVo();
        vo.setId(favorite.getId());
        vo.setTargetType(favorite.getTargetType());
        vo.setTargetId(favorite.getTargetId());
        vo.setTargetSnapshot(favorite.getTargetSnapshot());
        vo.setCreatedAt(favorite.getCreatedAt() == null ? null : favorite.getCreatedAt().toString());
        return vo;
    }

    private TargetVo toFootprintVo(FootprintEntity footprint) {
        TargetVo vo = new TargetVo();
        vo.setId(footprint.getId());
        vo.setTargetType(footprint.getTargetType());
        vo.setTargetId(footprint.getTargetId());
        vo.setTargetSnapshot(refreshSnapshotImages(footprint.getTargetSnapshot()));
        vo.setViewAt(footprint.getViewAt() == null ? null : footprint.getViewAt().toString());
        return vo;
    }

    /** 存储前：把 snapshot JSON 里的预签名 URL 替换为 object key */
    private String normalizeSnapshotImages(String snapshot) {
        if (snapshot == null || snapshot.isBlank() || aliOSSUtils == null) return snapshot;
        try {
            snapshot = replaceJsonField(snapshot, "mainImage",
                v -> aliOSSUtils.normalizeForStorage(v));
            snapshot = replaceJsonField(snapshot, "images",
                v -> {
                    // images 是逗号分隔的 URL 列表
                    String[] parts = v.split(",");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < parts.length; i++) {
                        if (i > 0) sb.append(",");
                        String norm = aliOSSUtils.normalizeForStorage(parts[i].trim());
                        sb.append(norm != null ? norm : parts[i].trim());
                    }
                    return sb.toString();
                });
        } catch (Exception ignored) {}
        return snapshot;
    }

    /** 取出时：把 snapshot JSON 里的 object key 或旧预签名 URL 重新签名为新鲜 URL */
    private String refreshSnapshotImages(String snapshot) {
        if (snapshot == null || snapshot.isBlank() || aliOSSUtils == null) return snapshot;
        try {
            snapshot = replaceJsonField(snapshot, "mainImage", v -> freshUrl(v));
            snapshot = replaceJsonField(snapshot, "images", v -> {
                String[] parts = v.split(",");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(freshUrl(parts[i].trim()));
                }
                return sb.toString();
            });
        } catch (Exception ignored) {}
        return snapshot;
    }

    /** 把 objectKey 或旧预签名 URL 统一签名为新鲜 URL */
    private String freshUrl(String v) {
        if (v == null || v.isBlank()) return v;
        // 先尝试归一化（若是 OSS 预签名 URL 则提取 objectKey）
        String key = AliOSSUtils.isObjectKey(v) ? v : aliOSSUtils.normalizeForStorage(v);
        if (key == null || key.isBlank() || !AliOSSUtils.isObjectKey(key)) return v;
        String u = aliOSSUtils.generatePresignedUrl(key, 120);
        return u != null ? u : v;
    }

    /** 用正则把 JSON 字符串里指定字段的值做变换（值为 JSON string） */
    private String replaceJsonField(String json, String field,
                                    java.util.function.UnaryOperator<String> transform) {
        // 匹配 "field":"value" 形式，value 不含未转义双引号
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\"" + field + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        java.util.regex.Matcher m = p.matcher(json);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String original = m.group(1);
            String replaced = transform.apply(original);
            if (replaced == null) replaced = original;
            m.appendReplacement(sb, "\"" + field + "\":\"" +
                    replaced.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\"");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private int countFavorites(Long userId) {
        QueryWrapper<FavoriteEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return favoriteMapper.selectCount(wrapper);
    }

    private int countFootprints(Long userId) {
        QueryWrapper<FootprintEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return footprintMapper.selectCount(wrapper);
    }

    /**
     * MyBatis-Plus 3.4.0 中 selectCount(QueryWrapper) 不自动应用 @TableLogic，
     * 改用 selectPage(searchCount=true) 走同一 SQL Injector，与 list 查询完全一致。
     */
    private int countOrdersWithLogic(Long userId, QueryWrapper<OrderEntity> wrapper) {
        Page<OrderEntity> p = new Page<>(1, 1, true); // searchCount=true 会执行 COUNT
        orderMapper.selectPage(p, wrapper);
        return (int) p.getTotal();
    }

    private int countOrders(Long userId, String status) {
        QueryWrapper<OrderEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("customer_id", userId).eq("order_status", status);
        return countOrdersWithLogic(userId, wrapper);
    }

    /** 待发货数量：包含普通已支付订单（Paid）和积分换货待发货订单（PendingShipment） */
    private int countPendingShip(Long userId) {
        QueryWrapper<OrderEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("customer_id", userId).in("order_status", "Paid", "PendingShipment");
        return countOrdersWithLogic(userId, wrapper);
    }

    /** 待评价数量：包含 PendingReview 和 Received 两种状态 */
    private int countPendingReview(Long userId) {
        QueryWrapper<OrderEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("customer_id", userId).in("order_status", "PendingReview", "Received");
        return countOrdersWithLogic(userId, wrapper);
    }

    private int countCoupons(Long userId) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        QueryWrapper<UserCouponEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("status", 1)
                .and(w -> w.isNull("start_at").or().le("start_at", now))
                .and(w -> w.isNull("end_at").or().ge("end_at", now));
        return userCouponMapper.selectCount(wrapper);
    }

    private void requireTarget(TargetDto targetDto) {
        if (targetDto == null || targetDto.getTargetId() == null) {
            throw new IllegalArgumentException("目标对象不能为空");
        }
    }

    private String normalizeTargetType(String targetType) {
        return isBlank(targetType) ? "product" : targetType;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private UserProfileVo toVo(UserProfileEntity profile) {
        UserProfileVo vo = new UserProfileVo();
        vo.setUserId(profile.getUserId());
        vo.setNickname(profile.getNickname());
        vo.setPhone(profile.getPhone());
        vo.setMemberId(profile.getMemberId());
        vo.setMemberLevel(profile.getMemberLevel());
        vo.setRegisteredDays(calculateRegisteredDays(profile.getRegisteredAt()));
        vo.setGender(profile.getGender());
        vo.setBirthday(profile.getBirthday() == null ? null : profile.getBirthday().toString());
        vo.setBio(profile.getBio());
        vo.setAvatar(profile.getAvatar());
        // 补充鉴权相关字段（从 user 表读取最新 role）
        User user = userMapper.selectById(profile.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getNick_name());
            vo.setRole(user.getRole());
        }
        return vo;
    }

    private Integer calculateRegisteredDays(LocalDateTime registeredAt) {
        if (registeredAt == null) {
            return 0;
        }
        long days = Duration.between(registeredAt, LocalDateTime.now()).toDays();
        return (int) Math.max(days, 0);
    }

    private Integer parseGender(String gender) {
        if ("Male".equalsIgnoreCase(gender) || "男".equals(gender)) {
            return 1;
        }
        if ("Female".equalsIgnoreCase(gender) || "女".equals(gender)) {
            return 2;
        }
        return 0;
    }

    private String formatGender(Integer gender) {
        if (gender == null) {
            return null;
        }
        if (gender == 1) {
            return "Male";
        }
        if (gender == 2) {
            return "Female";
        }
        return "Other";
    }

    private String firstNotBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second;
    }
}
