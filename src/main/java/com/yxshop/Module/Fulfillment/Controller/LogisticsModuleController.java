package com.yxshop.Module.Fulfillment.Controller;

import com.yxshop.Module.Fulfillment.Service.LogisticsModuleService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物流独立接口预留控制器 — 当前为空壳，尚未实现任何端点。
 *
 * <p>前端实际物流查询走 {@code GET /app/fulfillment/orders/{orderId}}
 * （{@link com.yxshop.Module.Fulfillment.Controller.FulfillmentModuleController}），
 * 功能正常，此控制器暂不对外提供服务。</p>
 *
 * <p>后续如需独立的物流路由（运单查询、快递公司列表等），
 * 可在此类中添加方法并注入 {@code LogisticsModuleService}。</p>
 */
@RestController
@RequestMapping("/app/logistics")
public class LogisticsModuleController {
    private final LogisticsModuleService logisticsModuleService;

    public LogisticsModuleController(LogisticsModuleService logisticsModuleService) {
        this.logisticsModuleService = logisticsModuleService;
    }
}
