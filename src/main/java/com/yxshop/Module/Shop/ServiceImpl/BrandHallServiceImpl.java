package com.yxshop.Module.Shop.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxshop.Module.Catalog.Entity.AppProductEntity;
import com.yxshop.Module.Catalog.Mapper.AppProductMapper;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Module.Shop.Service.BrandHallService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BrandHallServiceImpl implements BrandHallService {
    private final ShopModuleMapper shopModuleMapper;
    private final AppProductMapper appProductMapper;

    public BrandHallServiceImpl(ShopModuleMapper shopModuleMapper, AppProductMapper appProductMapper) {
        this.shopModuleMapper = shopModuleMapper;
        this.appProductMapper = appProductMapper;
    }

    @Override
    public Object listBrands(Integer page, Integer size, String keyword) {
        LambdaQueryWrapper<ShopEntity> wrapper = new LambdaQueryWrapper<ShopEntity>()
                .eq(ShopEntity::getIsBrandShop, 1)
                .eq(ShopEntity::getStatus, "Active")
                .orderByDesc(ShopEntity::getSales)
                .orderByDesc(ShopEntity::getRating);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(q -> q.like(ShopEntity::getShopName, kw).or().like(ShopEntity::getDisplayName, kw));
        }
        return shopModuleMapper.selectPage(new Page<>(safePage(page), safeSize(size)), wrapper);
    }

    @Override
    public Object brandHome(Long shopId, Integer productLimit) {
        ShopEntity shop = shopModuleMapper.selectById(shopId);
        if (shop == null || shop.getIsBrandShop() == null || shop.getIsBrandShop() != 1 || !"Active".equals(shop.getStatus())) {
            throw new RuntimeException("品牌店铺不存在或未启用");
        }
        List<AppProductEntity> products = appProductMapper.selectList(new LambdaQueryWrapper<AppProductEntity>()
                .eq(AppProductEntity::getStatus, 1)
                .eq(AppProductEntity::getAuditStatus, "Approved")
                .and(q -> q.eq(AppProductEntity::getShopId, shopId).or().eq(AppProductEntity::getBrandShopId, shopId))
                .orderByDesc(AppProductEntity::getSales)
                .last("LIMIT " + Math.min(productLimit == null ? 12 : productLimit, 50)));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shop", shop);
        result.put("products", products);
        result.put("productCount", products.size());
        return result;
    }

    private long safePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long safeSize(Integer size) {
        return size == null || size < 1 ? 20 : Math.min(size, 100);
    }
}
