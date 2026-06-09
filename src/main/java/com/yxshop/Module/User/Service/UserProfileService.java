package com.yxshop.Module.User.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxshop.Module.User.Entity.User;
import com.yxshop.Module.User.Dto.AddressDto;
import com.yxshop.Module.User.Dto.TargetDto;
import com.yxshop.Module.User.Dto.UserProfileDto;
import com.yxshop.Module.User.Entity.UserProfileEntity;
import com.yxshop.Module.User.Vo.AddressVo;
import com.yxshop.Module.User.Vo.TargetVo;
import com.yxshop.Module.User.Vo.UserCenterVo;
import com.yxshop.Module.User.Vo.UserProfileVo;

import java.util.List;
import java.util.Map;

public interface UserProfileService extends IService<UserProfileEntity> {

    // ===== Admin =====
    Map<String, Object> listUsersAdmin(Integer pageNum, Integer pageSize, String keyword, String role, Integer status,
                                       String startDate, String endDate, Integer pointsMin, Integer pointsMax);

    Map<String, Object> getUserAdmin(Long userId);

    Map<String, Object> createUserAdmin(java.util.Map<String, String> data);

    void updateUserStatus(Long userId, Integer status);
    void resetUserPassword(Long userId, String newPassword);
    UserProfileVo getCurrentProfile(Long userId);

    UserProfileVo updateCurrentProfile(Long userId, UserProfileDto profileDto);

    void ensureProfile(User user);

    UserCenterVo getUserCenter(Long userId);

    List<AddressVo> listAddresses(Long userId);

    AddressVo addAddress(Long userId, AddressDto addressDto);

    AddressVo updateAddress(Long userId, Long addressId, AddressDto addressDto);

    void deleteAddress(Long userId, Long addressId);

    void setDefaultAddress(Long userId, Long addressId);

    List<TargetVo> listFavorites(Long userId, String targetType);

    void addFavorite(Long userId, TargetDto targetDto);

    void deleteFavorite(Long userId, String targetType, Long targetId);

    Boolean isFavorite(Long userId, String targetType, Long targetId);

    List<TargetVo> listFootprints(Long userId);

    void addFootprint(Long userId, TargetDto targetDto);

    void clearFootprints(Long userId);
}
