package com.yxshop.Module.Order.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxshop.Module.Order.Dto.OrderCreateDto;
import com.yxshop.Module.Order.Dto.OrderQueryDto;
import com.yxshop.Module.Order.Entity.OrderEntity;
import com.yxshop.Module.Order.Vo.OrderVo;

import java.util.Map;

public interface OrderModuleService extends IService<OrderEntity> {
    Object createOrder(Long userId, OrderCreateDto dto);

    Map<String, Object> listOrders(Long userId, String role, OrderQueryDto queryDto);

    OrderVo getOrderDetail(Long userId, String role, Long orderId);

    OrderVo cancelOrder(Long userId, String role, Long orderId, String reason);

    OrderVo confirmReceive(Long userId, Long orderId);

    OrderVo remindShipment(Long userId, Long orderId);

    OrderVo markPaid(Long orderId, Long operatorId);

    OrderVo shipOrder(Long operatorId, String role, Long orderId, String company, String trackingNo, String remark);

    OrderVo updateAfterSalesStatus(Long operatorId, String role, Long orderId, String afterSalesStatus, String remark);

    /** 平台仲裁：Admin 对争议订单记录仲裁意见 */
    OrderVo arbitrate(Long adminId, Long orderId, String remark);

    /**
     * 删除订单（软删除）。
     * 仅允许删除已取消（Cancelled）或已完成（Completed）的订单。
     * Admin 可删除任意符合条件的订单；ShopOwner 只能删除归属自己店铺的订单。
     */
    void deleteOrder(Long operatorId, String role, Long orderId);
}
