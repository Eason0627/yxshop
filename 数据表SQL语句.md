```sql
-- 创建用户表 (User)
CREATE TABLE User (
                      id INT AUTO_INCREMENT PRIMARY KEY, -- 用户ID，作为主键
                      username VARCHAR(255) NOT NULL UNIQUE, -- 用户名，必须唯一
                      password VARCHAR(255) NOT NULL, -- 密码哈希
                      email VARCHAR(255), -- 电子邮件
                      phone VARCHAR(20), -- 手机号码
                      role ENUM('Admin', 'ShopOwner', 'Customer') DEFAULT 'Customer', -- 用户角色
                      createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间，自动填充
                      updateTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间，自动更新
                      status ENUM('Active', 'Inactive', 'Invalid') DEFAULT 'Active' -- 用户状态
);

-- 创建店铺表 (Shop)
CREATE TABLE Shop (
                      shop_id INT AUTO_INCREMENT PRIMARY KEY, -- 店铺ID，作为主键
                      shop_name VARCHAR(255) NOT NULL, -- 店铺名称
                      owner_user_id INT, -- 店铺负责人的用户ID，作为外键引用User表
                      phone VARCHAR(255), -- 联系信息
                      location VARCHAR(255), -- 地址
                      registration_date DATE, -- 注册日期
                      shop_description TEXT, -- 店铺描述
                      shop_image VARCHAR(255), -- 店铺图片的URL
                      createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间，自动填充
                      updateTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间，自动更新
                      status ENUM('Active', 'Inactive') DEFAULT 'Active', -- 店铺状态
                      FOREIGN KEY (owner_user_id) REFERENCES User(id) -- 定义外键约束
);

-- 创建品牌表 (Brand)
CREATE TABLE Brand (
                       brand_id INT AUTO_INCREMENT PRIMARY KEY, -- 品牌ID，作为主键
                       brand_name VARCHAR(255) NOT NULL UNIQUE, -- 品牌名称，必须唯一
                       description TEXT, -- 品牌描述
                       logo_url VARCHAR(255), -- 品牌logo的URL
                       createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间，自动填充
                       updateTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间，自动更新
                       status ENUM('Active', 'Inactive') DEFAULT 'Active' -- 品牌状态
);

-- 创建商品分类表 (Category)
CREATE TABLE Category (
                          category_id INT AUTO_INCREMENT PRIMARY KEY, -- 分类ID，作为主键
                          category_name VARCHAR(255) NOT NULL UNIQUE, -- 分类名称，必须唯一
                          parent_category_id INT, -- 父分类ID，如果此分类属于另一个分类，则为其ID
                          description TEXT, -- 分类描述
                          image_url VARCHAR(255), -- 分类图标或图片的URL
                          createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间，自动填充
                          updateTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间，自动更新
                          FOREIGN KEY (parent_category_id) REFERENCES Category(category_id) -- 外键约束，引用自身以支持树状分类结构
);

-- 创建商品信息表 (ProductInfo)
CREATE TABLE ProductInfo (
                             product_id INT AUTO_INCREMENT PRIMARY KEY, -- 商品ID，作为主键
                             product_name VARCHAR(255) NOT NULL, -- 商品名称
                             description TEXT, -- 商品描述
                             brand_id INT, -- 品牌ID，作为外键引用Brand表
                             shop_id INT, -- 店铺ID，作为外键引用Shop表
                             origin VARCHAR(255), -- 原产地
                             material VARCHAR(255), -- 材质
                             size VARCHAR(255), -- 尺寸
                             color VARCHAR(255), -- 颜色
                             weight DECIMAL(10, 2), -- 重量
                             packaging_details VARCHAR(255), -- 包装详情
                             warranty_info VARCHAR(255), -- 保修信息
                             production_date DATE, -- 生产日期
                             expiration_date DATE, -- 保质期
                             category_id INT, -- 商品分类ID，作为外键引用Category表
                             main_image VARCHAR(255), -- 主要图片
                             additional_images JSON, -- 额外图片，使用JSON存储
                             tags JSON, -- 标签，使用JSON存储
                             createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间，自动填充
                             updateTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间，自动更新
                             FOREIGN KEY (brand_id) REFERENCES Brand(brand_id), -- 定义外键约束
                             FOREIGN KEY (shop_id) REFERENCES Shop(shop_id), -- 定义外键约束
                             FOREIGN KEY (category_id) REFERENCES Category(category_id) -- 定义外键约束
);

-- 创建商品销售信息表 (ProductSales)
CREATE TABLE ProductSales (
                              product_id INT PRIMARY KEY, -- 商品ID，作为主键
                              price DECIMAL(10, 2) NOT NULL, -- 商品价格
                              cost_price DECIMAL(10, 2), -- 成本价格
                              stock_quantity INT, -- 库存数量
                              reorder_threshold INT, -- 再订购点
                              sold_quantity INT, -- 已售数量
                              review_count INT, -- 评论数量
                              average_rating DECIMAL(3, 2), -- 平均评分
                              promotion_details VARCHAR(255), -- 促销详情
                              shipping_fee DECIMAL(10, 2), -- 运费
                              sales_status ENUM('Available', 'Out of Stock', 'Pre-order') DEFAULT 'Available', -- 销售状态
                              createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间，自动填充
                              updateTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间，自动更新
                              FOREIGN KEY (product_id) REFERENCES ProductInfo(product_id) -- 定义外键约束
);

-- 创建订单表 (Order)
CREATE TABLE OrderInfo (
                       order_id INT AUTO_INCREMENT PRIMARY KEY, -- 订单唯一标识符，主键
                       customer_id INT NOT NULL, -- 下单客户的用户ID，作为外键引用User表
                       order_total DECIMAL(10, 2) NOT NULL, -- 订单总额
                       order_status ENUM('Pending', 'Confirmed', 'Shipped', 'Delivered', 'Cancelled') NOT NULL DEFAULT 'Pending', -- 订单状态
                       payment_status ENUM('Unpaid', 'Paid', 'PartiallyPaid') NOT NULL DEFAULT 'Unpaid', -- 支付状态
                       shipping_address VARCHAR(255) NOT NULL, -- 发货地址
                       billing_address VARCHAR(255) NOT NULL, -- 账单地址
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间，自动填充
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间，自动更新
                       comments TEXT, -- 订单备注
                       FOREIGN KEY (customer_id) REFERENCES User(id) -- 定义外键约束
);
```

