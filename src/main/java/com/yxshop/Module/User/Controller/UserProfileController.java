package com.yxshop.Module.User.Controller;

import com.yxshop.Module.User.Dto.AddressDto;
import com.yxshop.Module.User.Dto.TargetDto;
import com.yxshop.Module.User.Dto.UserProfileDto;
import com.yxshop.Module.User.Service.UserProfileService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/app/users")
@Tag(name = "App User", description = "App 用户资料接口")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户资料")
    public Result me(HttpServletRequest request) {
        return Result.success(userProfileService.getCurrentProfile(currentUserId(request)));
    }

    @PutMapping("/me")
    @Operation(summary = "修改当前用户资料")
    public Result updateMe(HttpServletRequest request, @RequestBody UserProfileDto profileDto) {
        return Result.success(userProfileService.updateCurrentProfile(currentUserId(request), profileDto));
    }

    @GetMapping("/center")
    @Operation(summary = "个人中心聚合信息")
    public Result center(HttpServletRequest request) {
        return Result.success(userProfileService.getUserCenter(currentUserId(request)));
    }

    @GetMapping("/addresses")
    @Operation(summary = "收货地址列表")
    public Result addresses(HttpServletRequest request) {
        return Result.success(userProfileService.listAddresses(currentUserId(request)));
    }

    @PostMapping("/addresses")
    @Operation(summary = "新增收货地址")
    public Result addAddress(HttpServletRequest request, @RequestBody AddressDto addressDto) {
        return Result.success(userProfileService.addAddress(currentUserId(request), addressDto));
    }

    @PutMapping("/addresses/{addressId}")
    @Operation(summary = "修改收货地址")
    public Result updateAddress(HttpServletRequest request, @PathVariable Long addressId, @RequestBody AddressDto addressDto) {
        return Result.success(userProfileService.updateAddress(currentUserId(request), addressId, addressDto));
    }

    @DeleteMapping("/addresses/{addressId}")
    @Operation(summary = "删除收货地址")
    public Result deleteAddress(HttpServletRequest request, @PathVariable Long addressId) {
        userProfileService.deleteAddress(currentUserId(request), addressId);
        return Result.success("地址已删除");
    }

    @PutMapping("/addresses/{addressId}/default")
    @Operation(summary = "设置默认地址")
    public Result setDefaultAddress(HttpServletRequest request, @PathVariable Long addressId) {
        userProfileService.setDefaultAddress(currentUserId(request), addressId);
        return Result.success("默认地址已更新");
    }

    @GetMapping("/favorites")
    @Operation(summary = "收藏列表")
    public Result favorites(HttpServletRequest request, @RequestParam(required = false) String targetType) {
        return Result.success(userProfileService.listFavorites(currentUserId(request), targetType));
    }

    @PostMapping("/favorites")
    @Operation(summary = "添加收藏")
    public Result addFavorite(HttpServletRequest request, @RequestBody TargetDto targetDto) {
        userProfileService.addFavorite(currentUserId(request), targetDto);
        return Result.success("已收藏");
    }

    @DeleteMapping("/favorites/{targetType}/{targetId}")
    @Operation(summary = "取消收藏")
    public Result deleteFavorite(HttpServletRequest request, @PathVariable String targetType, @PathVariable Long targetId) {
        userProfileService.deleteFavorite(currentUserId(request), targetType, targetId);
        return Result.success("已取消收藏");
    }

    @GetMapping("/favorites/{targetType}/{targetId}")
    @Operation(summary = "检查是否收藏")
    public Result isFavorite(HttpServletRequest request, @PathVariable String targetType, @PathVariable Long targetId) {
        return Result.success(userProfileService.isFavorite(currentUserId(request), targetType, targetId));
    }

    @GetMapping("/footprints")
    @Operation(summary = "浏览足迹")
    public Result footprints(HttpServletRequest request) {
        return Result.success(userProfileService.listFootprints(currentUserId(request)));
    }

    @PostMapping("/footprints")
    @Operation(summary = "记录浏览足迹")
    public Result addFootprint(HttpServletRequest request, @RequestBody TargetDto targetDto) {
        userProfileService.addFootprint(currentUserId(request), targetDto);
        return Result.success("足迹已记录");
    }

    @DeleteMapping("/footprints")
    @Operation(summary = "清空浏览足迹")
    public Result clearFootprints(HttpServletRequest request) {
        userProfileService.clearFootprints(currentUserId(request));
        return Result.success("足迹已清空");
    }

    // ===== Admin: user management =====

    @PostMapping("/admin/create")
    @Operation(summary = "后台新建用户（管理员）")
    public Result adminCreate(@RequestBody java.util.Map<String, String> data) {
        return Result.success(userProfileService.createUserAdmin(data));
    }

    @GetMapping("/admin/list")
    @Operation(summary = "后台用户列表（管理员）")
    public Result adminList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer pointsMin,
            @RequestParam(required = false) Integer pointsMax) {
        return Result.success(userProfileService.listUsersAdmin(pageNum, pageSize, keyword, role, status, startDate, endDate, pointsMin, pointsMax));
    }

    @GetMapping("/admin/{userId}")
    @Operation(summary = "后台用户详情（管理员）")
    public Result adminDetail(@PathVariable Long userId) {
        return Result.success(userProfileService.getUserAdmin(userId));
    }

    @PutMapping("/admin/{userId}/status")
    @Operation(summary = "后台修改用户状态（管理员）")
    public Result adminUpdateStatus(@PathVariable Long userId, @RequestBody java.util.Map<String, Object> body) {
        Integer status = body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : null;
        if (status == null) throw new IllegalArgumentException("status 不能为空");
        userProfileService.updateUserStatus(userId, status);
        return Result.success("状态已更新");
    }

    @PutMapping("/admin/{userId}/reset-password")
    @Operation(summary = "后台重置用户密码（管理员）")
    public Result adminResetPassword(@PathVariable Long userId,
                                     @RequestBody java.util.Map<String, String> body) {
        String newPassword = body.get("password");
        if (newPassword == null || newPassword.isBlank()) throw new IllegalArgumentException("新密码不能为空");
        if (newPassword.length() < 6) throw new IllegalArgumentException("密码不能少于6位");
        userProfileService.resetUserPassword(userId, newPassword);
        return Result.success("密码已重置");
    }

    private Long currentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value == null) {
            throw new IllegalArgumentException("请先登录");
        }
        return Long.valueOf(String.valueOf(value));
    }
}
