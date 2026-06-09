package com.yxshop.Module.Auth.Service;

import com.yxshop.Module.Auth.Dto.LoginDto;
import com.yxshop.Module.Auth.Dto.BindPhoneDto;
import com.yxshop.Module.Auth.Dto.PasswordChangeDto;
import com.yxshop.Module.Auth.Dto.PasswordResetDto;
import com.yxshop.Module.Auth.Dto.RegisterDto;
import com.yxshop.Module.Auth.Dto.SendCodeDto;
import com.yxshop.Module.Auth.Vo.LoginVo;

public interface AuthService {
    LoginVo login(LoginDto loginDto);

    LoginVo adminLogin(LoginDto loginDto);

    LoginVo register(RegisterDto registerDto);

    void sendCode(SendCodeDto sendCodeDto);

    /** 校验验证码是否正确，成功后验证码即失效 */
    boolean verifyCode(String account, String code);

    void resetPassword(PasswordResetDto resetDto);

    void changePassword(Long userId, PasswordChangeDto changeDto);

    void bindPhone(Long userId, BindPhoneDto bindPhoneDto);

    void logout(String authorizationHeader);
}
