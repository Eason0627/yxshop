package com.yxshop.Module.Catalog.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.Catalog.Dto.AppProductDto;
import com.yxshop.Module.Catalog.Dto.AppProductQueryDto;
import com.yxshop.Module.Catalog.Dto.ProductCommentDto;
import com.yxshop.Module.Catalog.Dto.ProductReviewDto;
import com.yxshop.Module.Catalog.Dto.ProductSpecTemplateDto;
import com.yxshop.Module.Catalog.Dto.RecommendationQueryDto;
import com.yxshop.Module.Catalog.Entity.AppProductEntity;
import com.yxshop.Module.Catalog.Entity.CategoryEntity;
import com.yxshop.Module.Catalog.Entity.ProductCommentEntity;
import com.yxshop.Module.Catalog.Entity.ProductSpecTemplateEntity;
import com.yxshop.Module.Catalog.Mapper.AppProductMapper;
import com.yxshop.Module.Catalog.Mapper.CategoryModuleMapper;
import com.yxshop.Module.Catalog.Mapper.ProductCommentMapper;
import com.yxshop.Module.Catalog.Mapper.ProductSpecTemplateMapper;
import com.yxshop.Module.Catalog.Service.AppProductService;
import com.yxshop.Module.Catalog.Vo.AppProductVo;
import com.yxshop.Module.Catalog.Vo.CategoryVo;
import com.yxshop.Module.Catalog.Vo.ProductCommentVo;
import com.yxshop.Module.Marketing.Dto.PromotionCalculateDto;
import com.yxshop.Module.Marketing.Dto.PromotionItemDto;
import com.yxshop.Module.Marketing.Entity.ActivityEntity;
import com.yxshop.Module.Marketing.Entity.ChannelEntity;
import com.yxshop.Module.Marketing.Mapper.ActivityMapper;
import com.yxshop.Module.Marketing.Mapper.ChannelMapper;
import com.yxshop.Module.Marketing.Service.PromotionCalculationService;
import com.yxshop.Module.Order.Entity.OrderEntity;
import com.yxshop.Module.Order.Entity.OrderItemEntity;
import com.yxshop.Module.Order.Mapper.OrderItemMapper;
import com.yxshop.Module.Order.Mapper.OrderModuleMapper;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Module.User.Entity.FavoriteEntity;
import com.yxshop.Module.User.Entity.FootprintEntity;
import com.yxshop.Module.User.Mapper.FavoriteMapper;
import com.yxshop.Module.User.Mapper.FootprintMapper;
import com.yxshop.Utils.AliOSSUtils;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Primary
@Service
public class AppProductServiceImpl extends ServiceImpl<AppProductMapper, AppProductEntity> implements AppProductService {

    private final ShopModuleMapper shopModuleMapper;
    private final CategoryModuleMapper categoryModuleMapper;
    private final ProductCommentMapper productCommentMapper;
    private final ProductSpecTemplateMapper productSpecTemplateMapper;
    private final OrderModuleMapper orderModuleMapper;
    private final OrderItemMapper orderItemMapper;
    private final PromotionCalculationService promotionCalculationService;
    private final ActivityMapper activityMapper;
    private final ChannelMapper channelMapper;
    private final FootprintMapper footprintMapper;
    private final FavoriteMapper favoriteMapper;
    private final AliOSSUtils aliOSSUtils;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(3, 1);

    public AppProductServiceImpl(ShopModuleMapper shopModuleMapper,
                                 CategoryModuleMapper categoryModuleMapper,
                                 ProductCommentMapper productCommentMapper,
                                 ProductSpecTemplateMapper productSpecTemplateMapper,
                                 OrderModuleMapper orderModuleMapper,
                                 OrderItemMapper orderItemMapper,
                                 PromotionCalculationService promotionCalculationService,
                                 ActivityMapper activityMapper,
                                 ChannelMapper channelMapper,
                                 FootprintMapper footprintMapper,
                                 FavoriteMapper favoriteMapper,
                                 AliOSSUtils aliOSSUtils) {
        this.shopModuleMapper = shopModuleMapper;
        this.categoryModuleMapper = categoryModuleMapper;
        this.productCommentMapper = productCommentMapper;
        this.productSpecTemplateMapper = productSpecTemplateMapper;
        this.orderModuleMapper = orderModuleMapper;
        this.orderItemMapper = orderItemMapper;
        this.promotionCalculationService = promotionCalculationService;
        this.activityMapper = activityMapper;
        this.channelMapper = channelMapper;
        this.footprintMapper = footprintMapper;
        this.favoriteMapper = favoriteMapper;
        this.aliOSSUtils = aliOSSUtils;
    }

    @Override
    public Map<String, Object> listPublicProducts(AppProductQueryDto queryDto) {
        AppProductQueryDto query = queryDto == null ? new AppProductQueryDto() : queryDto;
        int pageNum = validPageNum(query.getPageNum());
        int pageSize = validPageSize(query.getPageSize());
        QueryWrapper<AppProductEntity> wrapper = buildProductQuery(query);
        wrapper.eq("status", 1).eq("audit_status", "Approved");
        applySortExt(wrapper, query);
        Page<AppProductEntity> page = page(new Page<>(pageNum, pageSize), wrapper);
        return pageResult(page, pageNum, pageSize);
    }

    @Override
    public AppProductVo getProductDetail(Long productId) {
        return getProductDetail(null, productId);
    }

    @Override
    public AppProductVo getProductDetail(Long userId, Long productId) {
        AppProductEntity product = getById(productId);
        if (product == null || product.getStatus() == null || product.getStatus() != 1 || !"Approved".equals(product.getAuditStatus())) {
            throw new IllegalArgumentException("商品不存在");
        }
        AppProductVo vo = toVo(product);
        fillProductAggregates(vo, product, userId);
        return vo;
    }

    @Override
    public Map<String, Object> recommendationProducts(Long userId, RecommendationQueryDto queryDto) {
        RecommendationQueryDto query = queryDto == null ? new RecommendationQueryDto() : queryDto;
        String scene = query.getScene() == null ? "recommend" : query.getScene();
        if ("channel".equalsIgnoreCase(scene)) {
            return channelProducts(query);
        }
        if ("activity".equalsIgnoreCase(scene)) {
            return activityProducts(query);
        }
        return personalizedProducts(userId, query);
    }

    @Override
    public List<CategoryVo> listCategoryTree() {
        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByAsc("sort").orderByAsc("category_id");
        List<CategoryVo> categories = categoryModuleMapper.selectList(wrapper).stream()
                .map(this::toCategoryVo)
                .collect(Collectors.toList());
        Map<Long, CategoryVo> nodeMap = new LinkedHashMap<>();
        for (CategoryVo category : categories) {
            nodeMap.put(category.getCategoryId(), category);
        }
        List<CategoryVo> roots = new ArrayList<>();
        for (CategoryVo category : categories) {
            Long parentId = category.getParentCategoryId();
            if (parentId == null || parentId == 0 || !nodeMap.containsKey(parentId)) {
                roots.add(category);
            } else {
                nodeMap.get(parentId).getChildren().add(category);
            }
        }
        return roots;
    }

    @Override
    public List<CategoryVo> listAllCategoryTree() {
        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort").orderByAsc("category_id");
        List<CategoryVo> categories = categoryModuleMapper.selectList(wrapper).stream()
                .map(this::toCategoryVo)
                .collect(Collectors.toList());
        Map<Long, CategoryVo> nodeMap = new LinkedHashMap<>();
        for (CategoryVo c : categories) nodeMap.put(c.getCategoryId(), c);
        List<CategoryVo> roots = new ArrayList<>();
        for (CategoryVo c : categories) {
            Long pid = c.getParentCategoryId();
            if (pid == null || pid == 0 || !nodeMap.containsKey(pid)) roots.add(c);
            else nodeMap.get(pid).getChildren().add(c);
        }
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVo saveCategory(Long operatorId, String operatorRole, com.yxshop.Module.Catalog.Dto.CategoryDto dto) {
        if (!"Admin".equals(operatorRole)) {
            throw new IllegalArgumentException("仅管理员可管理分类");
        }
        requireNotBlank(dto.getCategoryName(), "分类名称不能为空");
        CategoryEntity entity;
        if (dto.getCategoryId() != null) {
            entity = categoryModuleMapper.selectById(dto.getCategoryId());
            if (entity == null) throw new IllegalArgumentException("分类不存在");
        } else {
            entity = new CategoryEntity();
            entity.setCategoryId(idWorker.nextId());
        }
        entity.setCategoryName(dto.getCategoryName());
        entity.setParentCategoryId(dto.getParentCategoryId() == null || dto.getParentCategoryId() == 0 ? null : dto.getParentCategoryId());
        entity.setDescription(dto.getDescription());
        entity.setImageUrl(dto.getImageUrl());
        entity.setIcon(dto.getIcon());
        entity.setBanner(dto.getBanner());
        entity.setSort(dto.getSort() == null ? 0 : dto.getSort());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        // 计算层级
        if (entity.getParentCategoryId() == null) {
            entity.setLevel(1);
        } else {
            CategoryEntity parent = categoryModuleMapper.selectById(entity.getParentCategoryId());
            entity.setLevel(parent == null ? 1 : (parent.getLevel() == null ? 1 : parent.getLevel() + 1));
        }
        entity.setUpdateTime(java.time.LocalDateTime.now());
        if (dto.getCategoryId() == null) {
            entity.setCreateTime(java.time.LocalDateTime.now());
            categoryModuleMapper.insert(entity);
        } else {
            categoryModuleMapper.updateById(entity);
        }
        return toCategoryVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategoryAdmin(Long operatorId, String operatorRole, Long categoryId) {
        if (!"Admin".equals(operatorRole)) {
            throw new IllegalArgumentException("仅管理员可删除分类");
        }
        // 检查是否有子分类
        QueryWrapper<CategoryEntity> childCheck = new QueryWrapper<>();
        childCheck.eq("parent_category_id", categoryId);
        if (categoryModuleMapper.selectCount(childCheck) > 0) {
            throw new IllegalArgumentException("请先删除子分类");
        }
        // 检查是否有商品
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AppProductEntity> productCheck
                = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        productCheck.eq("category_id", categoryId).ne("status", 2);
        if (count(productCheck) > 0) {
            throw new IllegalArgumentException("该分类下存在商品，请先移除商品");
        }
        categoryModuleMapper.deleteById(categoryId);
    }

    @Override
    public void updateCategoryStatus(Long operatorId, String operatorRole, Long categoryId, Integer status) {
        if (!"Admin".equals(operatorRole)) {
            throw new IllegalArgumentException("仅管理员可修改分类状态");
        }
        CategoryEntity entity = new CategoryEntity();
        entity.setCategoryId(categoryId);
        entity.setStatus(status);
        entity.setUpdateTime(java.time.LocalDateTime.now());
        categoryModuleMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppProductVo createProduct(Long operatorId, String operatorRole, AppProductDto productDto) {
        requireNotBlank(productDto.getName(), "商品名称不能为空");
        if (productDto.getShopId() == null) {
            throw new IllegalArgumentException("店铺ID不能为空");
        }
        assertShopManageable(operatorId, operatorRole, productDto.getShopId());
        AppProductEntity product = new AppProductEntity();
        product.setId(idWorker.nextId());
        applyDto(product, productDto);
        product.setSales(0);
        product.setLikes(0);
        product.setStatus(productDto.getStatus() == null ? 0 : productDto.getStatus());
        product.setAuditStatus(productDto.getAuditStatus() != null && !productDto.getAuditStatus().trim().isEmpty() ? productDto.getAuditStatus() : "Draft");
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        save(product);
        return toVo(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppProductVo updateProduct(Long operatorId, String operatorRole, Long productId, AppProductDto productDto) {
        AppProductEntity product = getById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        assertProductManageable(operatorId, operatorRole, product);
        applyDto(product, productDto);
        product.setId(productId);
        product.setUpdatedAt(LocalDateTime.now());
        updateById(product);
        return toVo(product);
    }

    @Override
    public void updateProductStatus(Long operatorId, String operatorRole, Long productId, Integer status) {
        if (status == null || (status != 0 && status != 1 && status != 2)) {
            throw new IllegalArgumentException("商品状态仅支持 0下架 1上架 2删除");
        }
        AppProductEntity product = getById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        assertProductManageable(operatorId, operatorRole, product);
        AppProductEntity update = new AppProductEntity();
        update.setId(productId);
        update.setStatus(status);
        update.setUpdatedAt(LocalDateTime.now());
        updateById(update);
    }

    @Override
    public void deleteProduct(Long operatorId, String operatorRole, Long productId) {
        updateProductStatus(operatorId, operatorRole, productId, 2);
    }

    @Override
    public Map<String, Object> listAdminProducts(Long operatorId, String operatorRole, AppProductQueryDto queryDto) {
        AppProductQueryDto query = queryDto == null ? new AppProductQueryDto() : queryDto;
        if (!"Admin".equals(operatorRole)) {
            if (query.getShopId() == null) {
                query.setShopId(resolveOwnerShopId(operatorId));
            }
            assertShopManageable(operatorId, operatorRole, query.getShopId());
        }
        int pageNum = validPageNum(query.getPageNum());
        int pageSize = validPageSize(query.getPageSize());
        QueryWrapper<AppProductEntity> wrapper = buildProductQuery(query);
        wrapper.ne("status", 2);
        applySortExt(wrapper, query);
        if (isBlank(query.getSortField()) && isBlank(query.getSortBy())) {
            wrapper.orderByDesc("updated_at");
        }
        Page<AppProductEntity> page = page(new Page<>(pageNum, pageSize), wrapper);
        return pageResult(page, pageNum, pageSize);
    }

    @Override
    public AppProductVo getProductDetailAdmin(Long operatorId, String operatorRole, Long productId) {
        AppProductEntity product = getById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        assertProductManageable(operatorId, operatorRole, product);
        return toVo(product);
    }

    @Override
    public void submitProductReview(Long operatorId, String operatorRole, Long productId) {
        AppProductEntity product = getById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        assertProductManageable(operatorId, operatorRole, product);
        AppProductEntity update = new AppProductEntity();
        update.setId(productId);
        update.setAuditStatus("Pending");
        update.setAuditReason(null);
        update.setUpdatedAt(LocalDateTime.now());
        updateById(update);
    }

    @Override
    public void reviewProduct(Long reviewerId, ProductReviewDto reviewDto) {
        if (reviewDto == null || reviewDto.getProductId() == null) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        String status = normalizeAuditStatus(reviewDto.getAuditStatus());
        AppProductEntity product = getById(reviewDto.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        AppProductEntity update = new AppProductEntity();
        update.setId(reviewDto.getProductId());
        update.setAuditStatus(status);
        update.setAuditReason(reviewDto.getAuditReason());
        update.setReviewedBy(reviewerId);
        update.setReviewedAt(LocalDateTime.now());
        update.setUpdatedAt(LocalDateTime.now());
        if ("Rejected".equals(status)) {
            update.setStatus(0);
        }
        updateById(update);
    }

    @Override
    public Map<String, Object> listComments(Long productId, Integer pageNum, Integer pageSize) {
        QueryWrapper<ProductCommentEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId).eq("status", 1).orderByDesc("created_at");
        int current = validPageNum(pageNum);
        int size = validPageSize(pageSize);
        Page<ProductCommentEntity> page = productCommentMapper.selectPage(new Page<>(current, size), wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords().stream().map(this::toCommentVo).collect(Collectors.toList()));
        result.put("total", page.getTotal());
        result.put("pageNum", current);
        result.put("pageSize", size);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductCommentVo addComment(Long userId, ProductCommentDto commentDto) {
        if (commentDto == null || commentDto.getProductId() == null) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        if (commentDto.getRating() == null || commentDto.getRating() < 1 || commentDto.getRating() > 5) {
            throw new IllegalArgumentException("评分必须在1到5之间");
        }
        if (getById(commentDto.getProductId()) == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        OrderItemEntity orderItem = requireReviewableOrderItem(userId, commentDto);
        ProductCommentEntity comment = new ProductCommentEntity();
        comment.setId(idWorker.nextId());
        comment.setProductId(commentDto.getProductId());
        comment.setUserId(userId);
        comment.setOrderId(commentDto.getOrderId());
        comment.setRating(commentDto.getRating());
        comment.setContent(commentDto.getContent());
        comment.setImages(commentDto.getImages());
        comment.setStatus(1);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        productCommentMapper.insert(comment);
        orderItem.setReviewStatus("Reviewed");
        orderItemMapper.updateById(orderItem);
        completeOrderIfFullyReviewed(commentDto.getOrderId());
        updateProductRating(commentDto.getProductId());
        return toCommentVo(comment);
    }

    private OrderItemEntity requireReviewableOrderItem(Long userId, ProductCommentDto commentDto) {
        if (commentDto.getOrderId() == null || commentDto.getOrderItemId() == null) {
            throw new IllegalArgumentException("评论必须关联已收货订单商品");
        }
        OrderEntity order = orderModuleMapper.selectById(commentDto.getOrderId());
        if (order == null || !userId.equals(order.getCustomerId()) || !("PendingReview".equals(order.getOrderStatus()) || "Completed".equals(order.getOrderStatus()))) {
            throw new IllegalArgumentException("仅已收货订单可评价");
        }
        OrderItemEntity orderItem = orderItemMapper.selectById(commentDto.getOrderItemId());
        if (orderItem == null || !order.getOrderId().equals(orderItem.getOrderId()) || !commentDto.getProductId().equals(orderItem.getProductId())) {
            throw new IllegalArgumentException("订单商品不匹配");
        }
        if ("Reviewed".equals(orderItem.getReviewStatus())) {
            throw new IllegalArgumentException("该商品已评价");
        }
        return orderItem;
    }

    private void completeOrderIfFullyReviewed(Long orderId) {
        if (orderId == null) return;
        QueryWrapper<OrderItemEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId);
        List<OrderItemEntity> items = orderItemMapper.selectList(wrapper);
        if (items.isEmpty()) return;
        boolean allReviewed = items.stream().allMatch(item -> "Reviewed".equals(item.getReviewStatus()));
        if (!allReviewed) return;
        OrderEntity order = orderModuleMapper.selectById(orderId);
        if (order == null || !"PendingReview".equals(order.getOrderStatus())) return;
        order.setOrderStatus("Completed");
        order.setUpdateTime(LocalDateTime.now());
        orderModuleMapper.updateById(order);
    }

    @Override
    public void deleteComment(Long operatorId, String operatorRole, Long commentId) {
        ProductCommentEntity comment = productCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        if (!"Admin".equals(operatorRole) && !operatorId.equals(comment.getUserId())) {
            throw new IllegalArgumentException("无权删除该评论");
        }
        comment.setStatus(0);
        comment.setUpdatedAt(LocalDateTime.now());
        productCommentMapper.updateById(comment);
        updateProductRating(comment.getProductId());
    }

    @Override
    public Map<String, Object> listSpecTemplates(Long operatorId, String operatorRole, ProductSpecTemplateDto queryDto) {
        ProductSpecTemplateDto query = queryDto == null ? new ProductSpecTemplateDto() : queryDto;
        QueryWrapper<ProductSpecTemplateEntity> wrapper = new QueryWrapper<>();
        if (query.getCategoryId() != null) {
            wrapper.eq("category_id", query.getCategoryId());
        }
        if (query.getShopId() != null) {
            wrapper.eq("shop_id", query.getShopId());
        }
        if (!"Admin".equals(operatorRole)) {
            Long shopId = query.getShopId() == null ? resolveOwnerShopId(operatorId) : query.getShopId();
            assertShopManageable(operatorId, operatorRole, shopId);
            wrapper.and(item -> item.eq("shop_id", shopId).or().isNull("shop_id"));
        }
        wrapper.ne("status", 0).orderByDesc("updated_at");
        Map<String, Object> result = new HashMap<>();
        result.put("records", productSpecTemplateMapper.selectList(wrapper).stream().map(this::toSpecTemplateDto).collect(Collectors.toList()));
        return result;
    }

    @Override
    public ProductSpecTemplateDto saveSpecTemplate(Long operatorId, String operatorRole, ProductSpecTemplateDto templateDto) {
        requireNotBlank(templateDto.getName(), "模板名称不能为空");
        if (!"Admin".equals(operatorRole)) {
            Long shopId = templateDto.getShopId() == null ? resolveOwnerShopId(operatorId) : templateDto.getShopId();
            assertShopManageable(operatorId, operatorRole, shopId);
            templateDto.setShopId(shopId);
        }
        ProductSpecTemplateEntity template = templateDto.getId() == null ? new ProductSpecTemplateEntity() : productSpecTemplateMapper.selectById(templateDto.getId());
        if (template == null) {
            throw new IllegalArgumentException("规格模板不存在");
        }
        if (template.getId() == null) {
            template.setId(idWorker.nextId());
            template.setCreatedAt(LocalDateTime.now());
        }
        template.setName(templateDto.getName());
        template.setCategoryId(templateDto.getCategoryId());
        template.setShopId(templateDto.getShopId());
        template.setSpecs(templateDto.getSpecs());
        template.setStatus(templateDto.getStatus() == null ? 1 : templateDto.getStatus());
        template.setUpdatedAt(LocalDateTime.now());
        if (templateDto.getId() == null) {
            productSpecTemplateMapper.insert(template);
        } else {
            productSpecTemplateMapper.updateById(template);
        }
        return toSpecTemplateDto(template);
    }

    @Override
    public void deleteSpecTemplate(Long operatorId, String operatorRole, Long templateId) {
        ProductSpecTemplateEntity template = productSpecTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("规格模板不存在");
        }
        if (!"Admin".equals(operatorRole)) {
            assertShopManageable(operatorId, operatorRole, template.getShopId());
        }
        template.setStatus(0);
        template.setUpdatedAt(LocalDateTime.now());
        productSpecTemplateMapper.updateById(template);
    }

    private QueryWrapper<AppProductEntity> buildProductQuery(AppProductQueryDto query) {
        QueryWrapper<AppProductEntity> wrapper = new QueryWrapper<>();
        if (query.getCategoryId() != null) {
            wrapper.eq("category_id", query.getCategoryId());
        }
        if (query.getShopId() != null) {
            wrapper.eq("shop_id", query.getShopId());
        }
        if (query.getBrandShopId() != null) {
            wrapper.eq("brand_shop_id", query.getBrandShopId());
        }
        if (query.getStatus() != null) {
            wrapper.eq("status", query.getStatus());
        }
        if (!isBlank(query.getAuditStatus())) {
            wrapper.eq("audit_status", query.getAuditStatus());
        }
        if (!isBlank(query.getKeyword())) {
            wrapper.and(item -> item.like("name", query.getKeyword()).or().like("subtitle", query.getKeyword()));
        }
        // 价格区间
        if (query.getPriceMin() != null) {
            wrapper.ge("price", query.getPriceMin());
        }
        if (query.getPriceMax() != null) {
            wrapper.le("price", query.getPriceMax());
        }
        // 库存预警
        if (!isBlank(query.getStockAlert())) {
            switch (query.getStockAlert()) {
                case "zero":
                    wrapper.eq("stock", 0);
                    break;
                case "low":
                    wrapper.gt("stock", 0).le("stock", 10);
                    break;
                case "normal":
                    wrapper.gt("stock", 10);
                    break;
                default:
                    break;
            }
        }
        return wrapper;
    }

    private void applySort(QueryWrapper<AppProductEntity> wrapper, String sortBy) {
        if ("priceAsc".equals(sortBy)) {
            wrapper.orderByAsc("price");
        } else if ("priceDesc".equals(sortBy)) {
            wrapper.orderByDesc("price");
        } else if ("sales".equals(sortBy)) {
            wrapper.orderByDesc("sales");
        } else {
            wrapper.orderByDesc("sales").orderByDesc("created_at");
        }
    }

    /** 支持 sortField + sortOrder 组合排序，兼容旧版 sortBy */
    private void applySortExt(QueryWrapper<AppProductEntity> wrapper, AppProductQueryDto query) {
        String field = query.getSortField();
        String order = query.getSortOrder();
        if (!isBlank(field)) {
            String col;
            switch (field) {
                case "price":     col = "price"; break;
                case "sales":     col = "sales"; break;
                case "stock":     col = "stock"; break;
                case "createTime":col = "created_at"; break;
                default:          col = "sales"; break;
            }
            if ("asc".equalsIgnoreCase(order)) {
                wrapper.orderByAsc(col);
            } else {
                wrapper.orderByDesc(col);
            }
        } else {
            applySort(wrapper, query.getSortBy());
        }
    }

    private void applyDto(AppProductEntity product, AppProductDto dto) {
        if (dto.getName() != null)          product.setName(dto.getName());
        if (dto.getProductCode() != null)   product.setProductCode(dto.getProductCode());
        if (dto.getSubtitle() != null)      product.setSubtitle(dto.getSubtitle());
        if (dto.getCategoryId() != null)    product.setCategoryId(dto.getCategoryId());
        if (dto.getShopId() != null)        product.setShopId(dto.getShopId());
        if (dto.getBrandShopId() != null)   product.setBrandShopId(dto.getBrandShopId());
        if (dto.getPrice() != null)         product.setPrice(dto.getPrice());
        if (dto.getOriginalPrice() != null) product.setOriginalPrice(dto.getOriginalPrice());
        if (dto.getStock() != null)         product.setStock(dto.getStock());
        if (dto.getWeight() != null)        product.setWeight(dto.getWeight());
        if (dto.getShippingType() != null)  product.setShippingType(dto.getShippingType());
        if (dto.getShippingFee() != null)   product.setShippingFee(dto.getShippingFee());
        if (dto.getShipFrom() != null)      product.setShipFrom(dto.getShipFrom());
        if (dto.getWarehouseId() != null)   product.setWarehouseId(dto.getWarehouseId());
        if (dto.getShippingDays() != null)  product.setShippingDays(dto.getShippingDays());
        if (dto.getPurchaseLimit() != null) product.setPurchaseLimit(dto.getPurchaseLimit());
        if (dto.getMoq() != null)           product.setMoq(dto.getMoq());
        if (dto.getVideoUrl() != null)      product.setVideoUrl(dto.getVideoUrl());
        // 将前端回传的预签名 URL 统一归一化为 objectKey 再落库，防止时效性过期
        if (dto.getMainImage() != null)     product.setMainImage(aliOSSUtils.normalizeForStorage(dto.getMainImage()));
        if (dto.getImages() != null)        product.setImages(normalizeImageList(dto.getImages()));
        if (dto.getTag() != null)           product.setTag(dto.getTag());
        if (dto.getTagColor() != null)      product.setTagColor(dto.getTagColor());
        if (dto.getTagBg() != null)         product.setTagBg(dto.getTagBg());
        if (dto.getDescription() != null)   product.setDescription(dto.getDescription());
        if (dto.getFeatures() != null)      product.setFeatures(dto.getFeatures());
        if (dto.getSpecs() != null)         product.setSpecs(dto.getSpecs());
        if (dto.getStatus() != null)        product.setStatus(dto.getStatus());
        if (dto.getAuditStatus() != null)   product.setAuditStatus(dto.getAuditStatus());
    }

    private void assertProductManageable(Long operatorId, String operatorRole, AppProductEntity product) {
        assertShopManageable(operatorId, operatorRole, product.getShopId());
    }

    private void assertShopManageable(Long operatorId, String operatorRole, Long shopId) {
        if ("Admin".equals(operatorRole)) {
            return;
        }
        if (shopId == null) {
            throw new IllegalArgumentException("店铺ID不能为空");
        }
        ShopEntity shop = shopModuleMapper.selectById(shopId);
        if (shop == null) {
            throw new IllegalArgumentException("店铺不存在");
        }
        if (!operatorId.equals(shop.getOwnerUserId())) {
            throw new IllegalArgumentException("无权操作其他店铺商品");
        }
        if (!"Active".equals(shop.getStatus())) {
            throw new IllegalArgumentException("店铺未通过审核，不能发布商品");
        }
    }

    private Long resolveOwnerShopId(Long operatorId) {
        QueryWrapper<ShopEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("owner_user_id", operatorId).eq("status", "Active").last("LIMIT 1");
        ShopEntity shop = shopModuleMapper.selectOne(wrapper);
        if (shop == null) {
            throw new IllegalArgumentException("当前用户没有可管理店铺");
        }
        return shop.getShopId();
    }

    private AppProductVo toVo(AppProductEntity product) {
        AppProductVo vo = new AppProductVo();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setProductCode(product.getProductCode());
        vo.setSubtitle(product.getSubtitle());
        vo.setCategoryId(product.getCategoryId());
        vo.setShopId(product.getShopId());
        vo.setBrandShopId(product.getBrandShopId());
        vo.setPrice(product.getPrice());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setStock(product.getStock());
        vo.setWeight(product.getWeight());
        vo.setShippingType(product.getShippingType());
        vo.setShippingFee(product.getShippingFee());
        vo.setShipFrom(product.getShipFrom());
        vo.setWarehouseId(product.getWarehouseId());
        vo.setShippingDays(product.getShippingDays());
        vo.setPurchaseLimit(product.getPurchaseLimit());
        vo.setMoq(product.getMoq());
        vo.setVideoUrl(product.getVideoUrl());
        vo.setSales(product.getSales());
        vo.setRating(product.getRating());
        vo.setLikes(product.getLikes());
        // 若存储的是对象键（无 :// 前缀），按需生成签名 URL
        vo.setMainImage(resolveUrl(product.getMainImage()));
        vo.setImages(resolveImages(product.getImages()));
        vo.setTag(product.getTag());
        vo.setTagColor(product.getTagColor());
        vo.setTagBg(product.getTagBg());
        vo.setDescription(product.getDescription());
        vo.setFeatures(product.getFeatures());
        vo.setSpecs(product.getSpecs());
        vo.setStatus(product.getStatus());
        vo.setAuditStatus(product.getAuditStatus());
        vo.setAuditReason(product.getAuditReason());
        vo.setAvailableStock(product.getStock() == null ? 0 : product.getStock());
        vo.setFavorited(false);
        fillShopSummary(vo, product.getShopId());
        return vo;
    }

    /**
     * 若 value 是 OSS 对象键（无 :// 前缀），生成 2 小时签名 URL；否则原样返回
     */
    private String resolveUrl(String value) {
        if (value == null || value.isBlank()) return value;
        if (AliOSSUtils.isObjectKey(value)) {
            String signed = aliOSSUtils.generatePresignedUrl(value, 120);
            return signed != null ? signed : value;
        }
        return value;
    }

    /**
     * 将前端回传的逗号分隔图片列表中的每项预签名 URL 归一化为 objectKey（落库前调用）
     */
    private String normalizeImageList(String images) {
        if (images == null || images.isBlank()) return images;
        String[] parts = images.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(",");
            String normalized = aliOSSUtils.normalizeForStorage(parts[i].trim());
            sb.append(normalized != null ? normalized : parts[i].trim());
        }
        return sb.toString();
    }

    @Override
    public Object fixProductImageKeys() {
        // 查出所有商品（不受审核/上下架状态限制）
        List<AppProductEntity> all = list();
        int fixed = 0;
        for (AppProductEntity p : all) {
            boolean changed = false;

            String newMain = aliOSSUtils.normalizeForStorage(p.getMainImage());
            if (newMain != null && !newMain.equals(p.getMainImage())) {
                p.setMainImage(newMain);
                changed = true;
            }

            String newImages = normalizeImageList(p.getImages());
            if (newImages != null && !newImages.equals(p.getImages())) {
                p.setImages(newImages);
                changed = true;
            }

            if (changed) {
                p.setUpdatedAt(LocalDateTime.now());
                updateById(p);
                fixed++;
            }
        }
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("total", all.size());
        result.put("fixed", fixed);
        result.put("msg", "已将 " + fixed + " 件商品的图片 URL 归一化为 objectKey");
        return result;
    }

    /**
     * 对逗号分隔的图片字符串（每项可能是对象键或完整 URL）逐项解析
     */
    private String resolveImages(String images) {
        if (images == null || images.isBlank()) return images;
        String[] parts = images.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(resolveUrl(parts[i].trim()));
        }
        return sb.toString();
    }

    private Map<String, Object> channelProducts(RecommendationQueryDto query) {
        if (query.getChannelId() == null) {
            return listPublicProducts(toProductQuery(query));
        }
        ChannelEntity channel = channelMapper.selectById(query.getChannelId());
        AppProductQueryDto productQuery = toProductQuery(query);
        if (channel != null && channel.getPath() != null && channel.getPath().startsWith("category:")) {
            productQuery.setCategoryId(Long.valueOf(channel.getPath().substring("category:".length())));
        }
        return listPublicProducts(productQuery);
    }

    private Map<String, Object> activityProducts(RecommendationQueryDto query) {
        AppProductQueryDto productQuery = toProductQuery(query);
        if (query.getActivityId() == null) {
            productQuery.setSortBy("sales");
            return listPublicProducts(productQuery);
        }
        ActivityEntity activity = activityMapper.selectById(query.getActivityId());
        QueryWrapper<AppProductEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).eq("audit_status", "Approved");
        if (activity != null) {
            if ("Shop".equalsIgnoreCase(activity.getScopeType()) && activity.getShopId() != null) {
                wrapper.eq("shop_id", activity.getShopId());
            } else if ("Product".equalsIgnoreCase(activity.getScopeType()) && !isBlank(activity.getProductIds())) {
                List<Long> ids = parseIds(activity.getProductIds());
                if (!ids.isEmpty()) {
                    wrapper.in("id", ids);
                }
            }
        }
        wrapper.orderByDesc("sales").orderByDesc("created_at");
        int pageNum = validPageNum(query.getPageNum());
        int pageSize = validPageSize(query.getPageSize());
        Page<AppProductEntity> page = page(new Page<>(pageNum, pageSize), wrapper);
        return pageResult(page, pageNum, pageSize);
    }

    private Map<String, Object> personalizedProducts(Long userId, RecommendationQueryDto query) {
        AppProductQueryDto productQuery = toProductQuery(query);
        if (userId == null) {
            productQuery.setSortBy("sales");
            return listPublicProducts(productQuery);
        }
        QueryWrapper<AppProductEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).eq("audit_status", "Approved");
        List<Long> categoryIds = recentInterestCategoryIds(userId);
        if (!categoryIds.isEmpty()) {
            wrapper.in("category_id", categoryIds);
        }
        wrapper.orderByDesc("sales").orderByDesc("rating").orderByDesc("created_at");
        int pageNum = validPageNum(query.getPageNum());
        int pageSize = validPageSize(query.getPageSize());
        Page<AppProductEntity> page = page(new Page<>(pageNum, pageSize), wrapper);
        if (page.getRecords().isEmpty() && !categoryIds.isEmpty()) {
            productQuery.setSortBy("sales");
            return listPublicProducts(productQuery);
        }
        return pageResult(page, pageNum, pageSize);
    }

    private List<Long> recentInterestCategoryIds(Long userId) {
        List<Long> productIds = new ArrayList<>();
        QueryWrapper<FootprintEntity> footprintWrapper = new QueryWrapper<>();
        footprintWrapper.eq("user_id", userId).eq("target_type", "Product").orderByDesc("view_at").last("LIMIT 20");
        productIds.addAll(footprintMapper.selectList(footprintWrapper).stream().map(FootprintEntity::getTargetId).collect(Collectors.toList()));
        QueryWrapper<FavoriteEntity> favoriteWrapper = new QueryWrapper<>();
        favoriteWrapper.eq("user_id", userId).eq("target_type", "Product").orderByDesc("created_at").last("LIMIT 20");
        productIds.addAll(favoriteMapper.selectList(favoriteWrapper).stream().map(FavoriteEntity::getTargetId).collect(Collectors.toList()));
        if (productIds.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<AppProductEntity> wrapper = new QueryWrapper<>();
        wrapper.in("id", productIds);
        return list(wrapper).stream().map(AppProductEntity::getCategoryId).filter(value -> value != null).distinct().collect(Collectors.toList());
    }

    private AppProductQueryDto toProductQuery(RecommendationQueryDto query) {
        AppProductQueryDto productQuery = new AppProductQueryDto();
        productQuery.setPageNum(query.getPageNum());
        productQuery.setPageSize(query.getPageSize());
        productQuery.setSortBy("sales");
        return productQuery;
    }

    private List<Long> parseIds(String value) {
        List<Long> ids = new ArrayList<>();
        if (isBlank(value)) {
            return ids;
        }
        for (String item : value.split(",")) {
            try {
                ids.add(Long.valueOf(item.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private void fillProductAggregates(AppProductVo vo, AppProductEntity product, Long userId) {
        QueryWrapper<ProductCommentEntity> commentWrapper = new QueryWrapper<>();
        commentWrapper.eq("product_id", product.getId()).eq("status", 1).orderByDesc("created_at").last("LIMIT 5");
        List<ProductCommentVo> comments = productCommentMapper.selectList(commentWrapper).stream()
                .map(this::toCommentVo)
                .collect(Collectors.toList());
        QueryWrapper<ProductCommentEntity> countWrapper = new QueryWrapper<>();
        countWrapper.eq("product_id", product.getId()).eq("status", 1);
        vo.setCommentCount(productCommentMapper.selectCount(countWrapper).intValue());
        vo.setComments(comments);
        Map<String, Object> promotions = new HashMap<>();
        if (!isBlank(product.getTag())) {
            promotions.put("tag", product.getTag());
            promotions.put("tagColor", product.getTagColor());
            promotions.put("tagBg", product.getTagBg());
        }
        PromotionCalculateDto calculateDto = new PromotionCalculateDto();
        calculateDto.setUserId(userId);
        PromotionItemDto itemDto = new PromotionItemDto();
        itemDto.setProductId(product.getId());
        itemDto.setShopId(product.getShopId());
        itemDto.setPrice(product.getPrice());
        itemDto.setQuantity(1);
        calculateDto.getItems().add(itemDto);
        promotions.put("calculation", promotionCalculationService.calculate(calculateDto));
        vo.setPromotions(promotions);
    }

    private void updateProductRating(Long productId) {
        QueryWrapper<ProductCommentEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId).eq("status", 1);
        List<ProductCommentEntity> comments = productCommentMapper.selectList(wrapper);
        AppProductEntity update = new AppProductEntity();
        update.setId(productId);
        if (comments.isEmpty()) {
            update.setRating(BigDecimal.ZERO);
        } else {
            int total = comments.stream().map(ProductCommentEntity::getRating).filter(value -> value != null).reduce(0, Integer::sum);
            update.setRating(BigDecimal.valueOf(total).divide(BigDecimal.valueOf(comments.size()), 2, RoundingMode.HALF_UP));
        }
        update.setUpdatedAt(LocalDateTime.now());
        updateById(update);
    }

    private ProductCommentVo toCommentVo(ProductCommentEntity comment) {
        ProductCommentVo vo = new ProductCommentVo();
        vo.setId(comment.getId());
        vo.setProductId(comment.getProductId());
        vo.setUserId(comment.getUserId());
        vo.setOrderId(comment.getOrderId());
        vo.setRating(comment.getRating());
        vo.setContent(comment.getContent());
        vo.setImages(comment.getImages());
        vo.setCreatedAt(comment.getCreatedAt() == null ? null : comment.getCreatedAt().toString());
        return vo;
    }

    private ProductSpecTemplateDto toSpecTemplateDto(ProductSpecTemplateEntity template) {
        ProductSpecTemplateDto dto = new ProductSpecTemplateDto();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setCategoryId(template.getCategoryId());
        dto.setShopId(template.getShopId());
        dto.setSpecs(template.getSpecs());
        dto.setStatus(template.getStatus());
        return dto;
    }

    private String normalizeAuditStatus(String status) {
        if ("Approved".equalsIgnoreCase(status) || "通过".equals(status)) {
            return "Approved";
        }
        if ("Rejected".equalsIgnoreCase(status) || "拒绝".equals(status)) {
            return "Rejected";
        }
        throw new IllegalArgumentException("审核状态仅支持 Approved/Rejected");
    }

    private void fillShopSummary(AppProductVo vo, Long shopId) {
        if (shopId == null) {
            return;
        }
        ShopEntity shop = shopModuleMapper.selectById(shopId);
        if (shop == null) {
            return;
        }
        vo.setShopName(shop.getDisplayName() == null ? shop.getShopName() : shop.getDisplayName());
        vo.setShopLogo(shop.getLogo() == null ? shop.getAvatar() : shop.getLogo());
    }

    private CategoryVo toCategoryVo(CategoryEntity category) {
        CategoryVo vo = new CategoryVo();
        vo.setCategoryId(category.getCategoryId());
        vo.setCategoryName(category.getCategoryName());
        vo.setParentCategoryId(category.getParentCategoryId());
        vo.setDescription(category.getDescription());
        vo.setImageUrl(category.getImageUrl());
        vo.setIcon(category.getIcon());
        vo.setBanner(category.getBanner());
        vo.setLevel(category.getLevel());
        vo.setProductCount(category.getProductCount());
        vo.setSort(category.getSort());
        vo.setStatus(category.getStatus());
        return vo;
    }

    private Map<String, Object> pageResult(Page<AppProductEntity> page, int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        result.put("total", page.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    private int validPageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int validPageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
    }

    private void requireNotBlank(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
