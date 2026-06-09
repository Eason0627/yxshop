package com.yxshop.Module.Marketing.Service;

import com.yxshop.Module.Marketing.Dto.PromotionCalculateDto;
import com.yxshop.Module.Marketing.Vo.PromotionCalculateVo;

public interface PromotionCalculationService {
    PromotionCalculateVo calculate(PromotionCalculateDto dto);
}
