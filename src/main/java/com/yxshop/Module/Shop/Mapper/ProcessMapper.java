package com.yxshop.Module.Shop.Mapper;

import com.yxshop.Module.Shop.Entity.Process;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author hym
 * @since 2024-08-14
 */
@Mapper
public interface ProcessMapper extends BaseMapper<Process> {
    @Select("select user.nick_name from user where id = #{id,jdbcType=BIGINT}")
    String getApplicantName(@Param("id") Long id);

    /**
     * 严格匹配图片字段：退货退款、仅退货、仅退款、退换货
     */
    Map<String, Long> getAfterSaleCount(@Param("shopId") String shopId);
}
