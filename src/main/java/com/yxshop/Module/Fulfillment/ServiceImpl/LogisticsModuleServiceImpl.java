package com.yxshop.Module.Fulfillment.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.Fulfillment.Entity.LogisticsEntity;
import com.yxshop.Module.Fulfillment.Mapper.LogisticsModuleMapper;
import com.yxshop.Module.Fulfillment.Service.LogisticsModuleService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class LogisticsModuleServiceImpl extends ServiceImpl<LogisticsModuleMapper, LogisticsEntity> implements LogisticsModuleService {
}
