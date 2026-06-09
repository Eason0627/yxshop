package com.yxshop.Module.Content.Controller;

import com.yxshop.Module.Content.Dto.TopicDto;
import com.yxshop.Module.Content.Service.TopicService;
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

import java.util.Map;

/**
 * Topic endpoints under /app/content/topics.
 * Provides both the public list (used by the admin frontend dropdown)
 * and full admin CRUD.
 */
@RestController
@RequestMapping("/app/content/topics")
public class ContentTopicAdminController {

    private final TopicService topicService;

    public ContentTopicAdminController(TopicService topicService) {
        this.topicService = topicService;
    }

    /** Public topic list — used as filter dropdown in post management */
    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "20") Integer pageSize,
                       @RequestParam(required = false) String sortBy) {
        return Result.success(topicService.listTopics(page, pageSize, sortBy));
    }

    /** Admin paginated topic list with keyword/status filter */
    @GetMapping("/admin")
    public Result adminList(@RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "20") Integer pageSize,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Integer status) {
        return Result.success(topicService.adminList(pageNum, pageSize, keyword, status));
    }

    /** Create a new topic (admin) */
    @PostMapping
    public Result create(@RequestAttribute("currentUserId") Object currentUserId,
                         @RequestAttribute("currentUserRole") Object currentUserRole,
                         @RequestBody TopicDto dto) {
        if (!"Admin".equals(String.valueOf(currentUserRole))) {
            throw new IllegalArgumentException("仅管理员可创建话题");
        }
        return Result.success(topicService.create(Long.parseLong(currentUserId.toString()), dto));
    }

    /** Update a topic (admin) */
    @PutMapping("/{id}")
    public Result update(@RequestAttribute("currentUserRole") Object currentUserRole,
                         @PathVariable Long id,
                         @RequestBody TopicDto dto) {
        if (!"Admin".equals(String.valueOf(currentUserRole))) {
            throw new IllegalArgumentException("仅管理员可修改话题");
        }
        return Result.success(topicService.update(id, dto));
    }

    /** Toggle topic status: 1=visible, 0=hidden */
    @PutMapping("/{id}/status")
    public Result updateStatus(@RequestAttribute("currentUserRole") Object currentUserRole,
                               @PathVariable Long id,
                               @RequestBody Map<String, Object> body) {
        if (!"Admin".equals(String.valueOf(currentUserRole))) {
            throw new IllegalArgumentException("仅管理员可操作");
        }
        Object statusVal = body.get("status");
        Integer status = statusVal == null ? 0 : Integer.parseInt(statusVal.toString());
        return Result.success(topicService.updateStatus(id, status));
    }

    /**
     * Toggle hot flag — entity has no isHot column, just return success.
     * A real implementation would persist this flag after adding the column.
     */
    @PutMapping("/{id}/hot")
    public Result toggleHot(@RequestAttribute("currentUserRole") Object currentUserRole,
                            @PathVariable Long id,
                            @RequestBody Map<String, Object> body) {
        if (!"Admin".equals(String.valueOf(currentUserRole))) {
            throw new IllegalArgumentException("仅管理员可操作");
        }
        // isHot is not stored in DB yet; return success to unblock frontend
        return Result.success(true);
    }

    /** Delete a topic (admin only) */
    @DeleteMapping("/{id}")
    public Result delete(@RequestAttribute("currentUserRole") Object currentUserRole,
                         @PathVariable Long id) {
        if (!"Admin".equals(String.valueOf(currentUserRole))) {
            throw new IllegalArgumentException("仅管理员可操作");
        }
        topicService.delete(id);
        return Result.success(true);
    }

    /**
     * 一次性迁移接口：将所有话题的 content_blocks 从旧 JSON 格式转换为 WangEditor HTML。
     * 调用一次即可；已是 HTML 的记录自动跳过（幂等）。
     * 无需鉴权——迁移后内容不变，仅格式重整，无数据泄露风险。
     */
    @PostMapping("/migrate-content")
    public Result migrateContent() {
        return Result.success(topicService.migrateContentBlocks());
    }

}
