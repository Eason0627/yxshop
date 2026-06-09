package com.yxshop.Module.Content.Controller;

import com.yxshop.Module.Content.Dto.ContentCommentDto;
import com.yxshop.Module.Content.Dto.ContentPostDto;
import com.yxshop.Module.Content.Dto.ContentReviewDto;
import com.yxshop.Module.Content.Service.ContentPostService;
import com.yxshop.Utils.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/app/content")
public class ContentPostController {
    private final ContentPostService contentPostService;

    public ContentPostController(ContentPostService contentPostService) {
        this.contentPostService = contentPostService;
    }

    @GetMapping("/feed")
    public Result feed(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "20") Integer size,
                       @RequestParam(required = false) Long topicId,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) String sortBy,
                       HttpServletRequest request) {
        return Result.success(contentPostService.feed(page, size, topicId, type, sortBy, optionalUserId(request)));
    }

    @GetMapping("/reels")
    public Result reels(@RequestParam(defaultValue = "1") Integer page,
                        @RequestParam(defaultValue = "20") Integer size,
                        HttpServletRequest request) {
        return Result.success(contentPostService.reels(page, size, optionalUserId(request)));
    }

    @GetMapping("/discover")
    public Result discover(@RequestParam(defaultValue = "12") Integer topicLimit,
                           @RequestParam(defaultValue = "6") Integer activityLimit,
                           @RequestParam(defaultValue = "10") Integer brandLimit) {
        return Result.success(contentPostService.discover(topicLimit, activityLimit, brandLimit));
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        return Result.success(contentPostService.detail(id));
    }

    @PostMapping
    public Result publish(@RequestAttribute("currentUserId") Object currentUserId,
                          @RequestBody ContentPostDto dto) {
        return Result.success(contentPostService.publish(Long.parseLong(currentUserId.toString()), dto));
    }

    @PutMapping("/{id}/like")
    public Result like(@RequestAttribute("currentUserId") Object currentUserId,
                       @PathVariable Long id) {
        return Result.success(contentPostService.like(Long.parseLong(currentUserId.toString()), id));
    }

    @GetMapping("/{id}/comments")
    public Result comments(@PathVariable Long id,
                           @RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(contentPostService.comments(id, page, size));
    }

    @PostMapping("/{id}/comments")
    public Result addComment(@RequestAttribute("currentUserId") Object currentUserId,
                             @PathVariable Long id,
                             @RequestBody ContentCommentDto dto) {
        if (dto == null) {
            dto = new ContentCommentDto();
        }
        dto.setPostId(id);
        return Result.success(contentPostService.addComment(Long.parseLong(currentUserId.toString()), dto));
    }

    @GetMapping("/mine")
    public Result mine(@RequestAttribute("currentUserId") Object currentUserId,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(contentPostService.myPosts(Long.parseLong(currentUserId.toString()), page, size));
    }

    @PostMapping("/admin/review")
    public Result review(@RequestAttribute("currentUserId") Object currentUserId,
                         @RequestAttribute("currentUserRole") Object currentUserRole,
                         @RequestBody ContentReviewDto dto) {
        if (!"Admin".equals(String.valueOf(currentUserRole))) {
            throw new IllegalArgumentException("仅管理员可审核内容");
        }
        return Result.success(contentPostService.review(Long.parseLong(currentUserId.toString()), dto));
    }

    // ── Admin management endpoints ────────────────────────────────────────────

    @GetMapping("/posts/admin")
    public Result adminList(@RequestAttribute("currentUserRole") Object currentUserRole,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "20") Integer pageSize,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Integer status,
                            @RequestParam(required = false) Long topicId,
                            @RequestParam(required = false) Boolean hasImages,
                            @RequestParam(required = false) String startDate,
                            @RequestParam(required = false) String endDate) {
        if (!"Admin".equals(String.valueOf(currentUserRole))) {
            throw new IllegalArgumentException("仅管理员可访问");
        }
        return Result.success(contentPostService.adminListPosts(
                pageNum, pageSize, keyword, status, topicId, hasImages, startDate, endDate));
    }

    @PutMapping("/posts/{id}/approve")
    public Result approve(@RequestAttribute("currentUserRole") Object currentUserRole,
                          @PathVariable Long id) {
        if (!"Admin".equals(String.valueOf(currentUserRole))) {
            throw new IllegalArgumentException("仅管理员可操作");
        }
        return Result.success(contentPostService.approvePost(id));
    }

    @PutMapping("/posts/{id}/reject")
    public Result reject(@RequestAttribute("currentUserRole") Object currentUserRole,
                         @PathVariable Long id) {
        if (!"Admin".equals(String.valueOf(currentUserRole))) {
            throw new IllegalArgumentException("仅管理员可操作");
        }
        return Result.success(contentPostService.rejectPost(id));
    }

    @PutMapping("/posts/{id}/hide")
    public Result hide(@RequestAttribute("currentUserRole") Object currentUserRole,
                       @PathVariable Long id) {
        if (!"Admin".equals(String.valueOf(currentUserRole))) {
            throw new IllegalArgumentException("仅管理员可操作");
        }
        return Result.success(contentPostService.hidePost(id));
    }

    @PutMapping("/posts/{id}/restore")
    public Result restore(@RequestAttribute("currentUserRole") Object currentUserRole,
                          @PathVariable Long id) {
        if (!"Admin".equals(String.valueOf(currentUserRole))) {
            throw new IllegalArgumentException("仅管理员可操作");
        }
        return Result.success(contentPostService.restorePost(id));
    }

    @DeleteMapping("/posts/{id}")
    public Result delete(@RequestAttribute("currentUserRole") Object currentUserRole,
                         @PathVariable Long id) {
        if (!"Admin".equals(String.valueOf(currentUserRole))) {
            throw new IllegalArgumentException("仅管理员可操作");
        }
        contentPostService.deletePost(id);
        return Result.success("删除成功");
    }

    private Long optionalUserId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        return value == null ? null : Long.parseLong(String.valueOf(value));
    }
}
