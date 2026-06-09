package com.yxshop.Module.Order.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.Cart.Entity.CartItemEntity;
import com.yxshop.Module.Cart.Mapper.CartItemMapper;
import com.yxshop.Module.Catalog.Entity.AppProductEntity;
import com.yxshop.Module.Catalog.Mapper.AppProductMapper;
import com.yxshop.Module.Inventory.Dto.StockAdjustDto;
import com.yxshop.Module.Inventory.Service.InventoryModuleService;
import com.yxshop.Module.Marketing.Dto.PromotionCalculateDto;
import com.yxshop.Module.Marketing.Dto.PromotionItemDto;
import com.yxshop.Module.Marketing.Entity.UserCouponEntity;
import com.yxshop.Module.Marketing.Mapper.UserCouponMapper;
import com.yxshop.Module.Marketing.Service.PromotionCalculationService;
import com.yxshop.Module.Marketing.Vo.PromotionCalculateVo;
import com.yxshop.Module.Fulfillment.Entity.FulfillmentEntity;
import com.yxshop.Module.Fulfillment.Entity.LogisticsTraceEntity;
import com.yxshop.Module.Fulfillment.Mapper.FulfillmentMapper;
import com.yxshop.Module.Fulfillment.Mapper.LogisticsTraceMapper;
import com.yxshop.Module.Notification.Service.NotificationService;
import com.yxshop.Module.Order.Dto.OrderCreateDto;
import com.yxshop.Module.Order.Dto.OrderCreateItemDto;
import com.yxshop.Module.Order.Dto.OrderQueryDto;
import com.yxshop.Module.Order.Entity.OrderEntity;
import com.yxshop.Module.Order.Entity.OrderItemEntity;
import com.yxshop.Module.Order.Mapper.OrderItemMapper;
import com.yxshop.Module.Order.Mapper.OrderModuleMapper;
import com.yxshop.Module.Order.Service.OrderModuleService;
import com.yxshop.Module.Order.Vo.OrderItemVo;
import com.yxshop.Module.Order.Vo.OrderVo;
import com.yxshop.Module.Points.Entity.PointsAccountEntity;
import com.yxshop.Module.Points.Entity.PointsRecordEntity;
import com.yxshop.Module.Points.Mapper.PointsAccountMapper;
import com.yxshop.Module.Points.Mapper.PointsRecordMapper;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Module.User.Entity.AddressEntity;
import com.yxshop.Module.User.Mapper.AddressModuleMapper;
import com.yxshop.Utils.AliOSSUtils;
import com.yxshop.Utils.SnowflakeIdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalTime;

@Service
@Primary
public class OrderModuleServiceImpl extends ServiceImpl<OrderModuleMapper, OrderEntity> implements OrderModuleService {

    private final CartItemMapper cartItemMapper;
    private final AppProductMapper productMapper;
    private final ShopModuleMapper shopMapper;
    private final OrderItemMapper orderItemMapper;
    private final InventoryModuleService inventoryService;
    private final UserCouponMapper userCouponMapper;
    private final PromotionCalculationService promotionCalculationService;
    private final PointsAccountMapper pointsAccountMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final NotificationService notificationService;
    private final FulfillmentMapper fulfillmentMapper;
    private final LogisticsTraceMapper logisticsTraceMapper;
    private final AddressModuleMapper addressMapper;
    private final ObjectMapper objectMapper;
    private final AliOSSUtils aliOSSUtils;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(7, 1);

    public OrderModuleServiceImpl(CartItemMapper cartItemMapper,
                                  AppProductMapper productMapper,
                                  ShopModuleMapper shopMapper,
                                  OrderItemMapper orderItemMapper,
                                  InventoryModuleService inventoryService,
                                  UserCouponMapper userCouponMapper,
                                  PromotionCalculationService promotionCalculationService,
                                  PointsAccountMapper pointsAccountMapper,
                                  PointsRecordMapper pointsRecordMapper,
                                  NotificationService notificationService,
                                  FulfillmentMapper fulfillmentMapper,
                                  LogisticsTraceMapper logisticsTraceMapper,
                                  AddressModuleMapper addressMapper,
                                  ObjectMapper objectMapper,
                                  AliOSSUtils aliOSSUtils) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.shopMapper = shopMapper;
        this.orderItemMapper = orderItemMapper;
        this.inventoryService = inventoryService;
        this.userCouponMapper = userCouponMapper;
        this.promotionCalculationService = promotionCalculationService;
        this.pointsAccountMapper = pointsAccountMapper;
        this.pointsRecordMapper = pointsRecordMapper;
        this.notificationService = notificationService;
        this.fulfillmentMapper = fulfillmentMapper;
        this.logisticsTraceMapper = logisticsTraceMapper;
        this.addressMapper = addressMapper;
        this.objectMapper = objectMapper;
        this.aliOSSUtils = aliOSSUtils;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object createOrder(Long userId, OrderCreateDto dto) {
        List<OrderItemDraft> drafts = resolveDraftItems(userId, dto);
        if (drafts.isEmpty()) {
            throw new IllegalArgumentException("订单商品不能为空");
        }
        Map<Long, List<OrderItemDraft>> byShop = drafts.stream().collect(Collectors.groupingBy(draft -> draft.product.getShopId()));
        if (dto != null && dto.getCouponId() != null && byShop.size() > 1) {
            throw new IllegalArgumentException("使用优惠券时暂不支持跨店铺合并结算");
        }
        List<OrderVo> orders = new ArrayList<>();
        for (Map.Entry<Long, List<OrderItemDraft>> entry : byShop.entrySet()) {
            Long couponId = orders.isEmpty() && dto != null ? dto.getCouponId() : null;
            orders.add(createSingleShopOrder(userId, dto, entry.getKey(), entry.getValue(), couponId));
        }
        clearCartItems(userId, dto);
        return orders.size() == 1 ? orders.get(0) : orders;
    }

    private OrderVo createSingleShopOrder(Long userId, OrderCreateDto dto, Long shopId, List<OrderItemDraft> drafts, Long couponId) {
        OrderEntity order = new OrderEntity();
        order.setOrderId(idWorker.nextId());
        order.setOrderNumber("YX" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + Math.abs(order.getOrderId() % 100000));
        order.setCustomerId(userId);
        order.setShopId(shopId);
        order.setShopName(resolveShopName(shopId));
        order.setOrderStatus("PendingPay");
        order.setPaymentStatus("Unpaid");
        order.setFulfillmentStatus("Unshipped");
        order.setAfterSalesStatus("None");
        Long addrId = dto == null ? null : dto.getAddressId();
        order.setAddressId(addrId);
        order.setAddressSnapshot(buildAddressSnapshot(addrId));
        order.setBuyerRemark(dto == null ? null : dto.getBuyerRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItemEntity> orderItems = new ArrayList<>();
        for (OrderItemDraft draft : drafts) {
            reserveStock(userId, order.getOrderId(), draft.product.getId(), draft.quantity);
            OrderItemEntity item = new OrderItemEntity();
            item.setId(idWorker.nextId());
            item.setOrderId(order.getOrderId());
            item.setProductId(draft.product.getId());
            item.setShopId(draft.product.getShopId());
            item.setProductName(draft.product.getName());
            item.setProductImage(draft.product.getMainImage());
            item.setSpecsText(draft.specsText);
            item.setPrice(draft.product.getPrice());
            item.setQuantity(draft.quantity);
            item.setRefundQuantity(0);
            item.setItemStatus("Normal");
            item.setAfterSalesStatus("None");
            item.setReviewStatus("PendingReview");
            item.setCreatedAt(LocalDateTime.now());
            orderItems.add(item);
            total = total.add(draft.product.getPrice().multiply(BigDecimal.valueOf(draft.quantity)));
        }
        PromotionCalculateVo promotion = promotionCalculationService.calculate(toPromotionDto(userId, couponId, drafts));
        BigDecimal totalDiscount = promotion.getTotalDiscount();
        BigDecimal couponAmount = lockCoupon(userId, couponId, order.getOrderId(), total, promotion);
        order.setCouponId(couponId);
        order.setCouponAmount(couponAmount);
        order.setGoodsAmount(total);
        order.setActivityDiscount(promotion.getActivityDiscount());
        order.setDiscountAmount(totalDiscount);
        order.setOrderTotal(total.subtract(totalDiscount).max(BigDecimal.ZERO));
        order.setItemsSnapshot(buildItemsSnapshot(orderItems));
        save(order);
        for (OrderItemEntity item : orderItems) {
            orderItemMapper.insert(item);
        }
        // 订单已创建不推送通知（用户下单后立即跳支付页，无需额外提醒）
        return toVo(order);
    }

    @Override
    public Map<String, Object> listOrders(Long userId, String role, OrderQueryDto queryDto) {
        OrderQueryDto query = queryDto == null ? new OrderQueryDto() : queryDto;
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 100);
        QueryWrapper<OrderEntity> wrapper = new QueryWrapper<>();
        if (!"Admin".equals(role)) {
            if ("ShopOwner".equals(role)) {
                Long shopId = query.getShopId() == null ? resolveOwnerShopId(userId) : query.getShopId();
                assertShopManageable(userId, shopId);
                wrapper.eq("shop_id", shopId);
            } else {
                wrapper.eq("customer_id", userId);
            }
        } else if (query.getShopId() != null) {
            wrapper.eq("shop_id", query.getShopId());
        }
        applyAppStatus(wrapper, query.getOrderStatus());
        if (query.getPaymentStatus() != null) wrapper.eq("payment_status", query.getPaymentStatus());
        if (query.getFulfillmentStatus() != null) wrapper.eq("fulfillment_status", query.getFulfillmentStatus());
        if (query.getAfterSalesStatus() != null) {
            if ("HasAfterSales".equals(query.getAfterSalesStatus())) {
                wrapper.ne("after_sales_status", "None");
            } else {
                wrapper.eq("after_sales_status", query.getAfterSalesStatus());
            }
        }
        if (query.getKeyword() != null && !query.getKeyword().trim().isEmpty()) {
            String kw = "%" + query.getKeyword().trim() + "%";
            wrapper.and(w -> w.like("order_number", kw).or().like("buyer_remark", kw).or().like("shop_name", kw));
        }
        if (query.getAmountMin() != null) wrapper.ge("order_total", query.getAmountMin());
        if (query.getAmountMax() != null) wrapper.le("order_total", query.getAmountMax());
        if (query.getStartDate() != null && !query.getStartDate().isEmpty()) {
            wrapper.ge("createTime", LocalDateTime.of(LocalDate.parse(query.getStartDate()), LocalTime.MIN));
        }
        if (query.getEndDate() != null && !query.getEndDate().isEmpty()) {
            wrapper.le("createTime", LocalDateTime.of(LocalDate.parse(query.getEndDate()), LocalTime.MAX));
        }
        wrapper.orderByDesc("createTime");
        Page<OrderEntity> page = page(new Page<>(pageNum, pageSize), wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        result.put("total", page.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    public OrderVo getOrderDetail(Long userId, String role, Long orderId) {
        return toVo(requireAccessibleOrder(userId, role, orderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVo cancelOrder(Long userId, String role, Long orderId, String reason) {
        OrderEntity order = requireAccessibleOrder(userId, role, orderId);
        if (!"PendingPay".equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("仅待付款订单可取消");
        }
        for (OrderItemEntity item : listItems(orderId)) {
            releaseStock(userId, orderId, item.getProductId(), item.getQuantity(), reason == null ? "取消订单释放库存" : reason);
            item.setItemStatus("Cancelled");
            orderItemMapper.updateById(item);
        }
        releaseCoupon(order);
        order.setOrderStatus("Cancelled");
        order.setCancelledAt(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        notificationService.send(order.getCustomerId(), "订单已取消", "订单 " + order.getOrderNumber() + " 已取消", "order", orderId);
        return toVo(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVo confirmReceive(Long userId, Long orderId) {
        OrderEntity order = requireAccessibleOrder(userId, "Customer", orderId);
        if (!"Shipped".equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("仅已发货订单可确认收货");
        }
        order.setOrderStatus("PendingReview");
        order.setFulfillmentStatus("Received");
        order.setConfirmedAt(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        notificationService.send(userId, "订单待评价", "订单 " + order.getOrderNumber() + " 已确认收货，请评价已购商品", "order", orderId);
        return toVo(order);
    }

    @Override
    public OrderVo remindShipment(Long userId, Long orderId) {
        OrderEntity order = requireAccessibleOrder(userId, "Customer", orderId);
        if (!"Paid".equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("仅待发货订单可提醒发货");
        }
        ShopEntity shop = shopMapper.selectById(order.getShopId());
        if (shop != null && shop.getOwnerUserId() != null) {
            notificationService.send(shop.getOwnerUserId(), "买家提醒发货", "订单 " + order.getOrderNumber() + " 买家已提醒发货，请及时处理", "order", orderId);
        }
        notificationService.send(userId, "提醒发货已发送", "订单 " + order.getOrderNumber() + " 已提醒商家发货", "order", orderId);
        return toVo(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVo markPaid(Long orderId, Long operatorId) {
        OrderEntity order = getById(orderId);
        if (order == null) throw new IllegalArgumentException("订单不存在");
        if (!"PendingPay".equals(order.getOrderStatus())) return toVo(order);
        for (OrderItemEntity item : listItems(orderId)) {
            deductReserved(operatorId, orderId, item.getProductId(), item.getQuantity());
        }
        useCoupon(order);
        addPayPoints(order);
        order.setOrderStatus("Paid");
        order.setPaymentStatus("Paid");
        order.setPaidAt(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        notificationService.send(order.getCustomerId(), "支付成功", "订单 " + order.getOrderNumber() + " 已支付成功", "order", orderId);
        return toVo(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVo shipOrder(Long operatorId, String role, Long orderId, String company, String trackingNo, String remark) {
        if (company == null || company.isBlank()) throw new IllegalArgumentException("请选择快递公司");
        if (trackingNo == null || trackingNo.isBlank()) throw new IllegalArgumentException("请填写快递单号");
        OrderEntity order = requireAccessibleOrder(operatorId, role, orderId);
        if (!"Paid".equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("仅待发货（已付款）订单可发货");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setOrderStatus("Shipped");
        order.setFulfillmentStatus("Shipped");
        order.setShippingCompany(company);
        order.setTrackingNo(trackingNo);
        order.setShippingRemark(remark);
        order.setShippedAt(now);
        order.setUpdateTime(now);
        updateById(order);

        // 创建 fulfillment 记录（若已存在则更新快递信息）
        QueryWrapper<FulfillmentEntity> fw = new QueryWrapper<>();
        fw.eq("order_id", order.getOrderId()).last("LIMIT 1");
        FulfillmentEntity fulfillment = fulfillmentMapper.selectOne(fw);
        if (fulfillment == null) {
            fulfillment = new FulfillmentEntity();
            fulfillment.setId(idWorker.nextId());
            fulfillment.setOrderId(order.getOrderId());
            fulfillment.setShopId(order.getShopId());
            fulfillment.setCreatedAt(now);
        }
        boolean isNew = fulfillment.getId() == null;
        fulfillment.setCarrierName(company);
        fulfillment.setTrackingNo(trackingNo);
        fulfillment.setStatus("Shipped");
        fulfillment.setShippedAt(now);
        fulfillment.setUpdatedAt(now);
        if (isNew) {
            fulfillmentMapper.insert(fulfillment);
        } else {
            fulfillmentMapper.updateById(fulfillment);
        }

        // 写入初始"已发货"轨迹节点
        LogisticsTraceEntity trace = new LogisticsTraceEntity();
        trace.setId(idWorker.nextId());
        trace.setFulfillmentId(fulfillment.getId());
        trace.setOrderId(order.getOrderId());
        trace.setStatus("Shipped");
        trace.setContent("商家已发货，快递：" + company + "，单号：" + trackingNo);
        trace.setTraceTime(now);
        trace.setCreatedAt(now);
        logisticsTraceMapper.insert(trace);

        notificationService.send(order.getCustomerId(), "订单已发货",
                "订单 " + order.getOrderNumber() + " 已发货，快递：" + company + "，单号：" + trackingNo,
                "order", orderId);
        return toVo(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVo updateAfterSalesStatus(Long operatorId, String role, Long orderId, String afterSalesStatus, String remark) {
        if (afterSalesStatus == null || afterSalesStatus.isBlank()) throw new IllegalArgumentException("售后状态不能为空");
        OrderEntity order = requireAccessibleOrder(operatorId, role, orderId);
        order.setAfterSalesStatus(afterSalesStatus);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        String notifyTitle = "Approved".equals(afterSalesStatus) ? "售后申请已同意" : "售后申请已拒绝";
        String notifyContent = notifyTitle + "，订单：" + order.getOrderNumber() + (remark != null && !remark.isBlank() ? "，备注：" + remark : "");
        notificationService.send(order.getCustomerId(), notifyTitle, notifyContent, "order", orderId);
        return toVo(order);
    }

    private List<OrderItemDraft> resolveDraftItems(Long userId, OrderCreateDto dto) {
        List<OrderItemDraft> drafts = new ArrayList<>();
        if (dto != null && dto.getCartItemIds() != null && !dto.getCartItemIds().isEmpty()) {
            QueryWrapper<CartItemEntity> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", userId).in("id", dto.getCartItemIds()).eq("is_invalid", 0);
            for (CartItemEntity cartItem : cartItemMapper.selectList(wrapper)) {
                drafts.add(toDraft(cartItem.getProductId(), cartItem.getQuantity(), cartItem.getSpecsText()));
            }
        } else if (dto != null && dto.getItems() != null) {
            for (OrderCreateItemDto item : dto.getItems()) {
                drafts.add(toDraft(item.getProductId(), item.getQuantity(), item.getSpecsText()));
            }
        }
        return drafts;
    }

    private OrderItemDraft toDraft(Long productId, Integer quantity, String specsText) {
        if (productId == null || quantity == null || quantity < 1) throw new IllegalArgumentException("商品和数量不能为空");
        AppProductEntity product = productMapper.selectById(productId);
        if (product == null || product.getStatus() == null || product.getStatus() != 1 || !"Approved".equals(product.getAuditStatus())) {
            throw new IllegalArgumentException("商品不存在或未上架");
        }
        OrderItemDraft draft = new OrderItemDraft();
        draft.product = product;
        draft.quantity = quantity;
        draft.specsText = specsText;
        return draft;
    }

    private PromotionCalculateDto toPromotionDto(Long userId, Long couponId, List<OrderItemDraft> drafts) {
        PromotionCalculateDto dto = new PromotionCalculateDto();
        dto.setUserId(userId);
        dto.setCouponId(couponId);
        for (OrderItemDraft draft : drafts) {
            PromotionItemDto item = new PromotionItemDto();
            item.setProductId(draft.product.getId());
            item.setShopId(draft.product.getShopId());
            item.setPrice(draft.product.getPrice());
            item.setQuantity(draft.quantity);
            dto.getItems().add(item);
        }
        return dto;
    }

    private BigDecimal lockCoupon(Long userId, Long couponId, Long orderId, BigDecimal total, PromotionCalculateVo promotion) {
        if (couponId == null) return BigDecimal.ZERO;
        UserCouponEntity coupon = userCouponMapper.selectById(couponId);
        if (coupon == null || !userId.equals(coupon.getUserId()) || !Integer.valueOf(1).equals(coupon.getStatus())) {
            throw new IllegalArgumentException("优惠券不可用");
        }
        if (coupon.getMinAmount() != null && total.compareTo(coupon.getMinAmount()) < 0) {
            throw new IllegalArgumentException("订单金额未达到优惠券使用门槛");
        }
        coupon.setStatus(2);
        coupon.setLockedOrderId(orderId);
        coupon.setLockedAt(LocalDateTime.now());
        coupon.setUpdatedAt(LocalDateTime.now());
        userCouponMapper.updateById(coupon);
        return promotion == null ? (coupon.getValue() == null ? BigDecimal.ZERO : coupon.getValue().min(total)) : promotion.getCouponDiscount();
    }

    private void useCoupon(OrderEntity order) {
        if (order.getCouponId() == null) return;
        UserCouponEntity coupon = userCouponMapper.selectById(order.getCouponId());
        if (coupon == null) return;
        coupon.setStatus(3);
        coupon.setUsedOrderId(order.getOrderId());
        coupon.setUsedAt(LocalDateTime.now());
        coupon.setUpdatedAt(LocalDateTime.now());
        userCouponMapper.updateById(coupon);
    }

    private void releaseCoupon(OrderEntity order) {
        if (order.getCouponId() == null) return;
        UserCouponEntity coupon = userCouponMapper.selectById(order.getCouponId());
        if (coupon == null || !Integer.valueOf(2).equals(coupon.getStatus())) return;
        coupon.setStatus(1);
        coupon.setLockedOrderId(null);
        coupon.setLockedAt(null);
        coupon.setUpdatedAt(LocalDateTime.now());
        userCouponMapper.updateById(coupon);
    }

    private void addPayPoints(OrderEntity order) {
        int points = order.getOrderTotal() == null ? 0 : order.getOrderTotal().intValue();
        if (points <= 0) return;
        PointsAccountEntity account = pointsAccountMapper.selectById(order.getCustomerId());
        if (account == null) {
            account = new PointsAccountEntity();
            account.setUserId(order.getCustomerId());
            account.setCurrentPoints(0);
            account.setTotalEarned(0);
            account.setTotalSpent(0);
            account.setExpireDate(LocalDate.now().plusYears(1));
        }
        int before = account.getCurrentPoints() == null ? 0 : account.getCurrentPoints();
        account.setCurrentPoints(before + points);
        account.setTotalEarned((account.getTotalEarned() == null ? 0 : account.getTotalEarned()) + points);
        account.setUpdatedAt(LocalDateTime.now());
        if (pointsAccountMapper.selectById(order.getCustomerId()) == null) {
            pointsAccountMapper.insert(account);
        } else {
            pointsAccountMapper.updateById(account);
        }
        PointsRecordEntity record = new PointsRecordEntity();
        record.setId(idWorker.nextId());
        record.setUserId(order.getCustomerId());
        record.setChangeType("Earn");
        record.setPoints(points);
        record.setBeforePoints(before);
        record.setAfterPoints(before + points);
        record.setBizType("order");
        record.setBizId(order.getOrderId());
        record.setDescription("支付订单获得积分");
        record.setCreatedAt(LocalDateTime.now());
        pointsRecordMapper.insert(record);
    }

    private void applyAppStatus(QueryWrapper<OrderEntity> wrapper, String status) {
        if (status == null) return;
        if ("PendingPay".equals(status) || "PendingPayment".equals(status)) wrapper.eq("order_status", "PendingPay");
        // 待发货：包含普通已付款订单（Paid）和积分换货直接创建的待发货订单（PendingShipment）
        else if ("PendingShip".equals(status) || "PendingShipment".equals(status)) wrapper.in("order_status", "Paid", "PendingShipment");
        else if ("PendingReceive".equals(status) || "PendingReceipt".equals(status)) wrapper.eq("order_status", "Shipped");
        else if ("PendingReview".equals(status)) wrapper.in("order_status", "PendingReview", "Received");
        else if ("AfterSales".equals(status)) wrapper.ne("after_sales_status", "None");
        else wrapper.eq("order_status", status);
    }

    private void reserveStock(Long userId, Long orderId, Long productId, Integer quantity) {
        StockAdjustDto dto = new StockAdjustDto();
        dto.setProductId(productId);
        dto.setOrderId(orderId);
        dto.setQuantity(quantity);
        dto.setReason("下单预占库存");
        inventoryService.reserveStock(userId, "Customer", dto);
    }

    private void releaseStock(Long userId, Long orderId, Long productId, Integer quantity, String reason) {
        StockAdjustDto dto = new StockAdjustDto();
        dto.setProductId(productId);
        dto.setOrderId(orderId);
        dto.setQuantity(quantity);
        dto.setReason(reason);
        inventoryService.releaseReservedStock(userId, "Customer", dto);
    }

    private void deductReserved(Long operatorId, Long orderId, Long productId, Integer quantity) {
        StockAdjustDto dto = new StockAdjustDto();
        dto.setProductId(productId);
        dto.setOrderId(orderId);
        dto.setQuantity(quantity);
        dto.setReason("支付成功扣减库存");
        inventoryService.deductReservedStock(operatorId, "Admin", dto);
    }

    private void clearCartItems(Long userId, OrderCreateDto dto) {
        if (dto == null || dto.getCartItemIds() == null || dto.getCartItemIds().isEmpty()) return;
        QueryWrapper<CartItemEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).in("id", dto.getCartItemIds());
        cartItemMapper.delete(wrapper);
    }

    private OrderEntity requireAccessibleOrder(Long userId, String role, Long orderId) {
        OrderEntity order = getById(orderId);
        if (order == null) throw new IllegalArgumentException("订单不存在");
        if ("Admin".equals(role)) return order;
        if ("ShopOwner".equals(role)) {
            assertShopManageable(userId, order.getShopId());
            return order;
        }
        if (!userId.equals(order.getCustomerId())) throw new IllegalArgumentException("无权访问该订单");
        return order;
    }

    private void assertShopManageable(Long userId, Long shopId) {
        ShopEntity shop = shopMapper.selectById(shopId);
        if (shop == null || !userId.equals(shop.getOwnerUserId())) throw new IllegalArgumentException("无权访问该店铺订单");
    }

    private Long resolveOwnerShopId(Long userId) {
        QueryWrapper<ShopEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("owner_user_id", userId).eq("status", "Active").last("LIMIT 1");
        ShopEntity shop = shopMapper.selectOne(wrapper);
        if (shop == null) throw new IllegalArgumentException("当前用户没有可管理店铺");
        return shop.getShopId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVo arbitrate(Long adminId, Long orderId, String remark) {
        OrderEntity order = getById(orderId);
        if (order == null) throw new IllegalArgumentException("订单不存在");
        if (remark == null || remark.trim().isEmpty()) throw new IllegalArgumentException("仲裁意见不能为空");
        order.setAdminRemark(remark.trim());
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
        return toVo(order);
    }

    private String resolveShopName(Long shopId) {
        ShopEntity shop = shopMapper.selectById(shopId);
        return shop == null ? null : shop.getDisplayName() == null ? shop.getShopName() : shop.getDisplayName();
    }

    private List<OrderItemEntity> listItems(Long orderId) {
        QueryWrapper<OrderItemEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId);
        return orderItemMapper.selectList(wrapper);
    }

    private OrderVo toVo(OrderEntity order) {
        OrderVo vo = new OrderVo();
        vo.setOrderId(order.getOrderId());
        vo.setOrderNumber(order.getOrderNumber());
        vo.setCustomerId(order.getCustomerId());
        vo.setShopId(order.getShopId());
        vo.setShopName(order.getShopName());
        vo.setGoodsAmount(order.getGoodsAmount());
        vo.setOrderTotal(order.getOrderTotal());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setPaymentStatus(order.getPaymentStatus());
        vo.setFulfillmentStatus(order.getFulfillmentStatus());
        vo.setAfterSalesStatus(order.getAfterSalesStatus());
        vo.setAddressId(order.getAddressId());
        vo.setCouponId(order.getCouponId());
        vo.setCouponAmount(order.getCouponAmount());
        vo.setActivityDiscount(order.getActivityDiscount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setAddressSnapshot(order.getAddressSnapshot());
        vo.setBuyerRemark(order.getBuyerRemark());
        vo.setAdminRemark(order.getAdminRemark());
        vo.setShippingCompany(order.getShippingCompany());
        vo.setTrackingNo(order.getTrackingNo());
        vo.setShippedAt(order.getShippedAt() == null ? null : order.getShippedAt().toString());
        vo.setPaidAt(order.getPaidAt() == null ? null : order.getPaidAt().toString());
        vo.setConfirmedAt(order.getConfirmedAt() == null ? null : order.getConfirmedAt().toString());
        vo.setCancelledAt(order.getCancelledAt() == null ? null : order.getCancelledAt().toString());
        vo.setItems(listItems(order.getOrderId()).stream().map(this::toItemVo).collect(Collectors.toList()));
        vo.setCreateTime(order.getCreateTime() == null ? null : order.getCreateTime().toString());
        vo.setUpdateTime(order.getUpdateTime() == null ? null : order.getUpdateTime().toString());
        return vo;
    }

    private OrderItemVo toItemVo(OrderItemEntity item) {
        OrderItemVo vo = new OrderItemVo();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setShopId(item.getShopId());
        vo.setProductName(item.getProductName());
        vo.setProductImage(resolveImageUrl(item.getProductImage()));
        vo.setSpecsText(item.getSpecsText());
        vo.setPrice(item.getPrice());
        vo.setQuantity(item.getQuantity());
        vo.setRefundQuantity(item.getRefundQuantity());
        vo.setItemStatus(item.getItemStatus());
        vo.setAfterSalesStatus(item.getAfterSalesStatus());
        vo.setReviewStatus(item.getReviewStatus());
        vo.setSubtotal((item.getPrice() == null ? BigDecimal.ZERO : item.getPrice()).multiply(BigDecimal.valueOf(item.getQuantity())));
        return vo;
    }

    /**
     * 将 productImage 字段统一转换为可直接访问的 URL：
     *  - objectKey（无 ://）→ 生成 2 小时预签名 URL
     *  - 已有时效预签名 URL → 提取 objectKey 后重新生成（避免使用过期 URL）
     *  - 外链（非本桶 URL）→ 原样返回
     */
    private String resolveImageUrl(String value) {
        if (value == null || value.isBlank()) return value;
        // 先归一化为 objectKey（若已是预签名 URL 则提取 key，若已是 key 则原样）
        String key = aliOSSUtils.normalizeForStorage(value);
        if (key != null && !key.isBlank() && AliOSSUtils.isObjectKey(key)) {
            String signed = aliOSSUtils.generatePresignedUrl(key, 120);
            return signed != null ? signed : value;
        }
        return value; // 外链保留原样
    }

    private String buildAddressSnapshot(Long addressId) {
        if (addressId == null) return null;
        AddressEntity addr = addressMapper.selectById(addressId);
        if (addr == null) return null;
        java.util.Map<String, Object> snap = new java.util.LinkedHashMap<>();
        snap.put("receiverName", nvl(addr.getReceiverName()));
        snap.put("phone",        nvl(addr.getPhone()));
        snap.put("province",     nvl(addr.getProvince()));
        snap.put("city",         nvl(addr.getCity()));
        snap.put("district",     nvl(addr.getDistrict()));
        snap.put("detail",       nvl(addr.getDetail()));
        snap.put("fullAddress",  nvl(addr.getFullAddress()));
        try { return objectMapper.writeValueAsString(snap); } catch (Exception e) { return null; }
    }

    private static String nvl(String v) { return v == null ? "" : v; }

    private String buildItemsSnapshot(List<OrderItemEntity> items) {
        return items.stream().map(item -> item.getProductId() + ":" + item.getQuantity() + ":" + item.getPrice()).collect(Collectors.joining(","));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long operatorId, String role, Long orderId) {
        OrderEntity order = requireAccessibleOrder(operatorId, role, orderId);
        String status = order.getOrderStatus();
        if (!"Cancelled".equals(status) && !"Completed".equals(status)) {
            throw new IllegalArgumentException("仅已取消或已完成的订单可以删除");
        }
        // MyBatis Plus @TableLogic 会将 updateById 变成软删除 UPDATE is_deleted=1
        order.setIsDeleted(1);
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
    }

    private static class OrderItemDraft {
        private AppProductEntity product;
        private Integer quantity;
        private String specsText;
    }
}
