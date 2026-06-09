package com.yxshop.Module.Payment.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.Order.Entity.OrderEntity;
import com.yxshop.Module.Order.Mapper.OrderModuleMapper;
import com.yxshop.Module.Order.Service.OrderModuleService;
import com.yxshop.Module.Payment.Dto.PaymentCallbackDto;
import com.yxshop.Module.Payment.Dto.PaymentCreateDto;
import com.yxshop.Module.Payment.Entity.PaymentRecordEntity;
import com.yxshop.Module.Payment.Mapper.PaymentRecordMapper;
import com.yxshop.Module.Payment.Service.PaymentService;
import com.yxshop.Module.Payment.Vo.PaymentVo;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Primary
@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecordEntity> implements PaymentService {

    private final OrderModuleMapper orderMapper;
    private final OrderModuleService orderService;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(8, 1);

    public PaymentServiceImpl(OrderModuleMapper orderMapper, OrderModuleService orderService) {
        this.orderMapper = orderMapper;
        this.orderService = orderService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVo createPayment(Long userId, PaymentCreateDto dto) {
        if (dto == null || dto.getOrderId() == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        OrderEntity order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !userId.equals(order.getCustomerId())) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!"PendingPay".equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("当前订单不可支付");
        }
        QueryWrapper<PaymentRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", order.getOrderId()).ne("status", "Closed").last("LIMIT 1");
        PaymentRecordEntity existing = getOne(wrapper);
        if (existing != null) {
            return toVo(existing);
        }
        PaymentRecordEntity payment = new PaymentRecordEntity();
        payment.setId(idWorker.nextId());
        payment.setPaymentNo("PAY" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + Math.abs(payment.getId() % 100000));
        payment.setOrderId(order.getOrderId());
        payment.setUserId(userId);
        payment.setAmount(order.getOrderTotal());
        payment.setPayMethod(dto.getPayMethod() == null ? "mock" : dto.getPayMethod());
        payment.setStatus("Pending");
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        save(payment);
        return toVo(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVo simulatePay(Long userId, Long paymentId) {
        PaymentRecordEntity payment = getById(paymentId);
        if (payment == null || !userId.equals(payment.getUserId())) {
            throw new IllegalArgumentException("支付单不存在");
        }
        markPaymentPaid(payment, "simulate-pay");
        return toVo(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVo handleCallback(PaymentCallbackDto dto) {
        if (dto == null || (dto.getPaymentNo() == null && dto.getOrderId() == null)) {
            throw new IllegalArgumentException("回调参数不能为空");
        }
        QueryWrapper<PaymentRecordEntity> wrapper = new QueryWrapper<>();
        if (dto.getPaymentNo() != null) {
            wrapper.eq("payment_no", dto.getPaymentNo());
        } else {
            wrapper.eq("order_id", dto.getOrderId());
        }
        wrapper.last("LIMIT 1");
        PaymentRecordEntity payment = getOne(wrapper);
        if (payment == null) {
            throw new IllegalArgumentException("支付单不存在");
        }
        payment.setCallbackPayload(dto.getPayload());
        if ("Paid".equalsIgnoreCase(dto.getStatus()) || "SUCCESS".equalsIgnoreCase(dto.getStatus())) {
            markPaymentPaid(payment, dto.getPayload());
        } else if ("Failed".equalsIgnoreCase(dto.getStatus())) {
            payment.setStatus("Failed");
            payment.setUpdatedAt(LocalDateTime.now());
            updateById(payment);
        }
        return toVo(payment);
    }

    @Override
    public PaymentVo getPayment(Long userId, String role, Long paymentId) {
        PaymentRecordEntity payment = getById(paymentId);
        if (payment == null) {
            throw new IllegalArgumentException("支付单不存在");
        }
        if (!"Admin".equals(role) && !userId.equals(payment.getUserId())) {
            throw new IllegalArgumentException("无权访问该支付单");
        }
        return toVo(payment);
    }

    private void markPaymentPaid(PaymentRecordEntity payment, String payload) {
        if ("Paid".equals(payment.getStatus())) {
            return;
        }
        payment.setStatus("Paid");
        payment.setCallbackPayload(payload);
        payment.setPaidAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        updateById(payment);
        orderService.markPaid(payment.getOrderId(), payment.getUserId());
    }

    private PaymentVo toVo(PaymentRecordEntity payment) {
        PaymentVo vo = new PaymentVo();
        vo.setId(payment.getId());
        vo.setPaymentNo(payment.getPaymentNo());
        vo.setOrderId(payment.getOrderId());
        vo.setUserId(payment.getUserId());
        vo.setAmount(payment.getAmount());
        vo.setPayMethod(payment.getPayMethod());
        vo.setStatus(payment.getStatus());
        vo.setPaidAt(payment.getPaidAt() == null ? null : payment.getPaidAt().toString());
        vo.setCreatedAt(payment.getCreatedAt() == null ? null : payment.getCreatedAt().toString());
        return vo;
    }
}
