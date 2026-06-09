package com.yxshop.Module.Content.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.User.Entity.User;
import com.yxshop.Module.User.Mapper.UserMapper;
import com.yxshop.Module.Content.Dto.TopicDto;
import com.yxshop.Module.Content.Entity.TopicEntity;
import com.yxshop.Module.Content.Mapper.TopicMapper;
import com.yxshop.Module.Content.Service.TopicService;
import com.yxshop.Module.Content.Vo.TopicVo;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Primary
@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, TopicEntity> implements TopicService {
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(27, 1);
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;
    private final com.yxshop.Utils.AliOSSUtils aliOSSUtils;

    public TopicServiceImpl(UserMapper userMapper,
                             JdbcTemplate jdbcTemplate,
                             com.yxshop.Utils.AliOSSUtils aliOSSUtils) {
        this.userMapper = userMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.aliOSSUtils = aliOSSUtils;
    }

    @Override
    public Object listTopics(Integer page, Integer size, String sortBy) {
        LambdaQueryWrapper<TopicEntity> wrapper = new LambdaQueryWrapper<TopicEntity>()
                .eq(TopicEntity::getStatus, 1);
        applySortBy(wrapper, sortBy);
        Page<TopicEntity> entityPage = baseMapper.selectPage(new Page<>(safePage(page), safeSize(size)), wrapper);
        Page<TopicVo> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object detail(Long id) {
        TopicEntity topic = baseMapper.selectById(id);
        if (topic == null || topic.getStatus() == null || topic.getStatus() != 1) {
            throw new RuntimeException("话题不存在或已下架");
        }
        baseMapper.update(null, new LambdaUpdateWrapper<TopicEntity>()
                .eq(TopicEntity::getId, id)
                .setSql("views = views + 1"));
        topic.setViews((topic.getViews() == null ? 0 : topic.getViews()) + 1);
        return toVo(topic);
    }

    @Override
    public Object create(Long adminId, TopicDto dto) {
        if (dto == null) throw new RuntimeException("请求体不能为空");
        String resolvedTitle = dto.resolvedTitle();
        if (resolvedTitle == null || resolvedTitle.isEmpty()) {
            throw new RuntimeException("话题标题不能为空");
        }
        TopicEntity topic = new TopicEntity();
        topic.setId(idWorker.nextId());
        topic.setTitle(resolvedTitle);
        topic.setAuthorId(adminId);
        topic.setAuthorName("Admin");
        topic.setCoverImage(aliOSSUtils.normalizeForStorage(dto.resolvedCoverImage()));
        topic.setLikes(0);
        topic.setViews(0);
        topic.setContentBlocks(dto.getContentBlocks());
        topic.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        topic.setPublishedAt(LocalDateTime.now());
        baseMapper.insert(topic);
        return toVo(topic);
    }

    @Override
    public Object adminList(Integer page, Integer size, String keyword, Integer status) {
        LambdaQueryWrapper<TopicEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(TopicEntity::getTitle, keyword.trim());
        }
        if (status != null) {
            wrapper.eq(TopicEntity::getStatus, status);
        }
        wrapper.orderByDesc(TopicEntity::getPublishedAt);
        Page<TopicEntity> entityPage = baseMapper.selectPage(new Page<>(safePage(page), safeSize(size)), wrapper);
        Page<TopicVo> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object update(Long id, TopicDto dto) {
        TopicEntity topic = baseMapper.selectById(id);
        if (topic == null) throw new RuntimeException("话题不存在");
        String resolvedTitle = dto.resolvedTitle();
        if (resolvedTitle != null && !resolvedTitle.isEmpty()) {
            topic.setTitle(resolvedTitle);
        }
        String resolvedImage = aliOSSUtils.normalizeForStorage(dto.resolvedCoverImage());
        if (resolvedImage != null) {
            topic.setCoverImage(resolvedImage);
        }
        if (dto.getContentBlocks() != null) {
            topic.setContentBlocks(dto.getContentBlocks());
        }
        if (dto.getStatus() != null) {
            topic.setStatus(dto.getStatus());
        }
        baseMapper.updateById(topic);
        return toVo(topic);
    }

    @Override
    public Object updateStatus(Long id, Integer status) {
        TopicEntity topic = baseMapper.selectById(id);
        if (topic == null) throw new RuntimeException("话题不存在");
        topic.setStatus(status);
        baseMapper.updateById(topic);
        return toVo(topic);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        TopicEntity topic = baseMapper.selectById(id);
        if (topic == null) throw new RuntimeException("话题不存在");
        baseMapper.deleteById(id);
    }

    @Override
    public Object migrateContentBlocks() {
        // 用 RowCallbackHandler + rs.getString() 直接读 TEXT 列，最可靠
        final int[] counters = {0, 0, 0}; // [total, updated, skipped]
        jdbcTemplate.query("SELECT id, content_blocks FROM topic WHERE content_blocks IS NOT NULL",
                (rs) -> {
                    counters[0]++;
                    long id = rs.getLong("id");
                    // 用 getBytes() 拿原始字节，再用 UTF-8 解码，避免 JDBC characterEncoding 不一致导致乱码
                    byte[] rawBytes = rs.getBytes("content_blocks");
                    if (rawBytes == null) { counters[2]++; return; }
                    String cb = new String(rawBytes, StandardCharsets.UTF_8);
                    if (cb.trim().isEmpty() || !cb.trim().startsWith("[")) {
                        counters[2]++;
                        return;
                    }
                    String html = jsonBlocksToHtml(cb);
                    if (!html.equals(cb)) {
                        // 同样用字节写回，避免写入时再次编码错乱
                        jdbcTemplate.update("UPDATE topic SET content_blocks = ? WHERE id = ?",
                                html, id);
                        counters[1]++;
                    } else {
                        counters[2]++;
                    }
                });
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("total", counters[0]);
        result.put("updated", counters[1]);
        result.put("skipped", counters[2]);
        return result;
    }

    /**
     * 将旧版 content_blocks JSON 数组转换为 WangEditor HTML。
     * 旧格式：[{"type":"text","content":"..."}, {"type":"image","url":"..."}]
     */
    private String jsonBlocksToHtml(String raw) {
        if (raw == null || raw.trim().isEmpty()) return raw;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            // 允许 JSON 字符串值内含有字面量控制字符（如 \n），mock 数据中存在此情况
            mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
            java.util.List<java.util.Map> blocks = mapper.readValue(raw.trim(),
                    mapper.getTypeFactory().constructCollectionType(java.util.List.class, java.util.Map.class));
            StringBuilder sb = new StringBuilder();
            for (java.util.Map<?, ?> block : blocks) {
                String type = String.valueOf(block.get("type"));
                if ("text".equals(type)) {
                    String content = block.get("content") == null ? "" : String.valueOf(block.get("content"));
                    for (String line : content.split("\n", -1)) {
                        String esc = line
                                .replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;");
                        sb.append("<p>").append(esc.isEmpty() ? "<br>" : esc).append("</p>");
                    }
                } else if ("image".equals(type)) {
                    Object url = block.get("url");
                    if (url != null && !url.toString().isEmpty()) {
                        sb.append("<p><img src=\"").append(url).append("\" style=\"max-width:100%\"/></p>");
                    }
                }
            }
            return sb.length() > 0 ? sb.toString() : raw;
        } catch (Exception e) {
            return raw;
        }
    }

    private TopicVo toVo(TopicEntity topic) {
        TopicVo vo = new TopicVo();
        vo.setId(topic.getId());
        vo.setTitle(topic.getTitle());
        vo.setName(topic.getTitle());            // alias for frontend
        vo.setAuthorId(topic.getAuthorId());
        User user = topic.getAuthorId() == null ? null : userMapper.getUserById(topic.getAuthorId());
        vo.setAuthorName(user == null ? topic.getAuthorName() : firstNotBlank(user.getNick_name(), topic.getAuthorName()));
        vo.setAuthorAvatar(user == null ? null : user.getAvatar());
        vo.setCoverImage(topic.getCoverImage());
        vo.setImage(topic.getCoverImage());      // alias for frontend
        vo.setDescription(null);                 // not in entity
        vo.setIsHot(false);                      // not in entity, default false
        vo.setSort(0);                           // not in entity, default 0
        vo.setPostCount(0);                      // not computed, default 0
        vo.setLikes(topic.getLikes() == null ? 0 : topic.getLikes());
        vo.setViews(topic.getViews() == null ? 0 : topic.getViews());
        vo.setViewCount(topic.getViews() == null ? 0 : topic.getViews()); // alias
        vo.setContentBlocks(topic.getContentBlocks());
        vo.setStatus(topic.getStatus());
        vo.setPublishedAt(topic.getPublishedAt());
        return vo;
    }

    private void applySortBy(LambdaQueryWrapper<TopicEntity> wrapper, String sortBy) {
        if ("hot".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(TopicEntity::getLikes)
                    .orderByDesc(TopicEntity::getViews)
                    .orderByDesc(TopicEntity::getPublishedAt);
        } else {
            wrapper.orderByDesc(TopicEntity::getPublishedAt);
        }
    }

    private String firstNotBlank(String first, String second) {
        return first == null || first.trim().isEmpty() ? second : first;
    }

    private long safePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long safeSize(Integer size) {
        return size == null || size < 1 ? 20 : Math.min(size, 100);
    }
}
