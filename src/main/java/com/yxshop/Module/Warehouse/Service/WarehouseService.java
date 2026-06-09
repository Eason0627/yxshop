package com.yxshop.Module.Warehouse.Service;

import com.yxshop.Module.Warehouse.Dto.WarehouseDto;
import com.yxshop.Module.Warehouse.Vo.WarehouseVo;

import java.util.List;

public interface WarehouseService {
    List<WarehouseVo> list(Long operatorId, String operatorRole, Long shopId);
    WarehouseVo saveWarehouse(Long operatorId, String operatorRole, WarehouseDto dto);
    void delete(Long operatorId, String operatorRole, Long id);
    WarehouseVo setDefault(Long operatorId, String operatorRole, Long id);
    WarehouseVo findWarehouseById(Long id);
}
