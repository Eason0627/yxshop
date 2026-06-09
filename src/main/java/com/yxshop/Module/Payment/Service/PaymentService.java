package com.yxshop.Module.Payment.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxshop.Module.Payment.Dto.PaymentCallbackDto;
import com.yxshop.Module.Payment.Dto.PaymentCreateDto;
import com.yxshop.Module.Payment.Entity.PaymentRecordEntity;
import com.yxshop.Module.Payment.Vo.PaymentVo;

public interface PaymentService extends IService<PaymentRecordEntity> {
    PaymentVo createPayment(Long userId, PaymentCreateDto dto);

    PaymentVo simulatePay(Long userId, Long paymentId);

    PaymentVo handleCallback(PaymentCallbackDto dto);

    PaymentVo getPayment(Long userId, String role, Long paymentId);
}
