package com.yxshop.Module.Shop.Controller;

import com.yxshop.Module.Shop.Service.BrandHallService;
import com.yxshop.Utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/brands")
public class BrandHallController {
    private final BrandHallService brandHallService;

    public BrandHallController(BrandHallService brandHallService) {
        this.brandHallService = brandHallService;
    }

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "20") Integer size,
                       @RequestParam(required = false) String keyword) {
        return Result.success(brandHallService.listBrands(page, size, keyword));
    }

    @GetMapping("/{shopId}")
    public Result detail(@PathVariable Long shopId,
                         @RequestParam(defaultValue = "12") Integer productLimit) {
        return Result.success(brandHallService.brandHome(shopId, productLimit));
    }
}
