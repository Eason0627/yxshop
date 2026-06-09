package com.yxshop.Module.Fulfillment.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.Fulfillment.Dto.ShipOrderDto;
import com.yxshop.Module.Fulfillment.Dto.TraceDto;
import com.yxshop.Module.Fulfillment.Entity.FulfillmentEntity;
import com.yxshop.Module.Fulfillment.Entity.LogisticsTraceEntity;
import com.yxshop.Module.Fulfillment.Mapper.FulfillmentMapper;
import com.yxshop.Module.Fulfillment.Mapper.LogisticsTraceMapper;
import com.yxshop.Module.Fulfillment.Service.FulfillmentService;
import com.yxshop.Module.Fulfillment.Vo.FulfillmentVo;
import com.yxshop.Module.Fulfillment.Vo.LogisticsTraceVo;
import com.yxshop.Module.Order.Entity.OrderEntity;
import com.yxshop.Module.Order.Mapper.OrderModuleMapper;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Primary
@Service
public class FulfillmentServiceImpl extends ServiceImpl<FulfillmentMapper, FulfillmentEntity> implements FulfillmentService {

    private final OrderModuleMapper orderMapper;
    private final ShopModuleMapper shopMapper;
    private final LogisticsTraceMapper traceMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(9, 1);

    public FulfillmentServiceImpl(OrderModuleMapper orderMapper, ShopModuleMapper shopMapper, LogisticsTraceMapper traceMapper) {
        this.orderMapper = orderMapper;
        this.shopMapper = shopMapper;
        this.traceMapper = traceMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FulfillmentVo ship(Long operatorId, String role, ShipOrderDto dto) {
        if (dto == null || dto.getOrderId() == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        OrderEntity order = requireOrderAccess(operatorId, role, dto.getOrderId(), true);
        if (!"Paid".equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("仅已支付订单可发货");
        }
        FulfillmentEntity fulfillment = findByOrder(order.getOrderId());
        if (fulfillment == null) {
            fulfillment = new FulfillmentEntity();
            fulfillment.setId(idWorker.nextId());
            fulfillment.setOrderId(order.getOrderId());
            fulfillment.setShopId(order.getShopId());
            fulfillment.setCreatedAt(LocalDateTime.now());
        }
        fulfillment.setCarrierName(dto.getCarrierName());
        fulfillment.setTrackingNo(dto.getTrackingNo());
        fulfillment.setStatus("Shipped");
        fulfillment.setShippedAt(LocalDateTime.now());
        fulfillment.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(fulfillment);
        addTraceInternal(fulfillment, "Shipped", "商家已发货", dto.getLocation());
        order.setOrderStatus("Shipped");
        order.setFulfillmentStatus("Shipped");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return toVo(fulfillment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FulfillmentVo addTrace(Long operatorId, String role, TraceDto dto) {
        if (dto == null || dto.getOrderId() == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        requireOrderAccess(operatorId, role, dto.getOrderId(), true);
        FulfillmentEntity fulfillment = findByOrder(dto.getOrderId());
        if (fulfillment == null) {
            throw new IllegalArgumentException("发货记录不存在，请先完成发货操作");
        }
        addTraceInternal(fulfillment, dto.getStatus(), dto.getContent(), dto.getLocation(),
                dto.getLat(), dto.getLng(), dto.getTraceTime());
        if ("Received".equals(dto.getStatus())) {
            fulfillment.setStatus("Received");
            fulfillment.setReceivedAt(LocalDateTime.now());
            fulfillment.setUpdatedAt(LocalDateTime.now());
            updateById(fulfillment);
        }
        return toVo(fulfillment);
    }

    @Override
    public FulfillmentVo getByOrder(Long operatorId, String role, Long orderId) {
        requireOrderAccess(operatorId, role, orderId, false);
        FulfillmentEntity fulfillment = findByOrder(orderId);
        if (fulfillment == null) {
            throw new IllegalArgumentException("物流记录不存在");
        }
        return toVo(fulfillment);
    }

    private OrderEntity requireOrderAccess(Long operatorId, String role, Long orderId, boolean manageRequired) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if ("Admin".equals(role)) {
            return order;
        }
        if ("ShopOwner".equals(role)) {
            ShopEntity shop = shopMapper.selectById(order.getShopId());
            if (shop == null || !operatorId.equals(shop.getOwnerUserId())) {
                throw new IllegalArgumentException("无权操作该订单物流");
            }
            return order;
        }
        if (manageRequired || !operatorId.equals(order.getCustomerId())) {
            throw new IllegalArgumentException("无权访问该订单物流");
        }
        return order;
    }

    private FulfillmentEntity findByOrder(Long orderId) {
        QueryWrapper<FulfillmentEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId).last("LIMIT 1");
        return getOne(wrapper);
    }

    private void addTraceInternal(FulfillmentEntity fulfillment, String status, String content, String location) {
        addTraceInternal(fulfillment, status, content, location, null, null, null);
    }

    private void addTraceInternal(FulfillmentEntity fulfillment, String status, String content,
                                   String location, Double lat, Double lng, String traceTimeStr) {
        LogisticsTraceEntity trace = new LogisticsTraceEntity();
        trace.setId(idWorker.nextId());
        trace.setFulfillmentId(fulfillment.getId());
        trace.setOrderId(fulfillment.getOrderId());
        trace.setStatus(status == null ? fulfillment.getStatus() : status);
        trace.setContent(content);
        trace.setLocation(location);
        trace.setLat(lat);
        trace.setLng(lng);
        LocalDateTime traceTime;
        if (traceTimeStr != null && !traceTimeStr.isBlank()) {
            try {
                traceTime = LocalDateTime.parse(traceTimeStr);
            } catch (Exception e) {
                traceTime = LocalDateTime.now();
            }
        } else {
            traceTime = LocalDateTime.now();
        }
        trace.setTraceTime(traceTime);
        trace.setCreatedAt(LocalDateTime.now());
        traceMapper.insert(trace);
    }

    private FulfillmentVo toVo(FulfillmentEntity fulfillment) {
        FulfillmentVo vo = new FulfillmentVo();
        vo.setId(fulfillment.getId());
        vo.setOrderId(fulfillment.getOrderId());
        vo.setShopId(fulfillment.getShopId());
        vo.setCarrierName(fulfillment.getCarrierName());
        vo.setTrackingNo(fulfillment.getTrackingNo());
        vo.setStatus(fulfillment.getStatus());
        vo.setShippedAt(fulfillment.getShippedAt() == null ? null : fulfillment.getShippedAt().toString());
        vo.setReceivedAt(fulfillment.getReceivedAt() == null ? null : fulfillment.getReceivedAt().toString());
        QueryWrapper<LogisticsTraceEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("fulfillment_id", fulfillment.getId()).orderByDesc("trace_time").orderByDesc("id");
        vo.setTraces(traceMapper.selectList(wrapper).stream().map(this::toTraceVo).collect(Collectors.toList()));
        return vo;
    }

    private LogisticsTraceVo toTraceVo(LogisticsTraceEntity trace) {
        LogisticsTraceVo vo = new LogisticsTraceVo();
        vo.setId(trace.getId());
        vo.setStatus(trace.getStatus());
        vo.setContent(trace.getContent());
        vo.setLocation(trace.getLocation());
        vo.setLat(trace.getLat());
        vo.setLng(trace.getLng());
        vo.setTraceTime(trace.getTraceTime() == null ? null : trace.getTraceTime().toString());
        return vo;
    }
}
