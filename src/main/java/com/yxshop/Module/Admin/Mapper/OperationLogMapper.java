package com.yxshop.Module.Admin.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yxshop.Module.Admin.Entity.OperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OPerationLogMapper extends BaseMapper<OperationLog> {

//    插入日志数据
    @Insert("insert into operation_log(user_id,operation,request_url,method,params,ip,result_type,result_value,create_time,cost_time) " +
            "values(#{user_id},#{operation},#{request_url},#{method},#{params},#{ip},#{result_type},#{result_value},#{create_time},#{cost_time})")
    void insertOperationLog(OperationLog log);
}
