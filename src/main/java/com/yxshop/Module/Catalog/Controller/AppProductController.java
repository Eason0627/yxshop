package com.yxshop.Module.Catalog.Controller;

import com.yxshop.Module.Catalog.Dto.AppProductDto;
import com.yxshop.Module.Catalog.Dto.AppProductQueryDto;
import com.yxshop.Module.Catalog.Dto.CategoryDto;
import com.yxshop.Module.Catalog.Dto.ProductCommentDto;
import com.yxshop.Module.Catalog.Dto.ProductReviewDto;
import com.yxshop.Module.Catalog.Dto.ProductSpecTemplateDto;
import com.yxshop.Module.Catalog.Dto.RecommendationQueryDto;
import com.yxshop.Module.Catalog.Service.AppProductService;
import com.yxshop.Utils.JwtUtils;
import com.yxshop.Utils.Result;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/app/products")
@Tag(name = "Catalog", description = "商品目录、分类、商品详情和商品管理接口")
public class AppProductController {

    private final AppProductService appProductService;

    public AppProductController(AppProductService appProductService) {
        this.appProductService = appProductService;
    }

    @PostMapping("/feed")
    @Operation(summary = "App商品流")
    public Result feed(@RequestBody(required = false) AppProductQueryDto queryDto) {
        return Result.success(appProductService.listPublicProducts(queryDto));
    }

    @GetMapping("/detail/{productId}")
    @Operation(summary = "App商品详情")
    public Result detail(HttpServletRequest request, @PathVariable Long productId) {
        Long userId = optionalUserId(request);
        if (userId == null) {
            return Result.success(appProductService.getProductDetail(productId));
        }
        return Result.success(appProductService.getProductDetail(userId, productId));
    }

    @PostMapping("/recommendations")
    @Operation(summary = "推荐/频道/活动商品池")
    public Result recommendations(HttpServletRequest request, @RequestBody(required = false) RecommendationQueryDto queryDto) {
        return Result.success(appProductService.recommendationProducts(optionalUserId(request), queryDto));
    }

    @GetMapping("/categories")
    @Operation(summary = "分类树（仅启用）")
    public Result categories() {
        return Result.success(appProductService.listCategoryTree());
    }

    @GetMapping("/admin/categories")
    @Operation(summary = "后台分类树（含禁用）")
    public Result adminCategories(HttpServletRequest request) {
        currentUserId(request); // 要求登录
        return Result.success(appProductService.listAllCategoryTree());
    }

    @PostMapping("/admin/categories/save")
    @Operation(summary = "新增或修改分类（Admin）")
    public Result saveCategory(HttpServletRequest request, @RequestBody CategoryDto dto) {
        return Result.success(appProductService.saveCategory(currentUserId(request), currentUserRole(request), dto));
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    @Operation(summary = "删除分类（Admin）")
    public Result deleteCategory(HttpServletRequest request, @PathVariable Long categoryId) {
        appProductService.deleteCategoryAdmin(currentUserId(request), currentUserRole(request), categoryId);
        return Result.success("分类已删除");
    }

    @PutMapping("/admin/categories/{categoryId}/status")
    @Operation(summary = "修改分类状态（Admin）")
    public Result updateCategoryStatus(HttpServletRequest request, @PathVariable Long categoryId,
                                       @RequestBody java.util.Map<String, Object> body) {
        Integer status = Integer.valueOf(String.valueOf(body.get("status")));
        appProductService.updateCategoryStatus(currentUserId(request), currentUserRole(request), categoryId, status);
        return Result.success("状态已更新");
    }

    @GetMapping("/{productId}/comments")
    @Operation(summary = "商品评论列表")
    public Result comments(@PathVariable Long productId,
                           @RequestParam(required = false) Integer pageNum,
                           @RequestParam(required = false) Integer pageSize) {
        return Result.success(appProductService.listComments(productId, pageNum, pageSize));
    }

    @PostMapping("/comments")
    @Operation(summary = "发布商品评论")
    public Result addComment(HttpServletRequest request, @RequestBody ProductCommentDto commentDto) {
        return Result.success(appProductService.addComment(currentUserId(request), commentDto));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "删除商品评论")
    public Result deleteComment(HttpServletRequest request, @PathVariable Long commentId) {
        appProductService.deleteComment(currentUserId(request), currentUserRole(request), commentId);
        return Result.success("评论已删除");
    }

    @PostMapping("/admin/list")
    @Operation(summary = "后台商品分页列表")
    public Result adminList(HttpServletRequest request, @RequestBody(required = false) AppProductQueryDto queryDto) {
        return Result.success(appProductService.listAdminProducts(currentUserId(request), currentUserRole(request), queryDto));
    }

    @GetMapping("/admin/detail/{productId}")
    @Operation(summary = "后台商品详情（不限状态/审核）")
    public Result adminDetail(HttpServletRequest request, @PathVariable Long productId) {
        return Result.success(appProductService.getProductDetailAdmin(currentUserId(request), currentUserRole(request), productId));
    }

    @PostMapping
    @Operation(summary = "发布商品")
    public Result create(HttpServletRequest request, @RequestBody AppProductDto productDto) {
        return Result.success(appProductService.createProduct(currentUserId(request), currentUserRole(request), productDto));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "修改商品")
    public Result update(HttpServletRequest request, @PathVariable Long productId, @RequestBody AppProductDto productDto) {
        return Result.success(appProductService.updateProduct(currentUserId(request), currentUserRole(request), productId, productDto));
    }

    @PutMapping("/{productId}/status")
    @Operation(summary = "修改商品状态（支持 JSON body 或 query param）")
    public Result status(HttpServletRequest request, @PathVariable Long productId,
                         @RequestParam(required = false) Integer status,
                         @RequestBody(required = false) java.util.Map<String, Object> body) {
        Integer finalStatus = status;
        if (finalStatus == null && body != null) {
            Object v = body.get("status");
            if (v != null) {
                finalStatus = Integer.valueOf(String.valueOf(v));
            }
        }
        appProductService.updateProductStatus(currentUserId(request), currentUserRole(request), productId, finalStatus);
        return Result.success("商品状态已更新");
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "删除商品")
    public Result delete(HttpServletRequest request, @PathVariable Long productId) {
        appProductService.deleteProduct(currentUserId(request), currentUserRole(request), productId);
        return Result.success("商品已删除");
    }

    @PutMapping("/{productId}/submit-review")
    @Operation(summary = "提交商品审核")
    public Result submitReview(HttpServletRequest request, @PathVariable Long productId) {
        appProductService.submitProductReview(currentUserId(request), currentUserRole(request), productId);
        return Result.success("商品已提交审核");
    }

    @PostMapping("/admin/review")
    @Operation(summary = "后台商品审核")
    public Result review(HttpServletRequest request, @RequestBody ProductReviewDto reviewDto) {
        if (!"Admin".equals(currentUserRole(request))) {
            throw new IllegalArgumentException("仅管理员可审核商品");
        }
        appProductService.reviewProduct(currentUserId(request), reviewDto);
        return Result.success("审核完成");
    }

    @PostMapping("/spec-templates")
    @Operation(summary = "规格模板列表")
    public Result specTemplates(HttpServletRequest request, @RequestBody(required = false) ProductSpecTemplateDto queryDto) {
        return Result.success(appProductService.listSpecTemplates(currentUserId(request), currentUserRole(request), queryDto));
    }

    @PostMapping("/spec-templates/save")
    @Operation(summary = "新增或修改规格模板")
    public Result saveSpecTemplate(HttpServletRequest request, @RequestBody ProductSpecTemplateDto templateDto) {
        return Result.success(appProductService.saveSpecTemplate(currentUserId(request), currentUserRole(request), templateDto));
    }

    @DeleteMapping("/spec-templates/{templateId}")
    @Operation(summary = "删除规格模板")
    public Result deleteSpecTemplate(HttpServletRequest request, @PathVariable Long templateId) {
        appProductService.deleteSpecTemplate(currentUserId(request), currentUserRole(request), templateId);
        return Result.success("规格模板已删除");
    }

    /**
     * Admin 专属：将所有商品中存有时效性预签名 URL 的图片字段批量归一化为 objectKey
     * （一次性修复历史脏数据，可重复调用，幂等）
     */
    @PostMapping("/admin/fix-image-keys")
    @Operation(summary = "修复商品图片字段（预签名URL→objectKey）")
    public Result fixImageKeys(HttpServletRequest request) {
        if (!"Admin".equals(currentUserRole(request))) {
            throw new IllegalArgumentException("仅管理员可执行此操作");
        }
        return Result.success(appProductService.fixProductImageKeys());
    }

    private Long currentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value == null) {
            throw new IllegalArgumentException("请先登录");
        }
        return Long.valueOf(String.valueOf(value));
    }

    private Long optionalUserId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value != null) {
            return Long.valueOf(String.valueOf(value));
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.trim().isEmpty()) {
            return null;
        }
        String token = authorization.trim();
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        try {
            Claims claims = JwtUtils.checkToken(token);
            Object id = claims == null ? null : claims.get("id");
            return id == null ? null : Long.valueOf(String.valueOf(id));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String currentUserRole(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserRole");
        return value == null ? null : String.valueOf(value);
    }
}
