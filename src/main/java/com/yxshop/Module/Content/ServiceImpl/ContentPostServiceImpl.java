package com.yxshop.Module.Content.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yxshop.Module.User.Entity.User;
import com.yxshop.Module.User.Mapper.UserMapper;
import com.yxshop.Module.Content.Dto.ContentCommentDto;
import com.yxshop.Module.Content.Dto.ContentPostDto;
import com.yxshop.Module.Content.Dto.ContentReviewDto;
import com.yxshop.Module.Content.Entity.ContentCommentEntity;
import com.yxshop.Module.Content.Entity.ContentLikeEntity;
import com.yxshop.Module.Content.Entity.ContentPostEntity;
import com.yxshop.Module.Content.Entity.TopicEntity;
import com.yxshop.Module.Content.Mapper.ContentCommentMapper;
import com.yxshop.Module.Content.Mapper.ContentLikeMapper;
import com.yxshop.Module.Content.Mapper.ContentPostMapper;
import com.yxshop.Module.Content.Mapper.TopicMapper;
import com.yxshop.Module.Content.Service.ContentPostService;
import com.yxshop.Module.Content.Vo.ContentCommentVo;
import com.yxshop.Module.Content.Vo.ContentPostVo;
import com.yxshop.Module.Content.Vo.TopicVo;
import com.yxshop.Module.Marketing.Entity.ActivityEntity;
import com.yxshop.Module.Marketing.Mapper.ActivityMapper;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContentPostServiceImpl implements ContentPostService {
    private final ContentPostMapper contentPostMapper;
    private final ContentLikeMapper contentLikeMapper;
    private final ContentCommentMapper contentCommentMapper;
    private final TopicMapper topicMapper;
    private final ActivityMapper activityMapper;
    private final ShopModuleMapper shopModuleMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(25, 1);

    public ContentPostServiceImpl(ContentPostMapper contentPostMapper,
                                  ContentLikeMapper contentLikeMapper,
                                  ContentCommentMapper contentCommentMapper,
                                  TopicMapper topicMapper,
                                  ActivityMapper activityMapper,
                                  ShopModuleMapper shopModuleMapper,
                                  UserMapper userMapper,
                                  ObjectMapper objectMapper) {
        this.contentPostMapper = contentPostMapper;
        this.contentLikeMapper = contentLikeMapper;
        this.contentCommentMapper = contentCommentMapper;
        this.topicMapper = topicMapper;
        this.activityMapper = activityMapper;
        this.shopModuleMapper = shopModuleMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Object feed(Integer page, Integer size, Long topicId, String type, String sortBy, Long currentUserId) {
        LambdaQueryWrapper<ContentPostEntity> wrapper = publicPostWrapper(topicId, type);
        applySort(wrapper, sortBy);
        Page<ContentPostEntity> entityPage = contentPostMapper.selectPage(new Page<>(safePage(page), safeSize(size)), wrapper);
        return toPostVoPage(entityPage, currentUserId);
    }

    @Override
    public Object reels(Integer page, Integer size, Long currentUserId) {
        LambdaQueryWrapper<ContentPostEntity> wrapper = publicPostWrapper(null, "reel")
                .orderByDesc(ContentPostEntity::getCreatedAt);
        Page<ContentPostEntity> entityPage = contentPostMapper.selectPage(new Page<>(safePage(page), safeSize(size)), wrapper);
        return toPostVoPage(entityPage, currentUserId);
    }

    @Override
    public Object discover(Integer topicLimit, Integer activityLimit, Integer brandLimit) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activities", listDiscoverActivities(activityLimit));
        result.put("brands", listDiscoverBrands(brandLimit));
        result.put("recommendTopics", listDiscoverTopics(topicLimit, "recommend"));
        result.put("hotTopics", listDiscoverTopics(topicLimit, "hot"));
        result.put("newTopics", listDiscoverTopics(topicLimit, "new"));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object detail(Long id) {
        ContentPostEntity post = requirePublicPost(id);
        contentPostMapper.update(null, new LambdaUpdateWrapper<ContentPostEntity>()
                .eq(ContentPostEntity::getId, id)
                .setSql("view_count = view_count + 1"));
        post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
        return toPostVo(post, null);
    }

    @Override
    public Object publish(Long userId, ContentPostDto dto) {
        if (dto == null || isBlank(dto.getTitle())) {
            throw new IllegalArgumentException("标题不能为空");
        }
        ContentPostEntity post = new ContentPostEntity();
        post.setId(idWorker.nextId());
        post.setUserId(userId);
        post.setTitle(dto.getTitle().trim());
        post.setContent(dto.getContent());
        post.setCoverImage(dto.getCoverImage());
        post.setImages(dto.getImages());
        post.setVideoUrl(dto.getVideoUrl());
        post.setTags(dto.getTags());
        post.setMusic(dto.getMusic());
        post.setTopicId(dto.getTopicId());
        post.setProductId(dto.getProductId());
        post.setProductName(dto.getProductName());
        post.setProductPrice(dto.getProductPrice());
        post.setProductImage(dto.getProductImage());
        post.setType(isBlank(dto.getType()) ? "post" : dto.getType().trim());
        post.setAuditStatus("Pending");
        post.setStatus(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setShareCount(0);
        post.setViewCount(0);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        contentPostMapper.insert(post);
        return toPostVo(post, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object like(Long userId, Long postId) {
        requirePublicPost(postId);
        ContentLikeEntity exists = contentLikeMapper.selectOne(new LambdaQueryWrapper<ContentLikeEntity>()
                .eq(ContentLikeEntity::getPostId, postId)
                .eq(ContentLikeEntity::getUserId, userId)
                .last("LIMIT 1"));
        boolean liked;
        if (exists == null) {
            ContentLikeEntity like = new ContentLikeEntity();
            like.setId(idWorker.nextId());
            like.setPostId(postId);
            like.setUserId(userId);
            like.setCreatedAt(LocalDateTime.now());
            contentLikeMapper.insert(like);
            contentPostMapper.update(null, new LambdaUpdateWrapper<ContentPostEntity>()
                    .eq(ContentPostEntity::getId, postId)
                    .setSql("like_count = like_count + 1"));
            liked = true;
        } else {
            contentLikeMapper.deleteById(exists.getId());
            contentPostMapper.update(null, new LambdaUpdateWrapper<ContentPostEntity>()
                    .eq(ContentPostEntity::getId, postId)
                    .setSql("like_count = GREATEST(like_count - 1, 0)"));
            liked = false;
        }
        ContentPostEntity post = contentPostMapper.selectById(postId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liked", liked);
        result.put("postId", postId);
        result.put("likeCount", post == null ? 0 : defaultInt(post.getLikeCount()));
        return result;
    }

    @Override
    public Object comments(Long postId, Integer page, Integer size) {
        Page<ContentCommentEntity> entityPage = contentCommentMapper.selectPage(new Page<>(safePage(page), safeSize(size)),
                new LambdaQueryWrapper<ContentCommentEntity>()
                        .eq(ContentCommentEntity::getPostId, postId)
                        .eq(ContentCommentEntity::getStatus, 1)
                        .orderByDesc(ContentCommentEntity::getCreatedAt));
        Page<ContentCommentVo> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toCommentVo).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object addComment(Long userId, ContentCommentDto dto) {
        if (dto == null || dto.getPostId() == null) {
            throw new IllegalArgumentException("内容ID不能为空");
        }
        if (isBlank(dto.getContent())) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        requirePublicPost(dto.getPostId());
        ContentCommentEntity comment = new ContentCommentEntity();
        comment.setId(idWorker.nextId());
        comment.setPostId(dto.getPostId());
        comment.setUserId(userId);
        comment.setParentId(dto.getParentId());
        comment.setContent(dto.getContent().trim());
        comment.setLikeCount(0);
        comment.setStatus(1);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        contentCommentMapper.insert(comment);
        contentPostMapper.update(null, new LambdaUpdateWrapper<ContentPostEntity>()
                .eq(ContentPostEntity::getId, dto.getPostId())
                .setSql("comment_count = comment_count + 1"));
        return toCommentVo(comment);
    }

    @Override
    public Object myPosts(Long userId, Integer page, Integer size) {
        Page<ContentPostEntity> entityPage = contentPostMapper.selectPage(new Page<>(safePage(page), safeSize(size)),
                new LambdaQueryWrapper<ContentPostEntity>()
                        .eq(ContentPostEntity::getUserId, userId)
                        .orderByDesc(ContentPostEntity::getCreatedAt));
        return toPostVoPage(entityPage, userId);
    }

    @Override
    public Object review(Long reviewerId, ContentReviewDto dto) {
        if (dto == null || dto.getPostId() == null) {
            throw new IllegalArgumentException("内容ID不能为空");
        }
        ContentPostEntity post = contentPostMapper.selectById(dto.getPostId());
        if (post == null) {
            throw new IllegalArgumentException("内容不存在");
        }
        String auditStatus = dto.getAuditStatus() == null ? "Rejected" : dto.getAuditStatus();
        if (!"Approved".equals(auditStatus) && !"Rejected".equals(auditStatus) && !"Pending".equals(auditStatus)) {
            throw new IllegalArgumentException("审核状态不合法");
        }
        post.setAuditStatus(auditStatus);
        if (dto.getStatus() != null) {
            post.setStatus(dto.getStatus());
        }
        post.setUpdatedAt(LocalDateTime.now());
        contentPostMapper.updateById(post);
        return toPostVo(post, reviewerId);
    }

    // ── Admin management ──────────────────────────────────────────────────────

    @Override
    public Object adminListPosts(Integer page, Integer size, String keyword, Integer status,
                                 Long topicId, Boolean hasImages, String startDate, String endDate) {
        LambdaQueryWrapper<ContentPostEntity> wrapper = new LambdaQueryWrapper<>();
        if (!isBlank(keyword)) {
            wrapper.and(w -> w.like(ContentPostEntity::getTitle, keyword.trim())
                              .or().like(ContentPostEntity::getContent, keyword.trim()));
        }
        if (status != null) {
            wrapper.eq(ContentPostEntity::getStatus, status);
        }
        if (topicId != null) {
            wrapper.eq(ContentPostEntity::getTopicId, topicId);
        }
        if (Boolean.TRUE.equals(hasImages)) {
            wrapper.isNotNull(ContentPostEntity::getImages)
                   .ne(ContentPostEntity::getImages, "")
                   .ne(ContentPostEntity::getImages, "[]");
        } else if (Boolean.FALSE.equals(hasImages)) {
            wrapper.and(w -> w.isNull(ContentPostEntity::getImages)
                              .or().eq(ContentPostEntity::getImages, "")
                              .or().eq(ContentPostEntity::getImages, "[]"));
        }
        if (!isBlank(startDate)) {
            wrapper.ge(ContentPostEntity::getCreatedAt, startDate.trim() + " 00:00:00");
        }
        if (!isBlank(endDate)) {
            wrapper.le(ContentPostEntity::getCreatedAt, endDate.trim() + " 23:59:59");
        }
        wrapper.orderByDesc(ContentPostEntity::getCreatedAt);
        Page<ContentPostEntity> entityPage = contentPostMapper.selectPage(new Page<>(safePage(page), safeSize(size)), wrapper);
        return toPostVoPage(entityPage, null);
    }

    @Override
    public Object approvePost(Long postId) {
        ContentPostEntity post = requirePost(postId);
        post.setStatus(1);
        post.setAuditStatus("Approved");
        post.setUpdatedAt(LocalDateTime.now());
        contentPostMapper.updateById(post);
        return toPostVo(post, null);
    }

    @Override
    public Object rejectPost(Long postId) {
        ContentPostEntity post = requirePost(postId);
        post.setStatus(-1);
        post.setAuditStatus("Rejected");
        post.setUpdatedAt(LocalDateTime.now());
        contentPostMapper.updateById(post);
        return toPostVo(post, null);
    }

    @Override
    public Object hidePost(Long postId) {
        ContentPostEntity post = requirePost(postId);
        post.setStatus(-2);
        post.setUpdatedAt(LocalDateTime.now());
        contentPostMapper.updateById(post);
        return toPostVo(post, null);
    }

    @Override
    public Object restorePost(Long postId) {
        ContentPostEntity post = requirePost(postId);
        post.setStatus(1);
        post.setAuditStatus("Approved");
        post.setUpdatedAt(LocalDateTime.now());
        contentPostMapper.updateById(post);
        return toPostVo(post, null);
    }

    @Override
    public void deletePost(Long postId) {
        contentPostMapper.deleteById(postId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private LambdaQueryWrapper<ContentPostEntity> publicPostWrapper(Long topicId, String type) {
        LambdaQueryWrapper<ContentPostEntity> wrapper = new LambdaQueryWrapper<ContentPostEntity>()
                .eq(ContentPostEntity::getStatus, 1)
                .eq(ContentPostEntity::getAuditStatus, "Approved");
        if (topicId != null) {
            wrapper.eq(ContentPostEntity::getTopicId, topicId);
        }
        if (!isBlank(type)) {
            wrapper.eq(ContentPostEntity::getType, type.trim());
        }
        return wrapper;
    }

    private List<ActivityEntity> listDiscoverActivities(Integer limit) {
        int size = Math.min(Math.max(limit == null ? 6 : limit, 1), 20);
        return activityMapper.selectList(new LambdaQueryWrapper<ActivityEntity>()
                .eq(ActivityEntity::getStatus, 1)
                .orderByDesc(ActivityEntity::getPriority)
                .orderByDesc(ActivityEntity::getHotPercent)
                .last("LIMIT " + size));
    }

    private List<Map<String, Object>> listDiscoverBrands(Integer limit) {
        int size = Math.min(Math.max(limit == null ? 10 : limit, 1), 20);
        return shopModuleMapper.selectList(new LambdaQueryWrapper<ShopEntity>()
                        .eq(ShopEntity::getStatus, "Active")
                        .eq(ShopEntity::getIsBrandShop, 1)
                        .orderByDesc(ShopEntity::getFollowers)
                        .orderByDesc(ShopEntity::getSales)
                        .last("LIMIT " + size))
                .stream()
                .map(this::toDiscoverBrand)
                .collect(Collectors.toList());
    }

    private List<TopicVo> listDiscoverTopics(Integer limit, String sortBy) {
        int size = Math.min(Math.max(limit == null ? 12 : limit, 1), 30);
        LambdaQueryWrapper<TopicEntity> wrapper = new LambdaQueryWrapper<TopicEntity>()
                .eq(TopicEntity::getStatus, 1);
        if ("hot".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(TopicEntity::getLikes)
                    .orderByDesc(TopicEntity::getViews)
                    .orderByDesc(TopicEntity::getPublishedAt);
        } else {
            wrapper.orderByDesc(TopicEntity::getPublishedAt);
        }
        return topicMapper.selectList(wrapper.last("LIMIT " + size))
                .stream()
                .map(this::toTopicVo)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toDiscoverBrand(ShopEntity shop) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("shopId", shop.getShopId());
        item.put("shopName", firstNotBlank(shop.getDisplayName(), shop.getShopName()));
        item.put("displayName", firstNotBlank(shop.getDisplayName(), shop.getShopName()));
        item.put("logo", firstNotBlank(shop.getLogo(), shop.getAvatar()));
        item.put("avatar", firstNotBlank(shop.getAvatar(), shop.getLogo()));
        item.put("banner", shop.getBanner());
        item.put("shopImage", shop.getShopImage());
        item.put("shopDescription", shop.getShopDescription());
        item.put("followers", defaultInt(shop.getFollowers()));
        item.put("productCount", defaultInt(shop.getProductCount()));
        item.put("sales", defaultInt(shop.getSales()));
        item.put("rating", shop.getRating());
        return item;
    }

    private void applySort(LambdaQueryWrapper<ContentPostEntity> wrapper, String sortBy) {
        if ("hot".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(ContentPostEntity::getLikeCount)
                    .orderByDesc(ContentPostEntity::getViewCount)
                    .orderByDesc(ContentPostEntity::getCreatedAt);
            return;
        }
        wrapper.orderByDesc(ContentPostEntity::getCreatedAt);
    }

    private ContentPostEntity requirePublicPost(Long id) {
        ContentPostEntity post = contentPostMapper.selectById(id);
        if (post == null || post.getStatus() == null || post.getStatus() != 1 || !"Approved".equals(post.getAuditStatus())) {
            throw new IllegalArgumentException("内容不存在或未通过审核");
        }
        return post;
    }

    private ContentPostEntity requirePost(Long id) {
        ContentPostEntity post = contentPostMapper.selectById(id);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        return post;
    }

    private Page<ContentPostVo> toPostVoPage(Page<ContentPostEntity> entityPage, Long currentUserId) {
        Page<ContentPostVo> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<ContentPostVo> records = entityPage.getRecords().stream()
                .map(post -> toPostVo(post, currentUserId))
                .collect(Collectors.toList());
        voPage.setRecords(records);
        return voPage;
    }

    private ContentPostVo toPostVo(ContentPostEntity post, Long currentUserId) {
        ContentPostVo vo = new ContentPostVo();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        User user = post.getUserId() == null ? null : userMapper.getUserById(post.getUserId());
        String displayName = user == null ? "用户" + shortId(post.getUserId()) : firstNotBlank(user.getNick_name(), user.getUsername());
        String avatar = user == null ? null : user.getAvatar();
        vo.setAuthorName(displayName);
        vo.setAuthorAvatar(avatar);
        vo.setUsername(displayName);      // alias
        vo.setUserAvatar(avatar);          // alias
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setCoverImage(post.getCoverImage());
        vo.setImages(parseImages(post.getImages()));
        vo.setVideoUrl(post.getVideoUrl());
        vo.setTags(post.getTags());
        vo.setMusic(post.getMusic());
        vo.setTopicId(post.getTopicId());
        if (post.getTopicId() != null) {
            TopicEntity topic = topicMapper.selectById(post.getTopicId());
            String topicTitle = topic == null ? null : topic.getTitle();
            vo.setTopicTitle(topicTitle);
            vo.setTopicName(topicTitle);   // alias
        }
        vo.setProductId(post.getProductId());
        vo.setProductName(post.getProductName());
        vo.setProductPrice(post.getProductPrice());
        vo.setProductImage(post.getProductImage());
        vo.setType(post.getType());
        vo.setAuditStatus(post.getAuditStatus());
        vo.setStatus(post.getStatus());
        vo.setLikeCount(defaultInt(post.getLikeCount()));
        vo.setCommentCount(defaultInt(post.getCommentCount()));
        vo.setShareCount(defaultInt(post.getShareCount()));
        vo.setViewCount(defaultInt(post.getViewCount()));
        vo.setCreatedAt(post.getCreatedAt());
        vo.setUpdatedAt(post.getUpdatedAt());
        if (post.getCreatedAt() != null) {
            vo.setCreateTime(post.getCreatedAt().toString());
        }
        vo.setLiked(currentUserId != null && isLiked(currentUserId, post.getId()));
        return vo;
    }

    private ContentCommentVo toCommentVo(ContentCommentEntity comment) {
        ContentCommentVo vo = new ContentCommentVo();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setUserId(comment.getUserId());
        User user = comment.getUserId() == null ? null : userMapper.getUserById(comment.getUserId());
        vo.setAuthorName(user == null ? "用户" + shortId(comment.getUserId()) : firstNotBlank(user.getNick_name(), user.getUsername()));
        vo.setAuthorAvatar(user == null ? null : user.getAvatar());
        vo.setParentId(comment.getParentId());
        vo.setContent(comment.getContent());
        vo.setLikeCount(defaultInt(comment.getLikeCount()));
        vo.setStatus(comment.getStatus());
        vo.setCreatedAt(comment.getCreatedAt());
        return vo;
    }

    private TopicVo toTopicVo(TopicEntity topic) {
        TopicVo vo = new TopicVo();
        vo.setId(topic.getId());
        vo.setTitle(topic.getTitle());
        vo.setName(topic.getTitle());      // alias
        vo.setAuthorId(topic.getAuthorId());
        User user = topic.getAuthorId() == null ? null : userMapper.getUserById(topic.getAuthorId());
        vo.setAuthorName(user == null ? topic.getAuthorName() : firstNotBlank(user.getNick_name(), topic.getAuthorName()));
        vo.setAuthorAvatar(user == null ? null : user.getAvatar());
        vo.setCoverImage(topic.getCoverImage());
        vo.setImage(topic.getCoverImage());  // alias
        vo.setLikes(defaultInt(topic.getLikes()));
        vo.setViews(defaultInt(topic.getViews()));
        vo.setContentBlocks(topic.getContentBlocks());
        vo.setStatus(topic.getStatus());
        vo.setPublishedAt(topic.getPublishedAt());
        return vo;
    }

    private boolean isLiked(Long userId, Long postId) {
        return contentLikeMapper.selectCount(new LambdaQueryWrapper<ContentLikeEntity>()
                .eq(ContentLikeEntity::getUserId, userId)
                .eq(ContentLikeEntity::getPostId, postId)) > 0;
    }

    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.trim().isEmpty()) return null;
        String trimmed = imagesJson.trim();
        // JSON array format: ["url1","url2"]
        if (trimmed.startsWith("[")) {
            try {
                return objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                return null;
            }
        }
        // Comma-separated format: url1,url2,url3
        String[] parts = trimmed.split(",");
        List<String> result = new java.util.ArrayList<>();
        for (String part : parts) {
            String url = part.trim();
            if (!url.isEmpty()) result.add(url);
        }
        return result.isEmpty() ? null : result;
    }

    private long safePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long safeSize(Integer size) {
        return size == null || size < 1 ? 20 : Math.min(size, 100);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String firstNotBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private String shortId(Long id) {
        if (id == null) return "";
        String value = String.valueOf(id);
        return value.length() <= 4 ? value : value.substring(value.length() - 4);
    }
}
