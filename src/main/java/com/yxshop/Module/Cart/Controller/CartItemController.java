package com.yxshop.Module.Cart.Controller;

import com.yxshop.Module.Cart.Dto.CartItemDto;
import com.yxshop.Module.Cart.Dto.CartSelectionDto;
import com.yxshop.Module.Cart.Service.CartItemService;
import com.yxshop.Utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/app/cart")
@Tag(name = "Cart", description = "购物车、勾选和结算预览接口")
public class CartItemController {

    private final CartItemService cartItemService;

    public CartItemController(CartItemService cartItemService) {
        this.cartItemService = cartItemService;
    }

    @GetMapping
    @Operation(summary = "购物车列表")
    public Result list(HttpServletRequest request) {
        return Result.success(cartItemService.listCart(currentUserId(request)));
    }

    @PostMapping
    @Operation(summary = "加入购物车")
    public Result add(HttpServletRequest request, @RequestBody CartItemDto dto) {
        return Result.success(cartItemService.addItem(currentUserId(request), dto));
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "修改购物车数量或勾选状态")
    public Result update(HttpServletRequest request, @PathVariable Long itemId, @RequestBody CartItemDto dto) {
        return Result.success(cartItemService.updateItem(currentUserId(request), itemId, dto));
    }

    @PutMapping("/selection")
    @Operation(summary = "批量勾选购物车")
    public Result selection(HttpServletRequest request, @RequestBody CartSelectionDto dto) {
        return Result.success(cartItemService.updateSelection(currentUserId(request), dto));
    }

    @DeleteMapping
    @Operation(summary = "批量删除购物车项")
    public Result delete(HttpServletRequest request, @RequestBody List<Long> itemIds) {
        return Result.success(cartItemService.deleteItems(currentUserId(request), itemIds));
    }

    @DeleteMapping("/invalid")
    @Operation(summary = "清理失效商品")
    public Result clearInvalid(HttpServletRequest request) {
        return Result.success(cartItemService.clearInvalid(currentUserId(request)));
    }

    @GetMapping("/settlement")
    @Operation(summary = "勾选商品结算预览")
    public Result settlement(HttpServletRequest request) {
        return Result.success(cartItemService.selectedSettlement(currentUserId(request)));
    }

    private Long currentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value == null) {
            throw new IllegalArgumentException("请先登录");
        }
        return Long.valueOf(String.valueOf(value));
    }
}
