package com.yxshop.Module.Refund.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.AfterSales.Entity.AfterSalesRequestEntity;
import com.yxshop.Module.AfterSales.Mapper.AfterSalesRequestMapper;
import com.yxshop.Module.Notification.Service.NotificationService;
import com.yxshop.Module.Order.Entity.OrderEntity;
import com.yxshop.Module.Order.Mapper.OrderModuleMapper;
import com.yxshop.Module.Points.Entity.PointsAccountEntity;
import com.yxshop.Module.Points.Entity.PointsRecordEntity;
import com.yxshop.Module.Points.Mapper.PointsAccountMapper;
import com.yxshop.Module.Points.Mapper.PointsRecordMapper;
import com.yxshop.Module.Refund.Dto.RefundCallbackDto;
import com.yxshop.Module.Refund.Entity.RefundRecordEntity;
import com.yxshop.Module.Refund.Mapper.RefundRecordMapper;
import com.yxshop.Module.Refund.Service.RefundService;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Primary
@Service
public class RefundServiceImpl extends ServiceImpl<RefundRecordMapper, RefundRecordEntity> implements RefundService {
    private final AfterSalesRequestMapper afterSalesRequestMapper;
    private final OrderModuleMapper orderMapper;
    private final NotificationService notificationService;
    private final PointsAccountMapper pointsAccountMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final ShopModuleMapper shopModuleMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(28, 1);

    public RefundServiceImpl(AfterSalesRequestMapper afterSalesRequestMapper,
                             OrderModuleMapper orderMapper,
                             NotificationService notificationService,
                             PointsAccountMapper pointsAccountMapper,
                             PointsRecordMapper pointsRecordMapper,
                             ShopModuleMapper shopModuleMapper) {
        this.afterSalesRequestMapper = afterSalesRequestMapper;
        this.orderMapper = orderMapper;
        this.notificationService = notificationService;
        this.pointsAccountMapper = pointsAccountMapper;
        this.pointsRecordMapper = pointsRecordMapper;
        this.shopModuleMapper = shopModuleMapper;
    }

    @Override
    public Object detail(Long userId, String role, Long refundId) {
        RefundRecordEntity refund = getById(refundId);
        if (refund == null) {
            throw new IllegalArgumentException("退款单不存在");
        }
        if (!canAccessRefund(userId, role, refund)) {
            throw new IllegalArgumentException("无权访问该退款单");
        }
        return refund;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object callback(RefundCallbackDto dto) {
        if (dto == null || (dto.getRefundId() == null && dto.getRefundNo() == null)) {
            throw new IllegalArgumentException("退款回调参数不能为空");
        }
        RefundRecordEntity refund = findRefund(dto);
        if (refund == null) {
            throw new IllegalArgumentException("退款单不存在");
        }
        refund.setCallbackPayload(dto.getPayload());
        if ("Refunded".equalsIgnoreCase(dto.getStatus()) || "SUCCESS".equalsIgnoreCase(dto.getStatus())) {
            markRefunded(refund);
        } else if ("Failed".equalsIgnoreCase(dto.getStatus()) || "FAIL".equalsIgnoreCase(dto.getStatus())) {
            refund.setStatus("Failed");
            refund.setUpdatedAt(LocalDateTime.now());
            updateById(refund);
        }
        return refund;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object simulateSuccess(Long operatorId, String role, Long refundId) {
        if (!"Admin".equals(role) && !"ShopOwner".equals(role)) {
            throw new IllegalArgumentException("无权操作退款");
        }
        RefundRecordEntity refund = getById(refundId);
        if (refund == null) {
            throw new IllegalArgumentException("退款单不存在");
        }
        if ("ShopOwner".equals(role) && !canAccessRefund(operatorId, role, refund)) {
            throw new IllegalArgumentException("无权操作该退款");
        }
        markRefunded(refund);
        return refund;
    }

    private boolean canAccessRefund(Long userId, String role, RefundRecordEntity refund) {
        if ("Admin".equals(role)) {
            return true;
        }
        if ("ShopOwner".equals(role)) {
            OrderEntity order = orderMapper.selectById(refund.getOrderId());
            if (order == null) {
                return false;
            }
            ShopEntity shop = shopModuleMapper.selectById(order.getShopId());
            return shop != null && userId.equals(shop.getOwnerUserId());
        }
        return userId.equals(refund.getUserId());
    }

    private RefundRecordEntity findRefund(RefundCallbackDto dto) {
        if (dto.getRefundId() != null) {
            return getById(dto.getRefundId());
        }
        return getOne(new LambdaQueryWrapper<RefundRecordEntity>()
                .eq(RefundRecordEntity::getRefundNo, dto.getRefundNo())
                .last("LIMIT 1"));
    }

    private void markRefunded(RefundRecordEntity refund) {
        if ("Refunded".equals(refund.getStatus())) {
            return;
        }
        refund.setStatus("Refunded");
        refund.setRefundedAt(LocalDateTime.now());
        refund.setUpdatedAt(LocalDateTime.now());
        updateById(refund);

        AfterSalesRequestEntity afterSales = afterSalesRequestMapper.selectById(refund.getAfterSalesId());
        if (afterSales != null) {
            afterSales.setStatus("Completed");
            afterSales.setUpdatedAt(LocalDateTime.now());
            afterSalesRequestMapper.updateById(afterSales);
        }
        OrderEntity order = orderMapper.selectById(refund.getOrderId());
        if (order != null) {
            rollbackPoints(refund);
            order.setAfterSalesStatus("Completed");
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            notificationService.send(order.getCustomerId(), "退款成功", "订单 " + order.getOrderNumber() + " 的退款已完成", "refund", refund.getId());
        }
    }

    private void rollbackPoints(RefundRecordEntity refund) {
        int points = refund.getAmount() == null ? 0 : refund.getAmount().intValue();
        if (points <= 0) {
            return;
        }
        PointsAccountEntity account = pointsAccountMapper.selectById(refund.getUserId());
        if (account == null) {
            return;
        }
        int before = account.getCurrentPoints() == null ? 0 : account.getCurrentPoints();
        int deducted = Math.min(before, points);
        if (deducted <= 0) {
            return;
        }
        account.setCurrentPoints(before - deducted);
        account.setUpdatedAt(LocalDateTime.now());
        pointsAccountMapper.updateById(account);

        PointsRecordEntity record = new PointsRecordEntity();
        record.setId(idWorker.nextId());
        record.setUserId(refund.getUserId());
        record.setChangeType("Deduct");
        record.setPoints(deducted);
        record.setBeforePoints(before);
        record.setAfterPoints(before - deducted);
        record.setBizType("refund");
        record.setBizId(refund.getId());
        record.setDescription("退款扣回积分");
        record.setCreatedAt(LocalDateTime.now());
        pointsRecordMapper.insert(record);
    }
}
