package com.yxshop.Module.Admin.Service;

import com.yxshop.Module.Admin.Dto.AdminUserDto;
import com.yxshop.Module.Admin.Vo.AdminUserVo;

import java.util.List;
import java.util.Map;

public interface AdminUserService {
    Map<String, Object> login(String username, String password, String ip);
    void logout(Long adminUserId, String token);
    AdminUserVo getCurrentAdmin(Long adminUserId);
    Object listAdmins(Integer page, Integer size, String keyword);
    AdminUserVo createAdmin(AdminUserDto dto);
    AdminUserVo updateAdmin(Long operatorId, AdminUserDto dto);
    void deleteAdmin(Long operatorId, Long targetId);

    /** 当前管理员修改自己的密码 */
    void changePassword(Long adminId, String oldPassword, String newPassword);

    // 角色 CRUD
    Object listRoles();
    Object createRole(Map<String, Object> dto);
    Object updateRole(Long id, Map<String, Object> dto);
    void deleteRole(Long id);

    // 角色权限
    List<String> getRolePermissions(Long roleId);
    void saveRolePermissions(Long roleId, List<String> permissions);

    Object listMenuTree(Long adminUserId);
    Object listLoginLogs(Integer page, Integer size, Long adminUserId, String keyword, String startDate, String endDate);
    Object listOperationLogs(Integer page, Integer size, String keyword, String module, String action, String startDate, String endDate);

    // 系统配置
    Map<String, Object> getSystemConfig();
    void saveSystemConfig(String section, Map<String, Object> data);
}
