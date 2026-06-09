package com.yxshop.Module.Inventory.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yxshop.Module.Catalog.Entity.AppProductEntity;
import com.yxshop.Module.Catalog.Mapper.AppProductMapper;
import com.yxshop.Module.Inventory.Dto.InventoryQueryDto;
import com.yxshop.Module.Inventory.Dto.StockAdjustDto;
import com.yxshop.Module.Inventory.Dto.StockCheckDto;
import com.yxshop.Module.Inventory.Entity.InventoryEntity;
import com.yxshop.Module.Inventory.Entity.StockRecordEntity;
import com.yxshop.Module.Inventory.Mapper.InventoryModuleMapper;
import com.yxshop.Module.Inventory.Mapper.StockRecordMapper;
import com.yxshop.Module.Inventory.Service.InventoryModuleService;
import com.yxshop.Module.Inventory.Vo.InventoryVo;
import com.yxshop.Module.Shop.Entity.ShopEntity;
import com.yxshop.Module.Shop.Mapper.ShopModuleMapper;
import com.yxshop.Utils.SnowflakeIdWorker;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Primary
@Service
public class InventoryModuleServiceImpl extends ServiceImpl<InventoryModuleMapper, InventoryEntity> implements InventoryModuleService {

    private final AppProductMapper appProductMapper;
    private final ShopModuleMapper shopModuleMapper;
    private final StockRecordMapper stockRecordMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker(4, 1);

    public InventoryModuleServiceImpl(AppProductMapper appProductMapper,
                                      ShopModuleMapper shopModuleMapper,
                                      StockRecordMapper stockRecordMapper) {
        this.appProductMapper = appProductMapper;
        this.shopModuleMapper = shopModuleMapper;
        this.stockRecordMapper = stockRecordMapper;
    }

    @Override
    public InventoryVo getByProductId(Long productId) {
        QueryWrapper<InventoryEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId).last("LIMIT 1");
        InventoryEntity inventory = getOne(wrapper);
        if (inventory == null) {
            AppProductEntity product = appProductMapper.selectById(productId);
            if (product == null) {
                throw new IllegalArgumentException("商品不存在");
            }
            return productOnlyVo(product);
        }
        return toVo(inventory);
    }

    @Override
    public Map<String, Object> listInventory(Long operatorId, String operatorRole, InventoryQueryDto queryDto) {
        InventoryQueryDto query = queryDto == null ? new InventoryQueryDto() : queryDto;
        int pageNum = validPageNum(query.getPageNum());
        int pageSize = validPageSize(query.getPageSize());
        QueryWrapper<InventoryEntity> wrapper = buildQuery(operatorId, operatorRole, query);
        wrapper.orderByDesc("updateTime");
        Page<InventoryEntity> page = page(new Page<>(pageNum, pageSize), wrapper);
        return pageResult(page, pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryVo adjustStock(Long operatorId, String operatorRole, StockAdjustDto adjustDto) {
        if (adjustDto.getProductId() == null) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        if (adjustDto.getQuantity() == null) {
            throw new IllegalArgumentException("调整数量不能为空");
        }
        AppProductEntity product = appProductMapper.selectById(adjustDto.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        assertProductManageable(operatorId, operatorRole, product);

        InventoryEntity inventory = findOrCreateInventory(adjustDto, product);
        int oldStock = inventory.getStockQuantity() == null ? 0 : inventory.getStockQuantity();
        int newStock = calculateStock(oldStock, adjustDto);
        if (newStock < 0) {
            throw new IllegalArgumentException("库存不足，不能调整为负数");
        }
        inventory.setStockQuantity(newStock);
        if (inventory.getReservedStock() == null) {
            inventory.setReservedStock(0);
        }
        if (adjustDto.getSafetyStock() != null) {
            inventory.setSafetyStock(adjustDto.getSafetyStock());
        }
        if (adjustDto.getRestockThreshold() != null) {
            inventory.setRestockThreshold(adjustDto.getRestockThreshold());
        }
        inventory.setLastRestockDate(LocalDateTime.now());
        inventory.setUpdateTime(LocalDateTime.now());
        saveOrUpdate(inventory);
        syncProductStock(product.getId(), newStock);
        writeStockRecord(inventory, adjustDto.getOrderId(), "adjust", adjustDto.getQuantity(), oldStock, newStock, adjustDto.getReason(), operatorId);
        return toVo(inventory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized InventoryVo reserveStock(Long operatorId, String operatorRole, StockAdjustDto adjustDto) {
        return changeReservedStock(operatorId, operatorRole, adjustDto, "reserve");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized InventoryVo deductReservedStock(Long operatorId, String operatorRole, StockAdjustDto adjustDto) {
        return changeReservedStock(operatorId, operatorRole, adjustDto, "deduct_reserved");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized InventoryVo releaseReservedStock(Long operatorId, String operatorRole, StockAdjustDto adjustDto) {
        return changeReservedStock(operatorId, operatorRole, adjustDto, "release_reserved");
    }

    @Override
    public boolean checkStock(StockCheckDto checkDto) {
        if (checkDto.getProductId() == null || checkDto.getQuantity() == null || checkDto.getQuantity() < 1) {
            throw new IllegalArgumentException("商品ID和购买数量不能为空");
        }
        InventoryVo inventory = getByProductId(checkDto.getProductId());
        return inventory.getAvailableStock() != null && inventory.getAvailableStock() >= checkDto.getQuantity();
    }

    @Override
    public Map<String, Object> listWarnings(Long operatorId, String operatorRole, InventoryQueryDto queryDto) {
        InventoryQueryDto query = queryDto == null ? new InventoryQueryDto() : queryDto;
        query.setWarningOnly(true);
        return listInventory(operatorId, operatorRole, query);
    }

    @Override
    public Map<String, Object> listRecords(Long operatorId, String operatorRole, InventoryQueryDto queryDto) {
        InventoryQueryDto query = queryDto == null ? new InventoryQueryDto() : queryDto;
        int pageNum = validPageNum(query.getPageNum());
        int pageSize = validPageSize(query.getPageSize());
        QueryWrapper<StockRecordEntity> wrapper = buildRecordQuery(operatorId, operatorRole, query);
        wrapper.orderByDesc("created_at");
        Page<StockRecordEntity> page = stockRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    private InventoryVo changeReservedStock(Long operatorId, String operatorRole, StockAdjustDto adjustDto, String changeType) {
        validateStockChange(adjustDto);
        AppProductEntity product = appProductMapper.selectById(adjustDto.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if ("Admin".equals(operatorRole) || "ShopOwner".equals(operatorRole)) {
            assertProductManageable(operatorId, operatorRole, product);
        } else if ("deduct_reserved".equals(changeType)) {
            throw new IllegalArgumentException("无权扣减预占库存");
        }
        InventoryEntity inventory = findOrCreateInventory(adjustDto, product);
        int oldStock = inventory.getStockQuantity() == null ? 0 : inventory.getStockQuantity();
        int oldReserved = inventory.getReservedStock() == null ? 0 : inventory.getReservedStock();
        int quantity = adjustDto.getQuantity();
        if ("reserve".equals(changeType)) {
            if (oldStock - oldReserved < quantity) {
                throw new IllegalArgumentException("可用库存不足");
            }
            inventory.setReservedStock(oldReserved + quantity);
        } else if ("deduct_reserved".equals(changeType)) {
            if (oldReserved < quantity || oldStock < quantity) {
                throw new IllegalArgumentException("预占库存不足");
            }
            inventory.setStockQuantity(oldStock - quantity);
            inventory.setReservedStock(oldReserved - quantity);
            syncProductStock(product.getId(), oldStock - quantity);
        } else if ("release_reserved".equals(changeType)) {
            if (oldReserved < quantity) {
                throw new IllegalArgumentException("释放数量超过预占库存");
            }
            inventory.setReservedStock(oldReserved - quantity);
        }
        inventory.setUpdateTime(LocalDateTime.now());
        saveOrUpdate(inventory);
        writeStockRecord(inventory, adjustDto.getOrderId(), changeType, quantity, oldStock, inventory.getStockQuantity(), adjustDto.getReason(), operatorId);
        return toVo(inventory);
    }

    private void validateStockChange(StockAdjustDto adjustDto) {
        if (adjustDto == null || adjustDto.getProductId() == null) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        if (adjustDto.getQuantity() == null || adjustDto.getQuantity() < 1) {
            throw new IllegalArgumentException("数量必须大于0");
        }
    }

    private QueryWrapper<InventoryEntity> buildQuery(Long operatorId, String operatorRole, InventoryQueryDto query) {
        QueryWrapper<InventoryEntity> wrapper = new QueryWrapper<>();
        if (query.getProductId() != null) {
            wrapper.eq("product_id", query.getProductId());
        }
        if (query.getWarehouseId() != null) {
            wrapper.eq("warehouse_id", query.getWarehouseId());
        }
        if (Boolean.TRUE.equals(query.getWarningOnly())) {
            wrapper.apply("stock_quantity <= COALESCE(safety_stock, restock_threshold, 0)");
        }
        if (!"Admin".equals(operatorRole)) {
            Long shopId = query.getShopId() == null ? resolveOwnerShopId(operatorId) : query.getShopId();
            assertShopManageable(operatorId, operatorRole, shopId);
            wrapper.inSql("product_id", "SELECT id FROM product WHERE shop_id = " + shopId.longValue());
        } else if (query.getShopId() != null) {
            wrapper.inSql("product_id", "SELECT id FROM product WHERE shop_id = " + query.getShopId().longValue());
        }
        return wrapper;
    }

    private QueryWrapper<StockRecordEntity> buildRecordQuery(Long operatorId, String operatorRole, InventoryQueryDto query) {
        QueryWrapper<StockRecordEntity> wrapper = new QueryWrapper<>();
        if (query.getProductId() != null) {
            wrapper.eq("product_id", query.getProductId());
        }
        if (query.getWarehouseId() != null) {
            wrapper.eq("warehouse_id", query.getWarehouseId());
        }
        if (!"Admin".equals(operatorRole)) {
            Long shopId = query.getShopId() == null ? resolveOwnerShopId(operatorId) : query.getShopId();
            assertShopManageable(operatorId, operatorRole, shopId);
            wrapper.inSql("product_id", "SELECT id FROM product WHERE shop_id = " + shopId.longValue());
        } else if (query.getShopId() != null) {
            wrapper.inSql("product_id", "SELECT id FROM product WHERE shop_id = " + query.getShopId().longValue());
        }
        return wrapper;
    }

    private InventoryEntity findOrCreateInventory(StockAdjustDto adjustDto, AppProductEntity product) {
        QueryWrapper<InventoryEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", adjustDto.getProductId());
        if (adjustDto.getWarehouseId() != null) {
            wrapper.eq("warehouse_id", adjustDto.getWarehouseId());
        }
        wrapper.last("LIMIT 1");
        InventoryEntity inventory = getOne(wrapper);
        if (inventory != null) {
            return inventory;
        }
        InventoryEntity created = new InventoryEntity();
        created.setInventoryId(idWorker.nextId());
        created.setProductId(product.getId());
        created.setWarehouseId(adjustDto.getWarehouseId());
        created.setStockQuantity(product.getStock() == null ? 0 : product.getStock());
        created.setReservedStock(0);
        created.setSafetyStock(adjustDto.getSafetyStock() == null ? 0 : adjustDto.getSafetyStock());
        created.setRestockThreshold(adjustDto.getRestockThreshold() == null ? 0 : adjustDto.getRestockThreshold());
        created.setCreateTime(LocalDateTime.now());
        created.setUpdateTime(LocalDateTime.now());
        return created;
    }

    private int calculateStock(int oldStock, StockAdjustDto adjustDto) {
        String type = adjustDto.getAdjustType();
        if ("set".equalsIgnoreCase(type)) {
            return adjustDto.getQuantity();
        }
        if ("out".equalsIgnoreCase(type) || "decrease".equalsIgnoreCase(type)) {
            return oldStock - adjustDto.getQuantity();
        }
        return oldStock + adjustDto.getQuantity();
    }

    private void syncProductStock(Long productId, Integer stock) {
        AppProductEntity update = new AppProductEntity();
        update.setId(productId);
        update.setStock(stock);
        update.setUpdatedAt(LocalDateTime.now());
        appProductMapper.updateById(update);
    }

    private void writeStockRecord(InventoryEntity inventory, Long orderId, String changeType, Integer changeQuantity,
                                  Integer beforeQuantity, Integer afterQuantity, String reason, Long operatorId) {
        StockRecordEntity record = new StockRecordEntity();
        record.setId(idWorker.nextId());
        record.setProductId(inventory.getProductId());
        record.setWarehouseId(inventory.getWarehouseId());
        record.setOrderId(orderId);
        record.setChangeType(changeType);
        record.setChangeQuantity(changeQuantity);
        record.setBeforeQuantity(beforeQuantity);
        record.setAfterQuantity(afterQuantity);
        record.setReason(reason);
        record.setOperatorId(operatorId);
        record.setCreatedAt(LocalDateTime.now());
        stockRecordMapper.insert(record);
    }

    private void assertProductManageable(Long operatorId, String operatorRole, AppProductEntity product) {
        if ("Admin".equals(operatorRole)) {
            return;
        }
        assertShopManageable(operatorId, operatorRole, product.getShopId());
    }

    private void assertShopManageable(Long operatorId, String operatorRole, Long shopId) {
        if ("Admin".equals(operatorRole)) {
            return;
        }
        if (shopId == null) {
            throw new IllegalArgumentException("店铺ID不能为空");
        }
        ShopEntity shop = shopModuleMapper.selectById(shopId);
        if (shop == null) {
            throw new IllegalArgumentException("店铺不存在");
        }
        if (!operatorId.equals(shop.getOwnerUserId())) {
            throw new IllegalArgumentException("无权操作其他店铺库存");
        }
    }

    private Long resolveOwnerShopId(Long operatorId) {
        QueryWrapper<ShopEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("owner_user_id", operatorId).eq("status", "Active").last("LIMIT 1");
        ShopEntity shop = shopModuleMapper.selectOne(wrapper);
        if (shop == null) {
            throw new IllegalArgumentException("当前用户没有可管理店铺");
        }
        return shop.getShopId();
    }

    private InventoryVo toVo(InventoryEntity inventory) {
        AppProductEntity product = appProductMapper.selectById(inventory.getProductId());
        InventoryVo vo = new InventoryVo();
        vo.setInventoryId(inventory.getInventoryId());
        vo.setProductId(inventory.getProductId());
        vo.setWarehouseId(inventory.getWarehouseId());
        int stock = inventory.getStockQuantity() == null ? 0 : inventory.getStockQuantity();
        int reserved = inventory.getReservedStock() == null ? 0 : inventory.getReservedStock();
        vo.setStockQuantity(stock);
        vo.setReservedStock(reserved);
        vo.setAvailableStock(Math.max(stock - reserved, 0));
        vo.setSafetyStock(inventory.getSafetyStock());
        vo.setRestockThreshold(inventory.getRestockThreshold());
        vo.setWarning(isWarning(inventory));
        vo.setLastRestockDate(inventory.getLastRestockDate() == null ? null : inventory.getLastRestockDate().toString());
        if (product != null) {
            fillProductAndShop(vo, product);
        }
        return vo;
    }

    private InventoryVo productOnlyVo(AppProductEntity product) {
        InventoryVo vo = new InventoryVo();
        vo.setProductId(product.getId());
        int stock = product.getStock() == null ? 0 : product.getStock();
        vo.setStockQuantity(stock);
        vo.setReservedStock(0);
        vo.setAvailableStock(stock);
        vo.setSafetyStock(0);
        vo.setRestockThreshold(0);
        vo.setWarning(false);
        fillProductAndShop(vo, product);
        return vo;
    }

    private void fillProductAndShop(InventoryVo vo, AppProductEntity product) {
        vo.setProductName(product.getName());
        vo.setProductImage(product.getMainImage());
        vo.setShopId(product.getShopId());
        if (product.getShopId() != null) {
            ShopEntity shop = shopModuleMapper.selectById(product.getShopId());
            if (shop != null) {
                vo.setShopName(shop.getDisplayName() == null ? shop.getShopName() : shop.getDisplayName());
            }
        }
    }

    private boolean isWarning(InventoryEntity inventory) {
        int stock = inventory.getStockQuantity() == null ? 0 : inventory.getStockQuantity();
        int threshold = inventory.getSafetyStock() != null ? inventory.getSafetyStock()
                : inventory.getRestockThreshold() == null ? 0 : inventory.getRestockThreshold();
        return stock <= threshold;
    }

    private Map<String, Object> pageResult(Page<InventoryEntity> page, int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        result.put("total", page.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    private int validPageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int validPageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
    }
}
