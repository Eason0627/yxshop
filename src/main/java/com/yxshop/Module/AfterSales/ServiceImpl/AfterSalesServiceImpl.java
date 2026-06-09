package com.yxshop.Module.AfterSales.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.AfterSales.Dto.AfterSalesApplyDto;
import com.yxshop.Module.AfterSales.Dto.AfterSalesQueryDto;
import com.yxshop.Module.AfterSales.Dto.AfterSalesReviewDto;
import com.yxshop.Module.AfterSales.Entity.AfterSalesRequestEntity;
import com.yxshop.Module.AfterSales.Mapper.AfterSalesRequestMapper;
import com.yxshop.Module.AfterSales.Service.AfterSalesService;
import com.yxshop.Module.AfterSales.Vo.AfterSalesVo;
import com.yxshop.Module.Fulfillment.Entity.FulfillmentEntity;
import com.yxshop.Module.Fulfillment.Mapper.FulfillmentMapper;
import com.yxshop.Module.Inventory.Dto.StockAdjustDto;
import com.yxshop.Module.Inventory.Service.InventoryModuleService;
import com.yxshop.Module.Order.Entity.OrderEntity;
import com.yxshop.Module.Order.Entity.OrderItemEntity;
import com.yxshop.Module.Order.Mapper.OrderItemMapper;
import com.yxshop.Module.Order.Mapper.OrderModuleMapper;
import com.yxshop.Module.Refund.Entity.RefundRecordEntity;
import com.yxshop.Module.Refund.Mapper.RefundRecordMapper;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Primary
@Service("moduleAfterSalesServiceImpl")
public class AfterSalesServiceImpl extends ServiceImpl<AfterSalesRequestMapper, AfterSalesRequestEntity> implements AfterSalesService {

    private final OrderModuleMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ShopModuleMapper shopMapper;
    private final InventoryModuleService inventoryService;
    private final RefundRecordMapper refundRecordMapper;
    private final FulfillmentMapper fulfillmentMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(10, 1);

    public AfterSalesServiceImpl(OrderModuleMapper orderMapper,
                                 OrderItemMapper orderItemMapper,
                                 ShopModuleMapper shopMapper,
                                 InventoryModuleService inventoryService,
                                 RefundRecordMapper refundRecordMapper,
                                 FulfillmentMapper fulfillmentMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.shopMapper = shopMapper;
        this.inventoryService = inventoryService;
        this.refundRecordMapper = refundRecordMapper;
        this.fulfillmentMapper = fulfillmentMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSalesVo apply(Long userId, AfterSalesApplyDto dto) {
        if (dto == null || dto.getOrderId() == null || dto.getOrderItemId() == null) {
            throw new IllegalArgumentException("订单和商品不能为空");
        }
        OrderEntity order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !userId.equals(order.getCustomerId())) {
            throw new IllegalArgumentException("订单不存在");
        }
        // 允许售后的订单状态：
        //   Paid        — 已付款待发货（可申请退款）
        //   Shipped     — 已发货待收货（可申请退款）
        //   PendingReview — 已确认收货待评价（可申请退款/退货/换货）
        //   Completed   — 已完成（可申请退款/退货/换货）
        boolean allowAfterSales = "Paid".equals(order.getOrderStatus())
                || "Shipped".equals(order.getOrderStatus())
                || "PendingReview".equals(order.getOrderStatus())
                || "Completed".equals(order.getOrderStatus());
        if (!allowAfterSales) {
            throw new IllegalArgumentException("当前订单不可申请售后");
        }
        // 未发货/未收货阶段只支持退款，不支持退货退款或换货
        boolean beforeReceipt = "Paid".equals(order.getOrderStatus()) || "Shipped".equals(order.getOrderStatus());
        if (beforeReceipt) {
            String normalizedType = normalizeType(dto.getType());
            if (!"RefundOnly".equals(normalizedType)) {
                throw new IllegalArgumentException("商品未收货前仅支持申请退款，不支持退货退款或换货");
            }
        }
        OrderItemEntity orderItem = orderItemMapper.selectById(dto.getOrderItemId());
        if (orderItem == null || !order.getOrderId().equals(orderItem.getOrderId())) {
            throw new IllegalArgumentException("订单商品不存在");
        }
        int quantity = dto.getQuantity() == null ? orderItem.getQuantity() : dto.getQuantity();
        if (quantity < 1 || quantity > orderItem.getQuantity() - defaultInt(orderItem.getRefundQuantity())) {
            throw new IllegalArgumentException("售后数量不合法");
        }
        AfterSalesRequestEntity request = new AfterSalesRequestEntity();
        request.setId(idWorker.nextId());
        request.setOrderId(order.getOrderId());
        request.setOrderItemId(orderItem.getId());
        request.setUserId(userId);
        request.setShopId(order.getShopId());
        request.setProductId(orderItem.getProductId());
        request.setType(normalizeType(dto.getType()));
        request.setQuantity(quantity);
        request.setAmount(orderItem.getPrice().multiply(BigDecimal.valueOf(quantity)));
        request.setReason(dto.getReason());
        request.setDescription(dto.getDescription());
        request.setImages(dto.getImages());
        request.setReturnCarrierName(dto.getReturnCarrierName());
        request.setReturnTrackingNo(dto.getReturnTrackingNo());
        request.setStatus("Pending");
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        save(request);
        order.setAfterSalesStatus("Processing");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return toVo(request);
    }

    @Override
    public Map<String, Object> list(Long userId, String role, AfterSalesQueryDto queryDto) {
        AfterSalesQueryDto query = queryDto == null ? new AfterSalesQueryDto() : queryDto;
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 100);
        QueryWrapper<AfterSalesRequestEntity> wrapper = new QueryWrapper<>();
        if ("Admin".equals(role)) {
            if (query.getShopId() != null) wrapper.eq("shop_id", query.getShopId());
        } else if ("ShopOwner".equals(role)) {
            Long shopId = query.getShopId() == null ? resolveOwnerShopId(userId) : query.getShopId();
            assertShopManageable(userId, shopId);
            wrapper.eq("shop_id", shopId);
        } else {
            wrapper.eq("user_id", userId);
        }
        if (query.getStatus() != null) wrapper.eq("status", query.getStatus());
        if (query.getType() != null) wrapper.eq("type", query.getType());
        wrapper.orderByDesc("created_at");
        Page<AfterSalesRequestEntity> page = page(new Page<>(pageNum, pageSize), wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        result.put("total", page.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    public AfterSalesVo detail(Long userId, String role, Long id) {
        return toVo(requireAccess(userId, role, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSalesVo review(Long operatorId, String role, AfterSalesReviewDto dto) {
        if (dto == null || dto.getId() == null) {
            throw new IllegalArgumentException("售后ID不能为空");
        }
        AfterSalesRequestEntity request = requireAccess(operatorId, role, dto.getId());
        if (!"Admin".equals(role) && !"ShopOwner".equals(role)) {
            throw new IllegalArgumentException("无权审核售后");
        }
        if (!"Pending".equals(request.getStatus())) {
            throw new IllegalArgumentException("该售后申请已处理");
        }
        String status = normalizeReviewStatus(dto.getStatus());
        request.setStatus(status);
        request.setReviewerId(operatorId);
        request.setReviewRemark(dto.getReviewRemark());
        request.setUpdatedAt(LocalDateTime.now());
        updateById(request);
        if ("Approved".equals(status)) {
            handleApproved(request);
        }
        return toVo(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSalesVo cancel(Long userId, Long id) {
        AfterSalesRequestEntity request = getById(id);
        if (request == null || !userId.equals(request.getUserId())) {
            throw new IllegalArgumentException("售后申请不存在");
        }
        if (!"Pending".equals(request.getStatus())) {
            throw new IllegalArgumentException("当前售后申请不可取消");
        }
        request.setStatus("Cancelled");
        request.setUpdatedAt(LocalDateTime.now());
        updateById(request);
        return toVo(request);
    }

    private void handleApproved(AfterSalesRequestEntity request) {
        OrderItemEntity item = orderItemMapper.selectById(request.getOrderItemId());
        item.setRefundQuantity(defaultInt(item.getRefundQuantity()) + request.getQuantity());
        item.setAfterSalesStatus("Approved");
        if (item.getRefundQuantity() >= item.getQuantity()) {
            item.setItemStatus("Exchange".equals(request.getType()) ? "Exchanged" : "Refunded");
        }
        orderItemMapper.updateById(item);
        if ("RefundOnly".equals(request.getType()) || "ReturnRefund".equals(request.getType())) {
            RefundRecordEntity refund = createRefundRecord(request);
            request.setRefundId(refund.getId());
            updateById(request);
        }
        if ("ReturnRefund".equals(request.getType()) || "Exchange".equals(request.getType())) {
            StockAdjustDto stockDto = new StockAdjustDto();
            stockDto.setProductId(request.getProductId());
            stockDto.setOrderId(request.getOrderId());
            stockDto.setQuantity(request.getQuantity());
            stockDto.setAdjustType("in");
            stockDto.setReason("售后通过返还库存");
            inventoryService.adjustStock(request.getReviewerId(), "Admin", stockDto);
        }
        if ("Exchange".equals(request.getType())) {
            Long exchangeOrderId = createExchangeOrder(request, item);
            request.setExchangeOrderId(exchangeOrderId);
            updateById(request);
        }
        OrderEntity order = orderMapper.selectById(request.getOrderId());
        order.setAfterSalesStatus("Approved");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private AfterSalesRequestEntity requireAccess(Long userId, String role, Long id) {
        AfterSalesRequestEntity request = getById(id);
        if (request == null) {
            throw new IllegalArgumentException("售后申请不存在");
        }
        if ("Admin".equals(role)) return request;
        if ("ShopOwner".equals(role)) {
            assertShopManageable(userId, request.getShopId());
            return request;
        }
        if (!userId.equals(request.getUserId())) {
            throw new IllegalArgumentException("无权访问该售后申请");
        }
        return request;
    }

    private void assertShopManageable(Long userId, Long shopId) {
        ShopEntity shop = shopMapper.selectById(shopId);
        if (shop == null || !userId.equals(shop.getOwnerUserId())) {
            throw new IllegalArgumentException("无权访问该店铺售后");
        }
    }

    private Long resolveOwnerShopId(Long userId) {
        QueryWrapper<ShopEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("owner_user_id", userId).eq("status", "Active").last("LIMIT 1");
        ShopEntity shop = shopMapper.selectOne(wrapper);
        if (shop == null) {
            throw new IllegalArgumentException("当前用户没有可管理店铺");
        }
        return shop.getShopId();
    }

    private String normalizeType(String type) {
        if ("RefundOnly".equals(type) || "ReturnRefund".equals(type) || "Exchange".equals(type)) {
            return type;
        }
        return "RefundOnly";
    }

    private String normalizeReviewStatus(String status) {
        if ("Approved".equalsIgnoreCase(status) || "通过".equals(status)) return "Approved";
        if ("Rejected".equalsIgnoreCase(status) || "拒绝".equals(status)) return "Rejected";
        throw new IllegalArgumentException("审核状态仅支持 Approved/Rejected");
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private AfterSalesVo toVo(AfterSalesRequestEntity request) {
        AfterSalesVo vo = new AfterSalesVo();
        vo.setId(request.getId());
        vo.setOrderId(request.getOrderId());
        vo.setOrderItemId(request.getOrderItemId());
        vo.setUserId(request.getUserId());
        vo.setShopId(request.getShopId());
        vo.setProductId(request.getProductId());
        vo.setType(request.getType());
        vo.setQuantity(request.getQuantity());
        vo.setAmount(request.getAmount());
        vo.setReason(request.getReason());
        vo.setDescription(request.getDescription());
        vo.setImages(request.getImages());
        vo.setReturnCarrierName(request.getReturnCarrierName());
        vo.setReturnTrackingNo(request.getReturnTrackingNo());
        vo.setExchangeOrderId(request.getExchangeOrderId());
        vo.setRefundId(request.getRefundId());
        vo.setStatus(request.getStatus());
        vo.setReviewerId(request.getReviewerId());
        vo.setReviewRemark(request.getReviewRemark());
        vo.setCreatedAt(request.getCreatedAt() == null ? null : request.getCreatedAt().toString());
        vo.setUpdatedAt(request.getUpdatedAt() == null ? null : request.getUpdatedAt().toString());
        return vo;
    }

    private RefundRecordEntity createRefundRecord(AfterSalesRequestEntity request) {
        RefundRecordEntity refund = new RefundRecordEntity();
        refund.setId(idWorker.nextId());
        refund.setRefundNo("RF" + System.currentTimeMillis() + Math.abs(refund.getId() % 100000));
        refund.setAfterSalesId(request.getId());
        refund.setOrderId(request.getOrderId());
        refund.setUserId(request.getUserId());
        refund.setAmount(request.getAmount());
        refund.setStatus("Pending");
        refund.setReason(request.getReason());
        refund.setCreatedAt(LocalDateTime.now());
        refund.setUpdatedAt(LocalDateTime.now());
        refundRecordMapper.insert(refund);
        return refund;
    }

    private Long createExchangeOrder(AfterSalesRequestEntity request, OrderItemEntity originalItem) {
        OrderEntity originalOrder = orderMapper.selectById(request.getOrderId());
        OrderEntity exchangeOrder = new OrderEntity();
        exchangeOrder.setOrderId(idWorker.nextId());
        exchangeOrder.setOrderNumber("EX" + System.currentTimeMillis() + Math.abs(exchangeOrder.getOrderId() % 100000));
        exchangeOrder.setCustomerId(request.getUserId());
        exchangeOrder.setShopId(request.getShopId());
        exchangeOrder.setShopName(originalOrder == null ? null : originalOrder.getShopName());
        exchangeOrder.setGoodsAmount(BigDecimal.ZERO);
        exchangeOrder.setOrderTotal(BigDecimal.ZERO);
        exchangeOrder.setOrderStatus("Paid");
        exchangeOrder.setPaymentStatus("Paid");
        exchangeOrder.setFulfillmentStatus("Unshipped");
        exchangeOrder.setAfterSalesStatus("None");
        exchangeOrder.setCouponAmount(BigDecimal.ZERO);
        exchangeOrder.setActivityDiscount(BigDecimal.ZERO);
        exchangeOrder.setDiscountAmount(BigDecimal.ZERO);
        exchangeOrder.setAddressId(originalOrder == null ? null : originalOrder.getAddressId());
        exchangeOrder.setAddressSnapshot(originalOrder == null ? null : originalOrder.getAddressSnapshot());
        exchangeOrder.setBuyerRemark("换货补发订单，来源售后单：" + request.getId());
        exchangeOrder.setPaidAt(LocalDateTime.now());
        exchangeOrder.setItemsSnapshot(originalItem.getProductId() + ":" + request.getQuantity() + ":0");
        exchangeOrder.setCreateTime(LocalDateTime.now());
        exchangeOrder.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(exchangeOrder);

        OrderItemEntity exchangeItem = new OrderItemEntity();
        exchangeItem.setId(idWorker.nextId());
        exchangeItem.setOrderId(exchangeOrder.getOrderId());
        exchangeItem.setProductId(originalItem.getProductId());
        exchangeItem.setShopId(originalItem.getShopId());
        exchangeItem.setProductName(originalItem.getProductName());
        exchangeItem.setProductImage(originalItem.getProductImage());
        exchangeItem.setSpecsText(originalItem.getSpecsText());
        exchangeItem.setPrice(BigDecimal.ZERO);
        exchangeItem.setQuantity(request.getQuantity());
        exchangeItem.setRefundQuantity(0);
        exchangeItem.setItemStatus("ExchangeReplacement");
        exchangeItem.setAfterSalesStatus("None");
        exchangeItem.setReviewStatus("NoNeedReview");
        exchangeItem.setCreatedAt(LocalDateTime.now());
        orderItemMapper.insert(exchangeItem);

        FulfillmentEntity fulfillment = new FulfillmentEntity();
        fulfillment.setId(idWorker.nextId());
        fulfillment.setOrderId(exchangeOrder.getOrderId());
        fulfillment.setShopId(exchangeOrder.getShopId());
        fulfillment.setStatus("Created");
        fulfillment.setCreatedAt(LocalDateTime.now());
        fulfillment.setUpdatedAt(LocalDateTime.now());
        fulfillmentMapper.insert(fulfillment);
        return exchangeOrder.getOrderId();
    }
}
