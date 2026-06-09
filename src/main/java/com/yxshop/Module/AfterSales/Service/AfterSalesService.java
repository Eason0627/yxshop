package com.yxshop.Module.AfterSales.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxshop.Module.AfterSales.Dto.AfterSalesApplyDto;
import com.yxshop.Module.AfterSales.Dto.AfterSalesQueryDto;
import com.yxshop.Module.AfterSales.Dto.AfterSalesReviewDto;
import com.yxshop.Module.AfterSales.Entity.AfterSalesRequestEntity;
import com.yxshop.Module.AfterSales.Vo.AfterSalesVo;

import java.util.Map;

public interface AfterSalesService extends IService<AfterSalesRequestEntity> {
    AfterSalesVo apply(Long userId, AfterSalesApplyDto dto);

    Map<String, Object> list(Long userId, String role, AfterSalesQueryDto queryDto);

    AfterSalesVo detail(Long userId, String role, Long id);

    AfterSalesVo review(Long operatorId, String role, AfterSalesReviewDto dto);

    AfterSalesVo cancel(Long userId, Long id);
}
