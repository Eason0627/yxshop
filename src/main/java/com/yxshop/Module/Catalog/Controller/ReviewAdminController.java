package com.yxshop.Module.Catalog.Controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yxshop.Module.Catalog.Entity.AppProductEntity;
import com.yxshop.Module.Catalog.Entity.ProductCommentEntity;
import com.yxshop.Module.Catalog.Mapper.AppProductMapper;
import com.yxshop.Module.Catalog.Mapper.ProductCommentMapper;
import com.yxshop.Module.User.Entity.User;
import com.yxshop.Module.User.Mapper.UserMapper;
import com.yxshop.Utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/app/reviews")
@Tag(name = "Review Admin", description = "商品评价管理接口")
public class ReviewAdminController {

    private final ProductCommentMapper commentMapper;
    private final UserMapper userMapper;
    private final AppProductMapper productMapper;

    public ReviewAdminController(ProductCommentMapper commentMapper,
                                  UserMapper userMapper,
                                  AppProductMapper productMapper) {
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
    }

    /**
     * GET /app/reviews/admin — admin paginated review list
     * Params: shopId, keyword, rating, status, hasImages, hasReply, startDate, endDate, pageNum, pageSize
     */
    @GetMapping("/admin")
    @Operation(summary = "后台评价列表（管理员）")
    public Result adminList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean hasImages,
            @RequestParam(required = false) Boolean hasReply,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        int page = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 20 : pageSize;

        QueryWrapper<ProductCommentEntity> wrapper = new QueryWrapper<>();

        // Filter by shop: collect product IDs belonging to this shop first
        if (shopId != null) {
            QueryWrapper<AppProductEntity> productQuery = new QueryWrapper<>();
            productQuery.eq("shop_id", shopId).select("id");
            List<Long> productIds = productMapper.selectList(productQuery)
                    .stream()
                    .map(AppProductEntity::getId)
                    .collect(Collectors.toList());
            if (productIds.isEmpty()) {
                // No products in this shop → return empty result
                Map<String, Object> emptyResult = new HashMap<>();
                emptyResult.put("records", new ArrayList<>());
                emptyResult.put("total", 0);
                emptyResult.put("pageNum", page);
                emptyResult.put("pageSize", size);
                return Result.success(emptyResult);
            }
            wrapper.in("product_id", productIds);
        }

        if (rating != null) {
            wrapper.eq("rating", rating);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (Boolean.TRUE.equals(hasImages)) {
            wrapper.isNotNull("images").ne("images", "").ne("images", "[]");
        } else if (Boolean.FALSE.equals(hasImages)) {
            wrapper.and(w -> w.isNull("images").or().eq("images", "").or().eq("images", "[]"));
        }
        if (Boolean.TRUE.equals(hasReply)) {
            wrapper.isNotNull("reply").ne("reply", "");
        } else if (Boolean.FALSE.equals(hasReply)) {
            wrapper.and(w -> w.isNull("reply").or().eq("reply", ""));
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge("created_at", startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le("created_at", endDate + " 23:59:59");
        }

        // keyword filter on content
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim() + "%";
            wrapper.like("content", kw);
        }

        wrapper.orderByDesc("created_at");

        IPage<ProductCommentEntity> resultPage = commentMapper.selectPage(new Page<>(page, size), wrapper);
        List<ProductCommentEntity> comments = resultPage.getRecords();

        List<Map<String, Object>> records = new ArrayList<>();
        for (ProductCommentEntity c : comments) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", c.getId());
            row.put("productId", c.getProductId());
            row.put("userId", c.getUserId());
            row.put("orderId", c.getOrderId());
            row.put("rating", c.getRating());
            row.put("content", c.getContent());
            row.put("status", c.getStatus() == null ? 0 : c.getStatus());
            row.put("reply", c.getReply());
            row.put("repliedAt", c.getRepliedAt() == null ? null : c.getRepliedAt().toString());
            row.put("createdAt", c.getCreatedAt() == null ? null : c.getCreatedAt().toString());

            // Parse images JSON array
            row.put("images", parseImages(c.getImages()));

            // Enrich with user info
            if (c.getUserId() != null) {
                User u = userMapper.selectById(c.getUserId());
                if (u != null) {
                    row.put("username", u.getNick_name() != null ? u.getNick_name() : u.getUsername());
                    row.put("userAvatar", u.getAvatar());
                }
            }

            // Enrich with product info
            if (c.getProductId() != null) {
                AppProductEntity p = productMapper.selectById(c.getProductId());
                if (p != null) {
                    row.put("productName", p.getName());
                    row.put("productImage", p.getMainImage());
                }
            }

            records.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", resultPage.getTotal());
        result.put("pageNum", page);
        result.put("pageSize", size);

        return Result.success(result);
    }

    /** PUT /app/reviews/{id}/approve — 通过审核 (status → 1) */
    @PutMapping("/{id}/approve")
    @Operation(summary = "通过评价审核")
    public Result approve(@PathVariable Long id) {
        ProductCommentEntity c = commentMapper.selectById(id);
        if (c == null) throw new IllegalArgumentException("评价不存在");
        c.setStatus(1);
        c.setUpdatedAt(LocalDateTime.now());
        commentMapper.updateById(c);
        return Result.success("已通过审核");
    }

    /** PUT /app/reviews/{id}/block — 屏蔽评价 (status → -1) */
    @PutMapping("/{id}/block")
    @Operation(summary = "屏蔽评价")
    public Result block(@PathVariable Long id) {
        ProductCommentEntity c = commentMapper.selectById(id);
        if (c == null) throw new IllegalArgumentException("评价不存在");
        c.setStatus(-1);
        c.setUpdatedAt(LocalDateTime.now());
        commentMapper.updateById(c);
        return Result.success("已屏蔽");
    }

    /** PUT /app/reviews/{id}/restore — 恢复评价 (status → 1) */
    @PutMapping("/{id}/restore")
    @Operation(summary = "恢复评价")
    public Result restore(@PathVariable Long id) {
        ProductCommentEntity c = commentMapper.selectById(id);
        if (c == null) throw new IllegalArgumentException("评价不存在");
        c.setStatus(1);
        c.setUpdatedAt(LocalDateTime.now());
        commentMapper.updateById(c);
        return Result.success("已恢复");
    }

    /** PUT /app/reviews/{id}/reply — 商家回复评价 */
    @PutMapping("/{id}/reply")
    @Operation(summary = "回复评价")
    public Result reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String replyText = body.get("reply");
        if (replyText == null || replyText.trim().isEmpty()) {
            throw new IllegalArgumentException("回复内容不能为空");
        }
        ProductCommentEntity c = commentMapper.selectById(id);
        if (c == null) throw new IllegalArgumentException("评价不存在");
        c.setReply(replyText.trim());
        c.setRepliedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        commentMapper.updateById(c);
        return Result.success("回复成功");
    }

    /**
     * Parse the JSON images string (e.g. ["url1","url2"] or "url1,url2")
     * into a List<String>. Returns empty list on null/empty.
     */
    private List<String> parseImages(String images) {
        if (images == null || images.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String s = images.trim();
        // JSON array format: ["url1","url2"]
        if (s.startsWith("[")) {
            s = s.replaceAll("^\\[|]$", "").trim();
            if (s.isEmpty()) return new ArrayList<>();
            List<String> result = new ArrayList<>();
            for (String part : s.split(",")) {
                String url = part.trim().replaceAll("^\"|\"$", "").trim();
                if (!url.isEmpty()) result.add(url);
            }
            return result;
        }
        // Comma-separated fallback
        return Arrays.asList(s.split(","));
    }
}
