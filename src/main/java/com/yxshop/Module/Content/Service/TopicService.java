package com.yxshop.Module.Content.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxshop.Module.Content.Dto.TopicDto;
import com.yxshop.Module.Content.Entity.TopicEntity;

public interface TopicService extends IService<TopicEntity> {
    Object listTopics(Integer page, Integer size, String sortBy);

    Object detail(Long id);

    Object create(Long adminId, TopicDto dto);

    Object adminList(Integer page, Integer size, String keyword, Integer status);

    Object update(Long id, TopicDto dto);

    Object updateStatus(Long id, Integer status);

    void delete(Long id);

    /** 一次性迁移：将旧 JSON content_blocks 批量转换为 WangEditor HTML */
    Object migrateContentBlocks();

}
