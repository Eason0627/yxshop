package com.yxshop.Module.Refund.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxshop.Module.Refund.Dto.RefundCallbackDto;
import com.yxshop.Module.Refund.Entity.RefundRecordEntity;

public interface RefundService extends IService<RefundRecordEntity> {
    Object detail(Long userId, String role, Long refundId);

    Object callback(RefundCallbackDto dto);

    Object simulateSuccess(Long operatorId, String role, Long refundId);
}
