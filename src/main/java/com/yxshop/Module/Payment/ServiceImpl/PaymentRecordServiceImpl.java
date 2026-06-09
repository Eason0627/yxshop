package com.yxshop.Module.Payment.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.Payment.Entity.PaymentRecordEntity;
import com.yxshop.Module.Payment.Mapper.PaymentRecordMapper;
import com.yxshop.Module.Payment.Service.PaymentRecordService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class PaymentRecordServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecordEntity> implements PaymentRecordService {
}
