package com.yxshop.Module.Catalog.Dto;

import lombok.Data;

@Data
public class AppProductQueryDto {
    private Long categoryId;
    private Long shopId;
    private Long brandShopId;
    private String keyword;
    private Integer status;
    private String auditStatus;
    /** 排序字段：sales/price/stock/createTime */
    private String sortField;
    /** 排序方向：asc/desc */
    private String sortOrder;
    /** 兼容旧版：priceAsc/priceDesc/sales */
    private String sortBy;
    /** 最低价格筛选 */
    private java.math.BigDecimal priceMin;
    /** 最高价格筛选 */
    private java.math.BigDecimal priceMax;
    /** 库存预警：normal / low / zero */
    private String stockAlert;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
