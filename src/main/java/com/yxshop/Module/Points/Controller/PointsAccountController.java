package com.yxshop.Module.Points.Controller;

import com.yxshop.Module.Points.Dto.PointsExchangeDto;
import com.yxshop.Module.Points.Service.PointsAccountService;
import com.yxshop.Utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/points")
public class PointsAccountController {

    private final PointsAccountService pointsAccountService;

    public PointsAccountController(PointsAccountService pointsAccountService) {
        this.pointsAccountService = pointsAccountService;
    }

    @GetMapping("/account")
    public Result account(@RequestAttribute("currentUserId") Object currentUserId) {
        return Result.success(pointsAccountService.getAccount(Long.parseLong(currentUserId.toString())));
    }

    @GetMapping("/records")
    public Result records(@RequestAttribute("currentUserId") Object currentUserId,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(pointsAccountService.getRecords(Long.parseLong(currentUserId.toString()), page, size));
    }

    @GetMapping("/goods")
    public Result goods(@RequestParam(defaultValue = "1") Integer page,
                        @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(pointsAccountService.listGoods(page, size));
    }

    @GetMapping("/goods/{goodsId}")
    public Result goodsDetail(@PathVariable Long goodsId) {
        return Result.success(pointsAccountService.getGoods(goodsId));
    }

    @GetMapping("/mall-meta")
    public Result mallMeta() {
        return Result.success(pointsAccountService.mallMeta());
    }

    @PostMapping("/exchange")
    public Result exchange(@RequestAttribute("currentUserId") Object currentUserId,
                           @RequestBody PointsExchangeDto dto) {
        return Result.success(pointsAccountService.exchange(Long.parseLong(currentUserId.toString()), dto));
    }

    @GetMapping("/orders")
    public Result orders(@RequestAttribute("currentUserId") Object currentUserId,
                         @RequestParam(defaultValue = "1") Integer page,
                         @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(pointsAccountService.listOrders(Long.parseLong(currentUserId.toString()), page, size));
    }

    @PostMapping("/checkin")
    public Result checkin(@RequestAttribute("currentUserId") Object currentUserId) {
        return Result.success(pointsAccountService.checkin(Long.parseLong(currentUserId.toString())));
    }

    @GetMapping("/checkin/today")
    public Result todayCheckin(@RequestAttribute("currentUserId") Object currentUserId) {
        return Result.success(pointsAccountService.todayCheckin(Long.parseLong(currentUserId.toString())));
    }

    @GetMapping("/checkin/config")
    public Result checkinConfig() {
        return Result.success(pointsAccountService.checkinConfig());
    }
}
