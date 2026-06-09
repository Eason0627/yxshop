package com.yxshop.Module.Shop.Service;

public interface BrandHallService {
    Object listBrands(Integer page, Integer size, String keyword);

    Object brandHome(Long shopId, Integer productLimit);
}
