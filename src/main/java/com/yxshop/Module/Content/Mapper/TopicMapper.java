package com.yxshop.Module.Content.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yxshop.Module.Content.Entity.TopicEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface TopicMapper extends BaseMapper<TopicEntity> {

    /** 读取所有有 content_blocks 的 topic id 列表 */
    @Select("SELECT id FROM topic WHERE content_blocks IS NOT NULL")
    List<Long> selectIdsWithContentBlocks();

    /**
     * 用 CAST 读取单行 content_blocks 到 Map，强制列名为 cb。
     * 返回 Map 而非 String 避免 MyBatis BLOB→String 映射失败。
     */
    @Select("SELECT CAST(content_blocks AS CHAR CHARACTER SET utf8mb4) AS cb FROM topic WHERE id = #{id}")
    Map<String, Object> selectRawContentBlocksMap(@Param("id") Long id);

    /** 直接更新 content_blocks，不经过实体映射 */
    @Update("UPDATE topic SET content_blocks = #{content} WHERE id = #{id}")
    int updateRawContentBlocks(@Param("id") Long id, @Param("content") String content);
}
