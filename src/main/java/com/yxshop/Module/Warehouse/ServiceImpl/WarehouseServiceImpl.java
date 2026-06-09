package com.yxshop.Module.Warehouse.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Module.Warehouse.Dto.WarehouseDto;
import com.yxshop.Module.Warehouse.Entity.WarehouseEntity;
import com.yxshop.Module.Warehouse.Mapper.WarehouseMapper;
import com.yxshop.Module.Warehouse.Service.WarehouseService;
import com.yxshop.Module.Warehouse.Vo.WarehouseVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, WarehouseEntity> implements WarehouseService {

    private final ShopModuleMapper shopMapper;

    public WarehouseServiceImpl(ShopModuleMapper shopMapper) {
        this.shopMapper = shopMapper;
    }

    @Override
    public List<WarehouseVo> list(Long operatorId, String operatorRole, Long shopId) {
        LambdaQueryWrapper<WarehouseEntity> wrapper = new LambdaQueryWrapper<WarehouseEntity>()
                .eq(WarehouseEntity::getStatus, "Active")
                .orderByDesc(WarehouseEntity::getIsDefault)
                .orderByDesc(WarehouseEntity::getCreatedAt);
        if ("Admin".equals(operatorRole)) {
            if (shopId != null) wrapper.eq(WarehouseEntity::getShopId, shopId);
        } else {
            Long ownerShopId = resolveShopId(operatorId);
            wrapper.eq(WarehouseEntity::getShopId, ownerShopId);
        }
        return list(wrapper).stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WarehouseVo saveWarehouse(Long operatorId, String operatorRole, WarehouseDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty())
            throw new IllegalArgumentException("仓库名称不能为空");
        Long shopId = "Admin".equals(operatorRole) ? null : resolveShopId(operatorId);

        WarehouseEntity entity;
        if (dto.getId() != null) {
            entity = baseMapper.selectById(dto.getId());
            if (entity == null) throw new IllegalArgumentException("仓库不存在");
            if (!"Admin".equals(operatorRole) && !entity.getShopId().equals(shopId))
                throw new IllegalArgumentException("无权修改其他店铺的仓库");
        } else {
            entity = new WarehouseEntity();
            entity.setShopId(shopId);
            entity.setStatus("Active");
            entity.setIsDefault(0);
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setName(dto.getName().trim());
        if (dto.getContact() != null)       entity.setContact(dto.getContact());
        if (dto.getPhone() != null)         entity.setPhone(dto.getPhone());
        if (dto.getProvince() != null)      entity.setProvince(dto.getProvince());
        if (dto.getCity() != null)          entity.setCity(dto.getCity());
        if (dto.getDistrict() != null)      entity.setDistrict(dto.getDistrict());
        if (dto.getDetailAddress() != null) entity.setDetailAddress(dto.getDetailAddress());
        if (dto.getLng() != null) entity.setLng(dto.getLng());
        if (dto.getLat() != null) entity.setLat(dto.getLat());
        entity.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(entity);

        // 新建时若是首个仓库，或显式指定 isDefault=1，设为默认
        long count = count(new LambdaQueryWrapper<WarehouseEntity>()
                .eq(WarehouseEntity::getShopId, entity.getShopId())
                .eq(WarehouseEntity::getStatus, "Active"));
        if (count == 1 || Integer.valueOf(1).equals(dto.getIsDefault())) {
            setDefaultInternal(entity.getShopId(), entity.getId());
        }
        return toVo(baseMapper.selectById(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long operatorId, String operatorRole, Long id) {
        WarehouseEntity entity = baseMapper.selectById(id);
        if (entity == null) throw new IllegalArgumentException("仓库不存在");
        if (!"Admin".equals(operatorRole) && !entity.getShopId().equals(resolveShopId(operatorId)))
            throw new IllegalArgumentException("无权删除其他店铺的仓库");
        entity.setStatus("Inactive");
        entity.setUpdatedAt(LocalDateTime.now());
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WarehouseVo setDefault(Long operatorId, String operatorRole, Long id) {
        WarehouseEntity target = baseMapper.selectById(id);
        if (target == null) throw new IllegalArgumentException("仓库不存在");
        if (!"Admin".equals(operatorRole) && !target.getShopId().equals(resolveShopId(operatorId)))
            throw new IllegalArgumentException("无权操作其他店铺的仓库");
        setDefaultInternal(target.getShopId(), id);
        return toVo(baseMapper.selectById(id));
    }

    @Override
    public WarehouseVo findWarehouseById(Long id) {
        WarehouseEntity entity = baseMapper.selectById(id);
        return entity == null ? null : toVo(entity);
    }

    // ── private ───────────────────────────────────────────────────────────────

    private void setDefaultInternal(Long shopId, Long targetId) {
        // 先取消该店铺所有仓库的 isDefault
        LambdaQueryWrapper<WarehouseEntity> qw = new LambdaQueryWrapper<WarehouseEntity>()
                .eq(WarehouseEntity::getShopId, shopId)
                .eq(WarehouseEntity::getIsDefault, 1);
        list(qw).forEach(w -> { w.setIsDefault(0); updateById(w); });
        // 设置目标仓库
        WarehouseEntity target = baseMapper.selectById(targetId);
        if (target != null) {
            target.setIsDefault(1);
            target.setUpdatedAt(LocalDateTime.now());
            updateById(target);
        }
    }

    private Long resolveShopId(Long userId) {
        QueryWrapper<ShopEntity> qw = new QueryWrapper<>();
        qw.eq("owner_user_id", userId).eq("status", "Active").last("LIMIT 1");
        ShopEntity shop = shopMapper.selectOne(qw);
        if (shop == null) throw new IllegalArgumentException("当前账号没有已激活的店铺");
        return shop.getShopId();
    }

    private WarehouseVo toVo(WarehouseEntity e) {
        WarehouseVo vo = new WarehouseVo();
        vo.setId(e.getId());
        vo.setShopId(e.getShopId());
        vo.setName(e.getName());
        vo.setContact(e.getContact());
        vo.setPhone(e.getPhone());
        vo.setProvince(e.getProvince());
        vo.setCity(e.getCity());
        vo.setDistrict(e.getDistrict());
        vo.setDetailAddress(e.getDetailAddress());
        vo.setIsDefault(e.getIsDefault());
        vo.setStatus(e.getStatus());
        vo.setLng(e.getLng());
        vo.setLat(e.getLat());
        vo.setCreatedAt(e.getCreatedAt());
        StringBuilder sb = new StringBuilder();
        if (e.getProvince() != null) sb.append(e.getProvince());
        if (e.getCity() != null)     sb.append(e.getCity());
        if (e.getDistrict() != null) sb.append(e.getDistrict());
        if (e.getDetailAddress() != null && !e.getDetailAddress().isBlank())
            sb.append(" ").append(e.getDetailAddress());
        vo.setFullAddress(sb.toString().trim());
        return vo;
    }
}
