package com.yxshop.Module.Search.Controller;

import com.yxshop.Module.Catalog.Dto.AppProductQueryDto;
import com.yxshop.Module.Search.Service.SearchService;
import com.yxshop.Utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/app/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/products")
    public Result products(HttpServletRequest request, @RequestBody(required = false) AppProductQueryDto queryDto) {
        return Result.success(searchService.searchProducts(currentUserIdOrNull(request), queryDto));
    }

    @GetMapping("/hotwords")
    public Result hotwords() {
        return Result.success(searchService.hotwords());
    }

    @GetMapping("/history")
    public Result history(HttpServletRequest request) {
        return Result.success(searchService.history(currentUserId(request)));
    }

    @GetMapping("/meta")
    public Result meta(HttpServletRequest request) {
        return Result.success(searchService.meta(currentUserIdOrNull(request)));
    }

    private Long currentUserIdOrNull(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private Long currentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value == null) throw new IllegalArgumentException("请先登录");
        return Long.valueOf(String.valueOf(value));
    }
}
