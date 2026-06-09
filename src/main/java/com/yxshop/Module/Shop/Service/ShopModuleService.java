package com.yxshop.Module.Shop.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxshop.Module.Shop.Dto.ShopDecorationDto;
import com.yxshop.Module.Shop.Dto.ShopDto;
import com.yxshop.Module.Shop.Dto.ShopQueryDto;
import com.yxshop.Module.Shop.Dto.ShopReviewDto;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Vo.ShopVo;

import java.util.List;
import java.util.Map;

public interface ShopModuleService extends IService<ShopEntity> {
    ShopVo getShopDetail(Long shopId);

    ShopVo getShopHome(Long shopId);

    List<ShopVo> listPublicShops(ShopQueryDto queryDto);

    List<ShopVo> listMyShops(Long userId);

    ShopVo applyShop(Long userId, ShopDto shopDto);

    ShopVo updateMyShop(Long userId, Long shopId, ShopDto shopDto);

    ShopVo saveDecoration(Long userId, String operatorRole, Long shopId, ShopDecorationDto decorationDto);

    void disableShop(Long operatorId, String operatorRole, Long shopId);

    void reviewShop(Long reviewerId, ShopReviewDto reviewDto);

    Map<String, Object> listAdminShops(ShopQueryDto queryDto);

    ShopVo updateShopStatus(Long shopId, String status);

    ShopVo adminUpdateShop(Long shopId, ShopDto shopDto);
}
