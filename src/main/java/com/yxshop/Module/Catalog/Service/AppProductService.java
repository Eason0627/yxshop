package com.yxshop.Module.Catalog.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxshop.Module.Catalog.Dto.AppProductDto;
import com.yxshop.Module.Catalog.Dto.AppProductQueryDto;
import com.yxshop.Module.Catalog.Dto.CategoryDto;
import com.yxshop.Module.Catalog.Dto.ProductCommentDto;
import com.yxshop.Module.Catalog.Dto.ProductReviewDto;
import com.yxshop.Module.Catalog.Dto.ProductSpecTemplateDto;
import com.yxshop.Module.Catalog.Dto.RecommendationQueryDto;
import com.yxshop.Module.Catalog.Entity.AppProductEntity;
import com.yxshop.Module.Catalog.Vo.AppProductVo;
import com.yxshop.Module.Catalog.Vo.CategoryVo;
import com.yxshop.Module.Catalog.Vo.ProductCommentVo;

import java.util.List;
import java.util.Map;

public interface AppProductService extends IService<AppProductEntity> {
    Map<String, Object> listPublicProducts(AppProductQueryDto queryDto);

    AppProductVo getProductDetail(Long productId);

    AppProductVo getProductDetail(Long userId, Long productId);

    Map<String, Object> recommendationProducts(Long userId, RecommendationQueryDto queryDto);

    List<CategoryVo> listCategoryTree();

    List<CategoryVo> listAllCategoryTree();  // 管理端：包含禁用分类

    CategoryVo saveCategory(Long operatorId, String operatorRole, CategoryDto dto);

    void deleteCategoryAdmin(Long operatorId, String operatorRole, Long categoryId);

    void updateCategoryStatus(Long operatorId, String operatorRole, Long categoryId, Integer status);

    AppProductVo createProduct(Long operatorId, String operatorRole, AppProductDto productDto);

    AppProductVo updateProduct(Long operatorId, String operatorRole, Long productId, AppProductDto productDto);

    void updateProductStatus(Long operatorId, String operatorRole, Long productId, Integer status);

    void deleteProduct(Long operatorId, String operatorRole, Long productId);

    void submitProductReview(Long operatorId, String operatorRole, Long productId);

    void reviewProduct(Long reviewerId, ProductReviewDto reviewDto);

    Map<String, Object> listAdminProducts(Long operatorId, String operatorRole, AppProductQueryDto queryDto);

    AppProductVo getProductDetailAdmin(Long operatorId, String operatorRole, Long productId);

    Map<String, Object> listComments(Long productId, Integer pageNum, Integer pageSize);

    ProductCommentVo addComment(Long userId, ProductCommentDto commentDto);

    void deleteComment(Long operatorId, String operatorRole, Long commentId);

    Map<String, Object> listSpecTemplates(Long operatorId, String operatorRole, ProductSpecTemplateDto queryDto);

    ProductSpecTemplateDto saveSpecTemplate(Long operatorId, String operatorRole, ProductSpecTemplateDto templateDto);

    void deleteSpecTemplate(Long operatorId, String operatorRole, Long templateId);

    /** 批量将商品图片字段中的预签名 URL 转为 objectKey（修复历史脏数据） */
    Object fixProductImageKeys();
}
