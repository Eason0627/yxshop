package com.yxshop.Module.Points.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxshop.Module.Points.Dto.PointsExchangeDto;
import com.yxshop.Module.Points.Entity.PointsAccountEntity;

public interface PointsAccountService extends IService<PointsAccountEntity> {
    Object getAccount(Long userId);

    Object getRecords(Long userId, Integer page, Integer size);

    Object listGoods(Integer page, Integer size);

    Object getGoods(Long goodsId);

    Object mallMeta();

    Object exchange(Long userId, PointsExchangeDto dto);

    Object listOrders(Long userId, Integer page, Integer size);

    Object checkin(Long userId);

    Object todayCheckin(Long userId);

    Object checkinConfig();
}
