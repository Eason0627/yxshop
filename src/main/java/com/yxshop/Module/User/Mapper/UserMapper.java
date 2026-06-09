package com.yxshop.Module.User.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yxshop.Module.User.Dto.LoginParam;
import com.yxshop.Module.User.Entity.Address;
import com.yxshop.Module.User.Entity.User;
import com.yxshop.Module.User.Vo.UserVo;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @author : hym
 * @date : 2024/7/15 10:30
 * @Version: 1.0
 */
@Mapper
// @Component
public interface UserMapper extends BaseMapper<User> {

    // 查询返回用户信息
    @Select("select id, username, email, phone, role, createTime, updateTime, status, avatar, birth, gender, default_address from user where username = #{username} and password = #{password}")
    User getByUsernameAndPassword(User user);

    // 注册用户
    @Insert("INSERT INTO user(id, username, nick_name, email, phone, password, role, status, createTime, updateTime) " +
            "VALUES (#{id}, #{username}, #{nick_name}, #{email}, #{phone}, #{password}, #{role}, #{status}, #{createTime}, #{updateTime})")
    int addUser(User user);

    // 用户登录
    @Select("select * from user where username = #{username}")
    User getUserByUsername(LoginParam user);

    @Select("select * from user where username = #{username}")
    User getUserByUsername(String user);

    // 邮箱登录
    @Select("select * from user where email = #{email}")
    User getUserByEmail(String email);

    // 根据id查询用户
    @Select("select * from user where id = #{id}")
    User getUserById(long id);

    /**
     * 动态查询用�?
     * 
     * @param user
     * @return
     */
    List<UserVo> getUserContains(User user);

    /**
     * 动态更新用户信�?
     * 
     * @param user
     * @return
     */
    int updateUser(User user);

    /**
     * 逻辑删除批量用户
     * 
     * @param ids
     * @return
     */
    int deleteUsers(@Param("ids") List<Long> ids);

    /**
     * 创建地址
     * 
     * @param address
     */
    @Insert("insert into address(address_id, city, country,  is_default, phone, postal_code, recipient_name, state_province, street_address, user_id) "
            +
            "values(#{address_id}, #{city}, #{country}, #{is_default}, #{phone}, #{postal_code}, #{recipient_name}, #{state_province}, #{street_address}, #{user_id})")
    void createAddress(Address address);

    /**
     * 根据id查询地址
     * 
     * @param user_id
     * @return
     */
    @Select("select * from address where user_id = #{user_id}")
    List<Address> getAddressById(String user_id);

    /**
     * 获取用户默认收货地址
     * 
     * @param userId 用户ID
     * @return 默认收货地址
     */
    @Select("SELECT * FROM address WHERE user_id = #{userId} AND is_default = 1 LIMIT 1")
    Address getDefaultAddressByUserId(String userId);

    @Update("UPDATE address SET is_default = 0 WHERE user_id = #{userId}")
    void resetDefaultAddresses(Long userId);

    /**
     * 动态修改地址
     * 
     * @param address
     */
    void updateAddress(Address address);

    /**
     * 根据 address_id 查询地址
     * 
     * @param address_id
     * @return
     */
    @Select("select * from address where address_id = #{address_id}")
    Address getAddressByaddress_id(Long address_id);

    /**
     * 删除地址
     * 
     * @param address_id
     */
    @Delete("delete from address where address_id = #{address_id}")
    void deleteAddress(Long address_id);

    @Select("select * from user where email = #{email}")
    User getUsersByEmail(String email);
}
