/*
 Navicat Premium Data Transfer

 Source Server         : Local_MySQL
 Source Server Type    : MySQL
 Source Server Version : 80034
 Source Host           : localhost:3306
 Source Schema         : yxshop

 Target Server Type    : MySQL
 Target Server Version : 80034
 File Encoding         : 65001

 Date: 15/07/2024 16:37:27
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for address
-- ----------------------------
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address`  (
  `address_id` int NOT NULL AUTO_INCREMENT COMMENT '收货地址ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `recipient_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '收件人姓名',
  `street_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '街道地址',
  `city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '城市',
  `state_province` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '州或省份',
  `postal_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '邮政编码',
  `country` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '国家',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '联系电话',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_default` tinyint(1) NULL DEFAULT 0 COMMENT '是否为默认收货地址',
  PRIMARY KEY (`address_id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `address_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of address
-- ----------------------------

-- ----------------------------
-- Table structure for after_sales
-- ----------------------------
DROP TABLE IF EXISTS `after_sales`;
CREATE TABLE `after_sales`  (
  `asr_id` int NOT NULL AUTO_INCREMENT COMMENT '售后记录唯一标识符',
  `order_id` int NOT NULL COMMENT '订单ID',
  `customer_id` int NOT NULL COMMENT '客户ID',
  `product_id` int NOT NULL COMMENT '商品ID',
  `issue_type` enum('Return','Exchange','Repair','Complaint','Consultation') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '售后问题类型: 退货、换货、维修、投诉或咨询',
  `issue_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '问题描述',
  `status` enum('Pending','InProcess','Resolved','Rejected') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'Pending' COMMENT '请求状态: 待处理、处理中、已解决或已拒绝',
  `resolved_by_user_id` int NULL DEFAULT NULL COMMENT '解决售后问题的员工ID',
  `resolution_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '解决方案或拒绝理由',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`asr_id`) USING BTREE,
  INDEX `order_id`(`order_id` ASC) USING BTREE,
  INDEX `customer_id`(`customer_id` ASC) USING BTREE,
  INDEX `product_id`(`product_id` ASC) USING BTREE,
  INDEX `resolved_by_user_id`(`resolved_by_user_id` ASC) USING BTREE,
  CONSTRAINT `after_sales_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `order_info` (`order_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `after_sales_ibfk_2` FOREIGN KEY (`customer_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `after_sales_ibfk_3` FOREIGN KEY (`product_id`) REFERENCES `product_info` (`product_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `after_sales_ibfk_4` FOREIGN KEY (`resolved_by_user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of after_sales
-- ----------------------------

-- ----------------------------
-- Table structure for brand
-- ----------------------------
DROP TABLE IF EXISTS `brand`;
CREATE TABLE `brand`  (
  `brand_id` int NOT NULL AUTO_INCREMENT COMMENT ' 品牌ID，作为主键',
  `brand_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '品牌名称，必须唯一',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT ' 品牌描述',
  `logo_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '品牌logo的URL',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，自动填充',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，自动更新',
  `status` enum('Active','Inactive') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'Active' COMMENT '品牌状态',
  PRIMARY KEY (`brand_id`) USING BTREE,
  UNIQUE INDEX `brand_name`(`brand_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of brand
-- ----------------------------

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `category_id` int NOT NULL AUTO_INCREMENT COMMENT '分类ID，作为主键',
  `category_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '分类名称，必须唯一',
  `parent_category_id` int NULL DEFAULT NULL COMMENT '父分类ID，如果此分类属于另一个分类，则为其ID',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '分类描述',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '分类图标或图片的URL',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，自动填充',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，自动更新',
  PRIMARY KEY (`category_id`) USING BTREE,
  UNIQUE INDEX `category_name`(`category_name` ASC) USING BTREE,
  INDEX `parent_category_id`(`parent_category_id` ASC) USING BTREE,
  CONSTRAINT `category_ibfk_1` FOREIGN KEY (`parent_category_id`) REFERENCES `category` (`category_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of category
-- ----------------------------

-- ----------------------------
-- Table structure for comments
-- ----------------------------
DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments`  (
  `comment_id` int NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `user_id` int NOT NULL COMMENT '发表评论的用户ID',
  `product_id` int NULL DEFAULT NULL COMMENT '产品ID',
  `post_id` int NULL DEFAULT NULL COMMENT '帖子ID',
  `parent_comment_id` int NULL DEFAULT NULL COMMENT '父评论ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '评论的内容',
  `rating` int NULL DEFAULT NULL COMMENT '评论的评分，范围通常是1到5',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论的创建时间',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '评论的最后更新时间',
  PRIMARY KEY (`comment_id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `product_id`(`product_id` ASC) USING BTREE,
  INDEX `post_id`(`post_id` ASC) USING BTREE,
  INDEX `parent_comment_id`(`parent_comment_id` ASC) USING BTREE,
  CONSTRAINT `comments_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `comments_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `product_info` (`product_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `comments_ibfk_3` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `comments_ibfk_4` FOREIGN KEY (`parent_comment_id`) REFERENCES `comments` (`comment_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of comments
-- ----------------------------

-- ----------------------------
-- Table structure for inventory
-- ----------------------------
DROP TABLE IF EXISTS `inventory`;
CREATE TABLE `inventory`  (
  `inventory_id` int NOT NULL AUTO_INCREMENT COMMENT '库存记录唯一标识符',
  `product_id` int NOT NULL COMMENT '商品ID',
  `warehouse_id` int NOT NULL COMMENT '仓库ID',
  `stock_quantity` int NOT NULL COMMENT '库存数量',
  `safety_stock` int NOT NULL COMMENT '安全库存量',
  `last_restock_date` date NULL DEFAULT NULL COMMENT '上次补货日期',
  `restock_threshold` int NOT NULL COMMENT '补货阈值',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`inventory_id`) USING BTREE,
  INDEX `product_id`(`product_id` ASC) USING BTREE,
  INDEX `warehouse_id`(`warehouse_id` ASC) USING BTREE,
  CONSTRAINT `inventory_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `product_info` (`product_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `inventory_ibfk_2` FOREIGN KEY (`warehouse_id`) REFERENCES `warehouse` (`warehouse_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of inventory
-- ----------------------------

-- ----------------------------
-- Table structure for order_info
-- ----------------------------
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info`  (
  `order_id` int NOT NULL AUTO_INCREMENT COMMENT '订单唯一标识符',
  `customer_id` int NOT NULL COMMENT '下单客户的用户ID',
  `order_total` decimal(10, 2) NOT NULL COMMENT '订单总额',
  `order_status` enum('Pending','Confirmed','Shipped','Delivered','Cancelled') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'Pending' COMMENT '订单状态',
  `payment_status` enum('Unpaid','Paid','PartiallyPaid') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'Unpaid' COMMENT '支付状态',
  `shipping_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '发货地址',
  `billing_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '账单地址',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `comments` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '订单备注',
  PRIMARY KEY (`order_id`) USING BTREE,
  INDEX `customer_id`(`customer_id` ASC) USING BTREE,
  CONSTRAINT `order_info_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order_info
-- ----------------------------

-- ----------------------------
-- Table structure for posts
-- ----------------------------
DROP TABLE IF EXISTS `posts`;
CREATE TABLE `posts`  (
  `post_id` int NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `user_id` int NOT NULL COMMENT '发布文章的用户ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '文章标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '文章内容',
  `category_id` int NULL DEFAULT NULL COMMENT '文章所属的类别ID',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `status` enum('Draft','Published','Archived') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'Draft' COMMENT '文章状态：草稿、已发布、已归档',
  `views` int NULL DEFAULT 0 COMMENT '浏览次数',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (`post_id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `category_id`(`category_id` ASC) USING BTREE,
  CONSTRAINT `posts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `posts_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `pots_category` (`category_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of posts
-- ----------------------------

-- ----------------------------
-- Table structure for pots_category
-- ----------------------------
DROP TABLE IF EXISTS `pots_category`;
CREATE TABLE `pots_category`  (
  `category_id` int NOT NULL AUTO_INCREMENT COMMENT '类别ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '类别名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '类别的描述',
  `createdTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updatedTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`category_id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of pots_category
-- ----------------------------

-- ----------------------------
-- Table structure for product_info
-- ----------------------------
DROP TABLE IF EXISTS `product_info`;
CREATE TABLE `product_info`  (
  `product_id` int NOT NULL AUTO_INCREMENT COMMENT '商品ID，作为主键',
  `product_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '商品名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '商品描述',
  `brand_id` int NULL DEFAULT NULL COMMENT '品牌ID，作为外键引用Brand表',
  `shop_id` int NULL DEFAULT NULL COMMENT '店铺ID，作为外键引用Shop表',
  `origin` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '原产地',
  `material` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '材质',
  `size` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '尺寸',
  `color` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '颜色',
  `weight` decimal(10, 2) NULL DEFAULT NULL COMMENT '重量',
  `packaging_details` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '包装详情',
  `warranty_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '保修信息',
  `production_date` date NULL DEFAULT NULL COMMENT '生产日期',
  `expiration_date` date NULL DEFAULT NULL COMMENT '保质期',
  `category_id` int NULL DEFAULT NULL COMMENT '商品分类ID，作为外键引用Category表',
  `main_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '商品首图',
  `additional_images` json NULL COMMENT '轮播图',
  `tags` json NULL COMMENT '标签',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，自动填充',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，自动更新',
  `details_images` json NULL COMMENT '商品详情介绍图',
  PRIMARY KEY (`product_id`) USING BTREE,
  INDEX `brand_id`(`brand_id` ASC) USING BTREE,
  INDEX `shop_id`(`shop_id` ASC) USING BTREE,
  INDEX `category_id`(`category_id` ASC) USING BTREE,
  CONSTRAINT `product_info_ibfk_1` FOREIGN KEY (`brand_id`) REFERENCES `brand` (`brand_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `product_info_ibfk_2` FOREIGN KEY (`shop_id`) REFERENCES `shop` (`shop_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `product_info_ibfk_3` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product_info
-- ----------------------------

-- ----------------------------
-- Table structure for product_sales
-- ----------------------------
DROP TABLE IF EXISTS `product_sales`;
CREATE TABLE `product_sales`  (
  `product_id` int NOT NULL COMMENT '商品ID',
  `price` decimal(10, 2) NOT NULL COMMENT '商品价格',
  `cost_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '成本价格',
  `stock_quantity` int NULL DEFAULT NULL COMMENT '库存数量',
  `reorder_threshold` int NULL DEFAULT NULL COMMENT '再订购点(补货阈值)',
  `sold_quantity` int NULL DEFAULT NULL COMMENT '已售数量',
  `review_count` int NULL DEFAULT NULL COMMENT '评论数量',
  `average_rating` decimal(3, 2) NULL DEFAULT NULL COMMENT '平均评分',
  `promotion_details` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '促销详情',
  `shipping_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '运费',
  `sales_status` enum('Available','Out of Stock','Pre-order') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'Available' COMMENT '销售状态',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`product_id`) USING BTREE,
  CONSTRAINT `product_sales_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `product_info` (`product_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product_sales
-- ----------------------------

-- ----------------------------
-- Table structure for promotion
-- ----------------------------
DROP TABLE IF EXISTS `promotion`;
CREATE TABLE `promotion`  (
  `promotion_id` int NOT NULL AUTO_INCREMENT COMMENT '促销活动唯一标识符',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '活动标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '活动描述',
  `type_id` int NULL DEFAULT NULL COMMENT '活动类型',
  `start_date` datetime NOT NULL COMMENT '活动开始日期和时间',
  `end_date` datetime NOT NULL COMMENT '活动结束日期和时间',
  `discount` decimal(5, 2) NULL DEFAULT NULL COMMENT '折扣百分比或固定金额',
  `minimum_spend` decimal(10, 2) NULL DEFAULT NULL COMMENT '最低消费金额才能享受优惠',
  `maximum_spend` decimal(10, 2) NULL DEFAULT NULL COMMENT '最高消费金额限制优惠',
  `product_id` int NULL DEFAULT NULL COMMENT '赠品ID',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '活动是否激活',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`promotion_id`) USING BTREE,
  INDEX `type_id`(`type_id` ASC) USING BTREE,
  INDEX `product_id`(`product_id` ASC) USING BTREE,
  CONSTRAINT `promotion_ibfk_1` FOREIGN KEY (`type_id`) REFERENCES `promotion_type` (`type_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `promotion_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `product_info` (`product_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of promotion
-- ----------------------------

-- ----------------------------
-- Table structure for promotion_type
-- ----------------------------
DROP TABLE IF EXISTS `promotion_type`;
CREATE TABLE `promotion_type`  (
  `type_id` int NOT NULL AUTO_INCREMENT COMMENT '活动类型唯一标识符',
  `name` enum('Discount','BuyOneGetOneFree','FixedAmountOff','PercentageOff','GiftWithPurchase') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '活动类型的名称: 打折、买一送一、满额减免、百分比折扣、购物赠品',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '描述活动类型的详细信息',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`type_id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of promotion_type
-- ----------------------------

-- ----------------------------
-- Table structure for shop
-- ----------------------------
DROP TABLE IF EXISTS `shop`;
CREATE TABLE `shop`  (
  `shop_id` int NOT NULL AUTO_INCREMENT COMMENT ' 店铺ID，作为主键',
  `shop_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '店铺名称',
  `owner_user_id` int NULL DEFAULT NULL COMMENT '店铺负责人的用户ID，作为外键引用User表',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '联系信息',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '地址',
  `registration_date` date NULL DEFAULT NULL COMMENT '注册日期',
  `shop_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '店铺描述',
  `shop_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '店铺图片的URL',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，自动填充',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，自动更新',
  `status` enum('Active','Inactive') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'Active' COMMENT '店铺状态',
  PRIMARY KEY (`shop_id`) USING BTREE,
  INDEX `owner_user_id`(`owner_user_id` ASC) USING BTREE,
  CONSTRAINT `shop_ibfk_1` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of shop
-- ----------------------------

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID，作为主键',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '用户名，必须唯一',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '密码哈希加密存储',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '电子邮件',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '手机号码',
  `role` enum('Admin','ShopOwner','Customer') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'Customer' COMMENT '用户角色: \'Admin\', \'ShopOwner\', \'Customer\'',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，自动填充',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，自动更新',
  `status` enum('Active','Inactive','Invalid') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'Active' COMMENT '用户状态: \'Active\', \'Inactive\', \'Invalid\'',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '用户头像',
  `birth` date NULL DEFAULT NULL COMMENT '出生日期',
  `gender` enum('Male','Female','Other') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'Male' COMMENT '性别: Male, Female, Other',
  `default_address_id` int NULL DEFAULT NULL COMMENT '默认收货地址',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  INDEX `fk_user_default_address`(`default_address_id` ASC) USING BTREE,
  CONSTRAINT `fk_user_default_address` FOREIGN KEY (`default_address_id`) REFERENCES `address` (`address_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------

-- ----------------------------
-- Table structure for warehouse
-- ----------------------------
DROP TABLE IF EXISTS `warehouse`;
CREATE TABLE `warehouse`  (
  `warehouse_id` int NOT NULL AUTO_INCREMENT COMMENT '仓库唯一标识符',
  `warehouse_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '仓库名称',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '仓库地址',
  `contact_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '联系信息',
  `total_capacity` int NOT NULL COMMENT '仓库总容量',
  `current_capacity` int NOT NULL COMMENT '当前占用容量',
  `manager_user_id` int NULL DEFAULT NULL COMMENT '仓库经理的用户ID',
  `createTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` enum('Active','Inactive') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'Active' COMMENT '仓库状态',
  PRIMARY KEY (`warehouse_id`) USING BTREE,
  INDEX `manager_user_id`(`manager_user_id` ASC) USING BTREE,
  CONSTRAINT `warehouse_ibfk_1` FOREIGN KEY (`manager_user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of warehouse
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
