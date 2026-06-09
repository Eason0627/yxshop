package com.yxshop.Module.Content.Controller;

import com.yxshop.Module.Content.Dto.TopicDto;
import com.yxshop.Module.Content.Service.TopicService;
import com.yxshop.Utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/topics")
public class TopicController {
    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "20") Integer size,
                       @RequestParam(required = false) String sortBy) {
        return Result.success(topicService.listTopics(page, size, sortBy));
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        return Result.success(topicService.detail(id));
    }

    @PostMapping("/admin")
    public Result create(@RequestAttribute("currentUserId") Object currentUserId,
                         @RequestAttribute("currentUserRole") Object currentUserRole,
                         @RequestBody TopicDto dto) {
        if (!"Admin".equals(String.valueOf(currentUserRole))) {
            throw new IllegalArgumentException("仅管理员可创建话题");
        }
        return Result.success(topicService.create(Long.parseLong(currentUserId.toString()), dto));
    }
}
