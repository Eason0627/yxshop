package com.yxshop.Module.Fulfillment.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxshop.Module.Fulfillment.Dto.ShipOrderDto;
import com.yxshop.Module.Fulfillment.Dto.TraceDto;
import com.yxshop.Module.Fulfillment.Entity.FulfillmentEntity;
import com.yxshop.Module.Fulfillment.Vo.FulfillmentVo;

public interface FulfillmentService extends IService<FulfillmentEntity> {
    FulfillmentVo ship(Long operatorId, String role, ShipOrderDto dto);

    FulfillmentVo addTrace(Long operatorId, String role, TraceDto dto);

    FulfillmentVo getByOrder(Long operatorId, String role, Long orderId);
}
