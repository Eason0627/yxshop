package com.yxshop.Module.Inventory.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxshop.Module.Inventory.Dto.InventoryQueryDto;
import com.yxshop.Module.Inventory.Dto.StockAdjustDto;
import com.yxshop.Module.Inventory.Dto.StockCheckDto;
import com.yxshop.Module.Inventory.Entity.InventoryEntity;
import com.yxshop.Module.Inventory.Vo.InventoryVo;

import java.util.Map;

public interface InventoryModuleService extends IService<InventoryEntity> {
    InventoryVo getByProductId(Long productId);

    Map<String, Object> listInventory(Long operatorId, String operatorRole, InventoryQueryDto queryDto);

    InventoryVo adjustStock(Long operatorId, String operatorRole, StockAdjustDto adjustDto);

    InventoryVo reserveStock(Long operatorId, String operatorRole, StockAdjustDto adjustDto);

    InventoryVo deductReservedStock(Long operatorId, String operatorRole, StockAdjustDto adjustDto);

    InventoryVo releaseReservedStock(Long operatorId, String operatorRole, StockAdjustDto adjustDto);

    boolean checkStock(StockCheckDto checkDto);

    Map<String, Object> listWarnings(Long operatorId, String operatorRole, InventoryQueryDto queryDto);

    Map<String, Object> listRecords(Long operatorId, String operatorRole, InventoryQueryDto queryDto);
}
