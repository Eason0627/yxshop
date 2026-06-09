package com.yxshop.Module.Search.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yxshop.Module.Catalog.Dto.AppProductQueryDto;
import com.yxshop.Module.Catalog.Service.AppProductService;
import com.yxshop.Module.Catalog.Vo.CategoryVo;
import com.yxshop.Module.Search.Entity.SearchHistoryEntity;
import com.yxshop.Module.Search.Entity.SearchHotwordEntity;
import com.yxshop.Module.Search.Mapper.SearchHistoryMapper;
import com.yxshop.Module.Search.Mapper.SearchHotwordMapper;
import com.yxshop.Module.Search.Service.SearchService;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SearchServiceImpl implements SearchService {
    private final AppProductService productService;
    private final SearchHistoryMapper historyMapper;
    private final SearchHotwordMapper hotwordMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(12, 1);

    public SearchServiceImpl(AppProductService productService, SearchHistoryMapper historyMapper, SearchHotwordMapper hotwordMapper) {
        this.productService = productService;
        this.historyMapper = historyMapper;
        this.hotwordMapper = hotwordMapper;
    }

    @Override
    public Map<String, Object> searchProducts(Long userId, AppProductQueryDto queryDto) {
        Map<String, Object> result = productService.listPublicProducts(queryDto);
        String keyword = queryDto == null ? null : queryDto.getKeyword();
        if (keyword != null && !keyword.trim().isEmpty()) {
            recordKeyword(userId, keyword.trim(), ((Number) result.get("total")).intValue());
        }
        return result;
    }

    @Override
    public List<?> hotwords() {
        QueryWrapper<SearchHotwordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByDesc("weight").orderByDesc("search_count").last("LIMIT 20");
        return hotwordMapper.selectList(wrapper);
    }

    @Override
    public List<?> history(Long userId) {
        QueryWrapper<SearchHistoryEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("searched_at").last("LIMIT 50");
        return historyMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> meta(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hotwords", hotwords());
        result.put("history", userId == null ? new ArrayList<>() : history(userId));
        result.put("categories", collectCategoryNames(productService.listCategoryTree()));
        return result;
    }

    private void recordKeyword(Long userId, String keyword, Integer resultCount) {
        // 同一用户同一关键词：只更新时间（防止重复记录）
        if (userId != null) {
            QueryWrapper<SearchHistoryEntity> existWrapper = new QueryWrapper<>();
            existWrapper.eq("user_id", userId).eq("keyword", keyword).last("LIMIT 1");
            SearchHistoryEntity existing = historyMapper.selectOne(existWrapper);
            if (existing != null) {
                existing.setSearchedAt(LocalDateTime.now());
                existing.setResultCount(resultCount);
                historyMapper.updateById(existing);
                // 只更新 hotword，不重复 insert history
                updateHotword(keyword);
                return;
            }
        }
        SearchHistoryEntity history = new SearchHistoryEntity();
        history.setId(idWorker.nextId());
        history.setUserId(userId);
        history.setKeyword(keyword);
        history.setResultCount(resultCount);
        history.setSearchedAt(LocalDateTime.now());
        historyMapper.insert(history);

        updateHotword(keyword);
    }

    private void updateHotword(String keyword) {
        QueryWrapper<SearchHotwordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("keyword", keyword).last("LIMIT 1");
        SearchHotwordEntity hotword = hotwordMapper.selectOne(wrapper);
        if (hotword == null) {
            hotword = new SearchHotwordEntity();
            hotword.setId(idWorker.nextId());
            hotword.setKeyword(keyword);
            hotword.setSearchCount(1);
            hotword.setWeight(0);
            hotword.setStatus(1);
        } else {
            hotword.setSearchCount((hotword.getSearchCount() == null ? 0 : hotword.getSearchCount()) + 1);
        }
        hotword.setUpdatedAt(LocalDateTime.now());
        if (hotwordMapper.selectById(hotword.getId()) == null) hotwordMapper.insert(hotword); else hotwordMapper.updateById(hotword);
    }

    private List<String> collectCategoryNames(List<CategoryVo> categories) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        appendCategoryNames(categories, values);
        return new ArrayList<>(values);
    }

    private void appendCategoryNames(List<CategoryVo> categories, LinkedHashSet<String> values) {
        if (categories == null || categories.isEmpty()) {
            return;
        }
        for (CategoryVo category : categories) {
            if (category == null) {
                continue;
            }
            if (category.getCategoryName() != null && !category.getCategoryName().trim().isEmpty()) {
                values.add(category.getCategoryName().trim());
            }
            appendCategoryNames(category.getChildren(), values);
        }
    }
}
