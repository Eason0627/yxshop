package com.yxshop.Module.Search.Service;

import com.yxshop.Module.Catalog.Dto.AppProductQueryDto;

import java.util.List;
import java.util.Map;

public interface SearchService {
    Map<String, Object> searchProducts(Long userId, AppProductQueryDto queryDto);

    List<?> hotwords();

    List<?> history(Long userId);

    Map<String, Object> meta(Long userId);
}
