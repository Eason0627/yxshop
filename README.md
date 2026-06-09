# YXShop 后端服务

基于 **Spring Boot 3** 构建的电商平台后端，提供完整的 RESTful API，支持多端前台（H5/小程序）与后台管理系统。

## 技术栈

| 层次 | 技术 |
|------|------|
| 框架 | Spring Boot 3、Spring MVC |
| ORM | MyBatis-Plus |
| 数据库 | MySQL 8 |
| 缓存 | Redis |
| 文件存储 | 阿里云 OSS |
| 短信 | 阿里云 SMS |
| 实时推送 | WebSocket |
| 构建 | Maven |

## 业务模块

```
Module/
├── Auth          # 认证授权（登录/注册/JWT）
├── User          # 用户资料
├── Admin         # 后台管理员
├── Catalog       # 商品目录（SPU/SKU/评价）
├── Shop          # 店铺
├── Cart          # 购物车
├── Order         # 订单
├── Payment       # 支付
├── Fulfillment   # 物流履约
├── Inventory     # 库存
├── Warehouse     # 仓库
├── AfterSales    # 售后申请
├── Refund        # 退款
├── Marketing     # 营销（Banner/活动/优惠券）
├── Points        # 积分账户
├── Content       # 内容社区（帖子/话题）
├── Search        # 搜索
├── Message       # 消息会话/客服工单
├── Notification  # 系统通知（WebSocket）
└── File          # 媒体资产管理
```

## 快速启动

### 环境要求

- JDK 17+
- MySQL 8+
- Redis 6+
- Maven 3.8+

### 配置

修改 `src/main/resources/application.yml`，填写数据库、Redis、OSS 等连接信息。

### 运行

```bash
mvn spring-boot:run
```

### Docker 启动

```bash
docker build -t yxshop-backend .
docker run -p 8080:8080 yxshop-backend
```

或使用项目根目录的 `docker-compose.yml` 一键启动全套服务：

```bash
docker-compose up -d
```

## 数据库初始化

建表 SQL 见 [`数据表SQL语句.md`](数据表SQL语句.md)。

## 接口文档

启动后访问：`http://localhost:8080/swagger-ui/index.html`
