package com.yxshop.Module.Cart.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.Cart.Dto.CartItemDto;
import com.yxshop.Module.Cart.Dto.CartSelectionDto;
import com.yxshop.Module.Cart.Entity.CartItemEntity;
import com.yxshop.Module.Cart.Mapper.CartItemMapper;
import com.yxshop.Module.Cart.Service.CartItemService;
import com.yxshop.Module.Cart.Vo.CartItemVo;
import com.yxshop.Module.Cart.Vo.CartShopVo;
import com.yxshop.Module.Cart.Vo.CartSummaryVo;
import com.yxshop.Module.Catalog.Entity.AppProductEntity;
import com.yxshop.Module.Catalog.Mapper.AppProductMapper;
import com.yxshop.Module.Inventory.Service.InventoryModuleService;
import com.yxshop.Module.Inventory.Vo.InventoryVo;
import com.yxshop.Module.Marketing.Dto.PromotionCalculateDto;
import com.yxshop.Module.Marketing.Dto.PromotionItemDto;
import com.yxshop.Module.Marketing.Entity.UserCouponEntity;
import com.yxshop.Module.Marketing.Mapper.UserCouponMapper;
import com.yxshop.Module.Marketing.Service.PromotionCalculationService;
import com.yxshop.Module.Marketing.Vo.PromotionCalculateVo;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Primary
@Service
public class CartItemServiceImpl extends ServiceImpl<CartItemMapper, CartItemEntity> implements CartItemService {

    private final AppProductMapper productMapper;
    private final ShopModuleMapper shopMapper;
    private final InventoryModuleService inventoryService;
    private final UserCouponMapper userCouponMapper;
    private final PromotionCalculationService promotionCalculationService;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(6, 1);

    public CartItemServiceImpl(AppProductMapper productMapper,
                               ShopModuleMapper shopMapper,
                               InventoryModuleService inventoryService,
                               UserCouponMapper userCouponMapper,
                               PromotionCalculationService promotionCalculationService) {
        this.productMapper = productMapper;
        this.shopMapper = shopMapper;
        this.inventoryService = inventoryService;
        this.userCouponMapper = userCouponMapper;
        this.promotionCalculationService = promotionCalculationService;
    }

    @Override
    public CartSummaryVo listCart(Long userId) {
        QueryWrapper<CartItemEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("updated_at");
        return buildSummary(userId, list(wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartSummaryVo addItem(Long userId, CartItemDto dto) {
        if (dto == null || dto.getProductId() == null) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        int quantity = dto.getQuantity() == null || dto.getQuantity() < 1 ? 1 : dto.getQuantity();
        AppProductEntity product = requireAvailableProduct(dto.getProductId());
        InventoryVo inventory = inventoryService.getByProductId(product.getId());
        if (inventory.getAvailableStock() < quantity) {
            throw new IllegalArgumentException("库存不足");
        }
        QueryWrapper<CartItemEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("product_id", product.getId());
        if (dto.getSpecsText() == null) {
            wrapper.isNull("specs_text");
        } else {
            wrapper.eq("specs_text", dto.getSpecsText());
        }
        wrapper.last("LIMIT 1");
        CartItemEntity item = getOne(wrapper);
        if (item == null) {
            item = new CartItemEntity();
            item.setId(idWorker.nextId());
            item.setUserId(userId);
            item.setProductId(product.getId());
            item.setQuantity(quantity);
            item.setSelected(dto.getSelected() == null ? 1 : dto.getSelected());
            item.setCreatedAt(LocalDateTime.now());
        } else {
            item.setQuantity(item.getQuantity() + quantity);
            if (item.getQuantity() > inventory.getAvailableStock()) {
                throw new IllegalArgumentException("库存不足");
            }
        }
        fillSnapshot(item, product, dto.getSpecsText(), inventory);
        item.setUpdatedAt(LocalDateTime.now());
        saveOrUpdate(item);
        return listCart(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartSummaryVo updateItem(Long userId, Long itemId, CartItemDto dto) {
        CartItemEntity item = requireOwnedItem(userId, itemId);
        if (dto.getQuantity() != null) {
            if (dto.getQuantity() < 1) {
                throw new IllegalArgumentException("数量必须大于0");
            }
            InventoryVo inventory = inventoryService.getByProductId(item.getProductId());
            if (inventory.getAvailableStock() < dto.getQuantity()) {
                throw new IllegalArgumentException("库存不足");
            }
            item.setQuantity(dto.getQuantity());
            item.setStock(inventory.getAvailableStock());
        }
        if (dto.getSelected() != null) {
            item.setSelected(dto.getSelected() == 1 ? 1 : 0);
        }
        item.setUpdatedAt(LocalDateTime.now());
        updateById(item);
        return listCart(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartSummaryVo updateSelection(Long userId, CartSelectionDto dto) {
        if (dto == null || dto.getCartItemIds() == null || dto.getCartItemIds().isEmpty()) {
            throw new IllegalArgumentException("请选择购物车项");
        }
        int selected = dto.getSelected() != null && dto.getSelected() == 1 ? 1 : 0;
        for (Long itemId : dto.getCartItemIds()) {
            CartItemEntity item = requireOwnedItem(userId, itemId);
            item.setSelected(selected);
            item.setUpdatedAt(LocalDateTime.now());
            updateById(item);
        }
        return listCart(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartSummaryVo deleteItems(Long userId, List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return listCart(userId);
        }
        QueryWrapper<CartItemEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).in("id", itemIds);
        remove(wrapper);
        return listCart(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartSummaryVo clearInvalid(Long userId) {
        QueryWrapper<CartItemEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("is_invalid", 1);
        remove(wrapper);
        return listCart(userId);
    }

    @Override
    public CartSummaryVo selectedSettlement(Long userId) {
        QueryWrapper<CartItemEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("selected", 1).eq("is_invalid", 0).orderByDesc("updated_at");
        return buildSummary(userId, list(wrapper));
    }

    private AppProductEntity requireAvailableProduct(Long productId) {
        AppProductEntity product = productMapper.selectById(productId);
        if (product == null || product.getStatus() == null || product.getStatus() != 1 || !"Approved".equals(product.getAuditStatus())) {
            throw new IllegalArgumentException("商品不存在或未上架");
        }
        return product;
    }

    private CartItemEntity requireOwnedItem(Long userId, Long itemId) {
        CartItemEntity item = getById(itemId);
        if (item == null || !userId.equals(item.getUserId())) {
            throw new IllegalArgumentException("购物车项不存在");
        }
        return item;
    }

    private void fillSnapshot(CartItemEntity item, AppProductEntity product, String specsText, InventoryVo inventory) {
        item.setShopId(product.getShopId());
        item.setShopName(resolveShopName(product.getShopId()));
        item.setProductName(product.getName());
        item.setPrice(product.getPrice());
        item.setOriginalPrice(product.getOriginalPrice());
        item.setImage(product.getMainImage());
        item.setSpecsText(specsText);
        item.setStock(inventory.getAvailableStock());
        item.setIsInvalid(0);
    }

    private String resolveShopName(Long shopId) {
        if (shopId == null) {
            return null;
        }
        ShopEntity shop = shopMapper.selectById(shopId);
        if (shop == null) {
            return null;
        }
        return shop.getDisplayName() == null ? shop.getShopName() : shop.getDisplayName();
    }

    private CartSummaryVo buildSummary(Long userId, List<CartItemEntity> items) {
        CartSummaryVo summary = new CartSummaryVo();
        Map<Long, CartShopVo> shopMap = new HashMap<>();
        for (CartItemEntity item : items) {
            refreshItemStatus(item);
            CartItemVo vo = toVo(item);
            CartShopVo shop = shopMap.computeIfAbsent(item.getShopId(), key -> {
                CartShopVo created = new CartShopVo();
                created.setShopId(item.getShopId());
                created.setShopName(item.getShopName());
                return created;
            });
            shop.getItems().add(vo);
            summary.setTotalCount(summary.getTotalCount() + item.getQuantity());
            if (Integer.valueOf(1).equals(item.getSelected()) && !Integer.valueOf(1).equals(item.getIsInvalid())) {
                summary.setSelectedCount(summary.getSelectedCount() + item.getQuantity());
                summary.setSelectedAmount(summary.getSelectedAmount().add(vo.getSubtotal()));
                shop.setSelectedCount(shop.getSelectedCount() + item.getQuantity());
                shop.setSelectedAmount(shop.getSelectedAmount().add(vo.getSubtotal()));
            }
        }
        summary.setShops(shopMap.values().stream().collect(Collectors.toList()));
        PromotionCalculateVo promotion = promotionCalculationService.calculate(toPromotionDto(userId, summary));
        summary.setAvailableCoupons(promotion.getAvailableCoupons());
        summary.setPromotionDiscounts(promotion.getDiscounts());
        summary.setDiscountAmount(promotion.getTotalDiscount());
        summary.setPayAmount(promotion.getPayAmount());
        return summary;
    }

    private PromotionCalculateDto toPromotionDto(Long userId, CartSummaryVo summary) {
        PromotionCalculateDto dto = new PromotionCalculateDto();
        dto.setUserId(userId);
        for (CartShopVo shop : summary.getShops()) {
            for (CartItemVo item : shop.getItems()) {
                if (!Integer.valueOf(1).equals(item.getSelected()) || Integer.valueOf(1).equals(item.getInvalid())) {
                    continue;
                }
                PromotionItemDto promotionItem = new PromotionItemDto();
                promotionItem.setProductId(item.getProductId());
                promotionItem.setShopId(item.getShopId());
                promotionItem.setPrice(item.getPrice());
                promotionItem.setQuantity(item.getQuantity());
                dto.getItems().add(promotionItem);
            }
        }
        return dto;
    }

    private List<UserCouponEntity> findAvailableCoupons(Long userId, BigDecimal amount) {
        QueryWrapper<UserCouponEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("status", 1)
                .and(item -> item.isNull("min_amount").or().le("min_amount", amount))
                .orderByDesc("value");
        return userCouponMapper.selectList(wrapper);
    }

    private void refreshItemStatus(CartItemEntity item) {
        AppProductEntity product = productMapper.selectById(item.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1 || !"Approved".equals(product.getAuditStatus())) {
            item.setIsInvalid(1);
            return;
        }
        InventoryVo inventory = inventoryService.getByProductId(item.getProductId());
        item.setStock(inventory.getAvailableStock());
        item.setIsInvalid(inventory.getAvailableStock() < item.getQuantity() ? 1 : 0);
    }

    private CartItemVo toVo(CartItemEntity item) {
        CartItemVo vo = new CartItemVo();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setShopId(item.getShopId());
        vo.setShopName(item.getShopName());
        vo.setProductName(item.getProductName());
        vo.setPrice(item.getPrice());
        vo.setOriginalPrice(item.getOriginalPrice());
        vo.setImage(item.getImage());
        vo.setSpecsText(item.getSpecsText());
        vo.setQuantity(item.getQuantity());
        vo.setSelected(item.getSelected());
        vo.setStock(item.getStock());
        vo.setAvailableStock(item.getStock());
        vo.setInvalid(item.getIsInvalid());
        BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
        vo.setSubtotal(price.multiply(BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity())));
        return vo;
    }
}
