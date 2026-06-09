package com.yxshop.Module.AfterSales.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.AfterSales.Entity.AfterSalesEntity;
import com.yxshop.Module.AfterSales.Mapper.AfterSalesModuleMapper;
import com.yxshop.Module.AfterSales.Service.AfterSalesModuleService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class AfterSalesModuleServiceImpl extends ServiceImpl<AfterSalesModuleMapper, AfterSalesEntity> implements AfterSalesModuleService {
}
