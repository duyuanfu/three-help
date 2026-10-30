-- =====================================================================
-- 帮帮帮小程序 · 一键初始化脚本（建库 + 建表 + 初始数据 + 应用账号）
--
-- 执行方式（WSL / Linux）：
--   mysql -u root -p < /mnt/f/Workspace/three-help/server/src/main/resources/db/init.sql
--
-- 执行方式（Windows CMD / PowerShell，需已安装 mysql 客户端）：
--   mysql -u root -p < F:\Workspace\three-help\server\src\main\resources\db\init.sql
--
-- 说明：
--   1. 全部语句幂等，可重复执行，不会清空或覆盖已有数据
--   2. 执行完成后会创建应用账号 bbb / bbb123456，后端默认用它连接
--   3. 想彻底重置数据，先执行 reset.sql 再执行本文件
-- =====================================================================

-- ★ 必须先声明连接字符集：
--   MySQL 容器首次初始化时在 LANG=C 环境里跑本脚本，mysql 客户端会把
--   连接字符集自动判定为 latin1，UTF-8 的中文按 latin1 解析后全部变成
--   双重编码乱码；而 seed 全部是 INSERT IGNORE，应用启动时用正确编码
--   重放也无法覆盖，乱码就固化了。SET NAMES 把会话字符集强制为 utf8mb4。
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `bbb`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `bbb`;

-- =====================================================================
-- 帮帮帮小程序 · MySQL 建表脚本
-- 对应前端 src/utils/db.ts 中的 13 张逻辑表
-- 字符集 utf8mb4，兼容 MySQL 5.7 / 8.x
--
-- 本文件由后端启动时自动执行（spring.sql.init），因此：
--   · 不含 CREATE DATABASE / USE（JDBC 连接已指定到 bbb 库）
--   · 全部使用 CREATE TABLE IF NOT EXISTS，可重复执行
-- 需要「从零建库」时请执行同目录下的 init.sql
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 用户（原 user 单行表；user 是 MySQL 保留字，故命名 sys_user）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`            VARCHAR(64)  NOT NULL COMMENT '用户ID',
    `name`          VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '姓名',
    `avatar`        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像路径',
    `gender`        VARCHAR(8)   NOT NULL DEFAULT '女' COMMENT '性别',
    `age`           INT          NOT NULL DEFAULT 0 COMMENT '年龄',
    `phone`         VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '手机号',
    `openid`        VARCHAR(64)  DEFAULT NULL COMMENT '微信 openid（未绑定时为 NULL）',
    `is_real_name`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否实名',
    `identity_tag`  VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '身份标签',
    `occupation`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '职业',
    `address`       VARCHAR(255) NOT NULL DEFAULT '' COMMENT '常用地址',
    `organization`  VARCHAR(128) NOT NULL DEFAULT '' COMMENT '所属组织',
    `points`        INT          NOT NULL DEFAULT 0 COMMENT '积分余额',
    `role`          VARCHAR(16)  NOT NULL DEFAULT 'elder' COMMENT '角色 elder/volunteer',
    `is_logged_in`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '登录态',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    -- 微信登录用；uk_openid 唯一索引允许存在多个 NULL（未绑定微信的用户）
    UNIQUE KEY `uk_openid` (`openid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户信息';

-- ---------------------------------------------------------------------
-- 2. 适老设置（原 setting 单行表）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `app_setting` (
    `id`               INT          NOT NULL DEFAULT 1 COMMENT '固定为 1',
    `font_scale_level` INT          NOT NULL DEFAULT 1 COMMENT '字号档位 0-3',
    `font_scale`       DECIMAL(4,2) NOT NULL DEFAULT 1.00 COMMENT '字号倍率',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '适老设置';

-- ---------------------------------------------------------------------
-- 3. 互助订单（原 orders 表；helper / review 平铺为列）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `help_order` (
    `id`             VARCHAR(64)  NOT NULL COMMENT '订单ID',
    `creator_id`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '发布人ID',
    `creator_name`   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '发布人姓名',
    `creator_avatar` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '发布人头像',
    `creator_tag`    VARCHAR(64)           DEFAULT NULL COMMENT '发布人标签',
    `category`       VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '服务类型',
    `title`          VARCHAR(128) NOT NULL DEFAULT '' COMMENT '任务简称',
    `content`        TEXT COMMENT '详细描述',
    `appoint_time`   VARCHAR(128) NOT NULL DEFAULT '' COMMENT '约定时间',
    `address`        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '约定地点',
    `bounty`         INT          NOT NULL DEFAULT 0 COMMENT '赏金',
    `status`         VARCHAR(32)  NOT NULL DEFAULT 'pending' COMMENT '订单状态',
    `status_text`    VARCHAR(128)          DEFAULT NULL COMMENT '状态描述',
    `helper_id`      VARCHAR(64)           DEFAULT NULL COMMENT '接单人ID',
    `helper_name`    VARCHAR(64)           DEFAULT NULL COMMENT '接单人姓名',
    `helper_tag`     VARCHAR(64)           DEFAULT NULL COMMENT '接单人标签',
    `helper_avatar`  VARCHAR(255)          DEFAULT NULL COMMENT '接单人头像',
    `helper_phone`   VARCHAR(32)           DEFAULT NULL COMMENT '接单人电话',
    `review_stars`   INT                   DEFAULT NULL COMMENT '评价星级',
    `review_comment` VARCHAR(500)          DEFAULT NULL COMMENT '评价内容',
    `created_at`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '创建时间（展示用）',
    `completed_at`   VARCHAR(64)           DEFAULT NULL COMMENT '完成时间（展示用）',
    `sort_no`        BIGINT       NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_creator` (`creator_id`),
    KEY `idx_sort` (`sort_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '互助订单';

-- ---------------------------------------------------------------------
-- 4. 兑换订单物流（原 exchange_orders 表；steps 存 JSON）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `exchange_order` (
    `id`              VARCHAR(64)  NOT NULL COMMENT '订单ID',
    `order_no`        VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '运单号',
    `goods_name`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '商品名称',
    `goods_image`     VARCHAR(255) NOT NULL DEFAULT '' COMMENT '商品图片',
    `quantity`        INT          NOT NULL DEFAULT 0 COMMENT '数量',
    `points_cost`     INT          NOT NULL DEFAULT 0 COMMENT '消耗积分',
    `address`         VARCHAR(255) NOT NULL DEFAULT '' COMMENT '收货地址',
    `express_company` VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '快递公司',
    `sender_city`     VARCHAR(128) NOT NULL DEFAULT '' COMMENT '发货地',
    `receiver_city`   VARCHAR(128) NOT NULL DEFAULT '' COMMENT '收货人',
    `steps`           TEXT COMMENT '物流节点 JSON 数组',
    `created_at`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '创建时间',
    `sort_no`         BIGINT       NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`id`),
    KEY `idx_sort` (`sort_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '积分兑换订单';

-- ---------------------------------------------------------------------
-- 5. 通讯录联系人（原 contacts 表）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `contact` (
    `id`        VARCHAR(64)  NOT NULL COMMENT '联系人ID',
    `name`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '姓名',
    `avatar`    VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像',
    `tag`       VARCHAR(64)           DEFAULT NULL COMMENT '标签',
    `letter`    VARCHAR(8)   NOT NULL DEFAULT 'A' COMMENT '拼音首字母',
    `phone`     VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '手机号',
    `is_friend` TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否已添加好友',
    `sort_no`   BIGINT       NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`id`),
    KEY `idx_letter` (`letter`),
    KEY `idx_sort` (`sort_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通讯录';

-- ---------------------------------------------------------------------
-- 6. 会话列表（原 sessions 表；targetUser 平铺）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_session` (
    `session_id`         VARCHAR(64)  NOT NULL COMMENT '会话ID',
    `target_user_id`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '对方ID',
    `target_user_name`   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '对方姓名',
    `target_user_avatar` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '对方头像',
    `target_user_tag`    VARCHAR(64)           DEFAULT NULL COMMENT '对方标签',
    `last_message`       VARCHAR(500) NOT NULL DEFAULT '' COMMENT '最后一条消息',
    `last_time`          VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '最后消息时间',
    `unread_count`       INT          NOT NULL DEFAULT 0 COMMENT '未读数',
    `related_order_id`   VARCHAR(64)           DEFAULT NULL COMMENT '关联订单ID',
    `sort_no`            BIGINT       NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`session_id`),
    KEY `idx_sort` (`sort_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '聊天会话';

-- ---------------------------------------------------------------------
-- 7. 聊天消息（原 messages 单行表 → 改为真正的行表，一条消息一行）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id`          VARCHAR(64)  NOT NULL COMMENT '消息ID',
    `session_id`  VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '会话ID',
    `sender_id`   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '发送者ID',
    `receiver_id` VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '接收者ID',
    `type`        VARCHAR(16)  NOT NULL DEFAULT 'text' COMMENT '消息类型',
    `content`     TEXT COMMENT '内容',
    `duration`    INT                   DEFAULT NULL COMMENT '语音时长(秒)',
    `audio_path`  VARCHAR(255)          DEFAULT NULL COMMENT '语音文件路径',
    `created_at`  VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '展示时间 HH:mm',
    `timestamp`   BIGINT       NOT NULL DEFAULT 0 COMMENT '时间戳(毫秒)',
    PRIMARY KEY (`id`),
    KEY `idx_session_ts` (`session_id`, `timestamp`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '聊天消息';

-- ---------------------------------------------------------------------
-- 8. 广场动态（原 posts 表；images 存 JSON 数组）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `post` (
    `id`            VARCHAR(64)  NOT NULL COMMENT '动态ID',
    `author_id`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '作者ID',
    `author_name`   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '作者姓名',
    `author_avatar` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '作者头像',
    `author_tag`    VARCHAR(64)           DEFAULT NULL COMMENT '作者标签',
    `content`       TEXT COMMENT '正文',
    `images`        TEXT COMMENT '图片 JSON 数组',
    `like_count`    INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT          NOT NULL DEFAULT 0 COMMENT '评论数',
    `time_ago`      VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '相对时间展示',
    `is_liked`      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '当前用户是否点赞',
    `sort_no`       BIGINT       NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`id`),
    KEY `idx_author` (`author_id`),
    KEY `idx_sort` (`sort_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '广场动态';

-- ---------------------------------------------------------------------
-- 9. 帖子评论（原 comments 表）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `post_comment` (
    `id`        VARCHAR(64)   NOT NULL COMMENT '评论ID',
    `post_id`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '所属动态ID',
    `user_name` VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '评论人姓名',
    `avatar`    VARCHAR(255)  NOT NULL DEFAULT '' COMMENT '评论人头像',
    `content`   VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '评论内容',
    `sort_no`   BIGINT        NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`id`),
    KEY `idx_post` (`post_id`),
    KEY `idx_sort` (`sort_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '帖子评论';

-- ---------------------------------------------------------------------
-- 10. 收货地址（原 addresses 表）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `address` (
    `id`         VARCHAR(64)  NOT NULL COMMENT '地址ID',
    `name`       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '收件人',
    `phone`      VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '手机号',
    `detail`     VARCHAR(255) NOT NULL DEFAULT '' COMMENT '详细地址',
    `is_default` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否默认',
    `sort_no`    BIGINT       NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`id`),
    KEY `idx_sort` (`sort_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收货地址';

-- ---------------------------------------------------------------------
-- 11. 志愿者 / 帮手（原 helpers 表）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `helper` (
    `id`      VARCHAR(64)  NOT NULL COMMENT '帮手ID',
    `name`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '姓名',
    `tag`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '标签',
    `avatar`  VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像',
    `phone`   VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '手机号',
    `sort_no` BIGINT       NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`id`),
    KEY `idx_sort` (`sort_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '志愿者帮手';

-- ---------------------------------------------------------------------
-- 12. 积分商城商品（原 goods 表）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `goods` (
    `id`      VARCHAR(64)  NOT NULL COMMENT '商品ID',
    `name`    VARCHAR(128) NOT NULL DEFAULT '' COMMENT '商品名称',
    `price`   INT          NOT NULL DEFAULT 0 COMMENT '所需积分',
    `image`   VARCHAR(255) NOT NULL DEFAULT '' COMMENT '商品图片',
    `unit`    VARCHAR(16)  NOT NULL DEFAULT '' COMMENT '单位',
    `stock`   INT          NOT NULL DEFAULT 0 COMMENT '库存',
    `sort_no` BIGINT       NOT NULL DEFAULT 0 COMMENT '排序号',
    PRIMARY KEY (`id`),
    KEY `idx_sort` (`sort_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '积分商城商品';

-- ---------------------------------------------------------------------
-- 13. 购物车（原 cart 表；goods 改为 goods_id 外键关联）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cart_item` (
    `id`       VARCHAR(64) NOT NULL COMMENT '行ID',
    `user_id`  VARCHAR(64) NOT NULL DEFAULT '' COMMENT '所属用户',
    `goods_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '商品ID',
    `quantity` INT         NOT NULL DEFAULT 1 COMMENT '数量',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_goods` (`user_id`, `goods_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '购物车';

-- =====================================================================
-- 初始数据
-- =====================================================================

-- =====================================================================
-- 帮帮帮小程序 · 初始化数据
-- 对应前端 src/mock/seeds.ts，全部使用 INSERT IGNORE 保证可重复执行
-- 由后端启动时自动执行（spring.sql.init），已连接至 bbb 库
-- =====================================================================

-- ---------------------------------------------------------------------
-- 用户
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_user`
(`id`, `name`, `avatar`, `gender`, `age`, `phone`, `is_real_name`, `identity_tag`,
 `occupation`, `address`, `organization`, `points`, `role`, `is_logged_in`)
VALUES
('u_zhangdongmei', '张冬梅', '/static/avatars/zhangdongmei.jpg', '女', 65, '138****5920', 1, '',
 '退休教师', '福建省泉州市丰泽区凤阳小区8号楼302', '泉州市丰泽区东湖街道社区居委会', 1000, 'elder', 1);

-- ---------------------------------------------------------------------
-- 适老设置
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `app_setting` (`id`, `font_scale_level`, `font_scale`)
VALUES (1, 1, 1.00);

-- ---------------------------------------------------------------------
-- 志愿者帮手
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `helper` (`id`, `name`, `tag`, `avatar`, `phone`, `sort_no`)
VALUES
('u_wuxiaoying', '吴小应', '@学生', '/static/avatars/malingling.jpg', '13912345678', 1),
('u_liuxiaoyan', '刘小燕', '@学生', '/static/avatars/malingling.jpg', '13987654321', 2),
('u_languiping', '蓝贵萍', '@村支书', '/static/avatars/wangqiuhua.jpg', '13866668888', 3);

-- ---------------------------------------------------------------------
-- 互助订单
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `help_order`
(`id`, `creator_id`, `creator_name`, `creator_avatar`, `category`, `title`, `content`,
 `appoint_time`, `address`, `bounty`, `status`, `status_text`,
 `helper_id`, `helper_name`, `helper_tag`, `helper_avatar`, `helper_phone`,
 `created_at`, `completed_at`, `sort_no`)
VALUES
('ord_001', 'u_zhangdongmei', '张冬梅', '/static/avatars/zhangdongmei.jpg', 'medical', '医院陪诊',
 '泉州市第一医院骨科门诊陪同挂号取药与照料', '2025/09/03 8:00 — 12:00', '泉州市第一医院东街院区', 50, 'in_progress', '帮手即将到达',
 'u_wuxiaoying', '吴小应', '@学生', '/static/avatars/malingling.jpg', '13912345678',
 '2025/09/02 18:00', NULL, 1),

('ord_002', 'u_zhangdongmei', '张冬梅', '/static/avatars/zhangdongmei.jpg', 'delivery', '帮抢鸡蛋',
 '永辉超市早市特价土鸡蛋抢购代买送至家门', '2025/09/03 17:30 — 18:00', '凤阳小区8号楼302', 20, 'in_progress', '已匹配到 刘小燕 帮手',
 'u_liuxiaoyan', '刘小燕', '@学生', '/static/avatars/malingling.jpg', '13987654321',
 '2025/09/03 10:20', NULL, 2),

('ord_003', 'u_zhangdongmei', '张冬梅', '/static/avatars/zhangdongmei.jpg', 'teach_phone', '教会手机',
 '教老人使用微信视频通话、线上生活缴费与挂号操作', '2025/08/25 14:30 — 18:00', '凤阳小区8号楼302', 35, 'completed', '已完成服务',
 'u_languiping', '蓝贵萍', '@村支书', '/static/avatars/wangqiuhua.jpg', '13866668888',
 '2025/08/25 10:00', '2025/08/25 18:00', 3),

('ord_004', 'u_wangnainai', '王奶奶', '/static/avatars/wangnainai.jpg', 'shopping', '陪散心',
 '需要有人来陪散心', '2026/06/08 14:00-17:00', '凤阳小区8号楼', 30, 'pending', '等待爱心帮手接单',
 NULL, NULL, NULL, NULL, NULL,
 '2026/06/08 09:30', NULL, 4);

-- ---------------------------------------------------------------------
-- 积分兑换订单（steps 为 JSON 数组）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `exchange_order`
(`id`, `order_no`, `goods_name`, `goods_image`, `quantity`, `points_cost`, `address`,
 `express_company`, `sender_city`, `receiver_city`, `steps`, `created_at`, `sort_no`)
VALUES
('ex_001', '75124398600122', '兑换鸡蛋一盒', '/static/goods/egg.jpg', 1, 1000,
 '福建省泉州市丰泽区凤阳小区8号楼302', '中通快递', '泉州市 帮帮超市', '泉州市 张冬梅',
 '[{"time":"2025/08/23 20:00","title":"已发货","subtitle":"泉州分拨中心揽收","isCurrent":false},{"time":"2025/08/24 8:00","title":"转站中","subtitle":"华大接驳点","isCurrent":false},{"time":"2025/08/24 12:00","title":"已到达","subtitle":"丰泽区服务站","code":"取件码 70-2-0029","isCurrent":true}]',
 '2025/08/23 18:28', 1);

-- ---------------------------------------------------------------------
-- 通讯录
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `contact` (`id`, `name`, `avatar`, `tag`, `letter`, `phone`, `is_friend`, `sort_no`)
VALUES
('c_li', '李老头', '/static/avatars/lilaotou.jpg', NULL, 'L', '13800000001', 1, 1),
('c_ma', '马玲玲', '/static/avatars/malingling.jpg', '@学生', 'M', '13900000002', 1, 2),
('c_wang', '王秋华', '/static/avatars/wangqiuhua.jpg', '@村委会干事', 'W', '13700000003', 1, 3);

-- ---------------------------------------------------------------------
-- 会话
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `chat_session`
(`session_id`, `target_user_id`, `target_user_name`, `target_user_avatar`, `target_user_tag`,
 `last_message`, `last_time`, `unread_count`, `sort_no`)
VALUES
('sess_li', 'c_li', '李老头', '/static/avatars/lilaotou.jpg', NULL,
 '好啊，先这样约好', '19 : 58', 0, 1),
('sess_ma', 'c_ma', '马玲玲', '/static/avatars/malingling.jpg', '@学生',
 '阿姨，明天上午九点我准时在楼下等您！', '16 : 56', 0, 2),
('sess_wang', 'c_wang', '王秋华', '/static/avatars/wangqiuhua.jpg', '@村委会干事',
 '张老师，社区端午节的慰问品明天统一发放。', '20 : 58', 0, 3);

-- ---------------------------------------------------------------------
-- 聊天消息（timestamp 用固定基准值，保证顺序稳定）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `chat_message`
(`id`, `session_id`, `sender_id`, `receiver_id`, `type`, `content`, `duration`, `audio_path`, `created_at`, `timestamp`)
VALUES
('m1', 'sess_li', 'c_li', 'u_zhangdongmei', 'text', '杨老太，明天晚上做我的舞伴去跳广场舞啊。', NULL, NULL, '19:50', 1757000000000),
('m2', 'sess_li', 'u_zhangdongmei', 'c_li', 'text', '好啊，先这样约好', NULL, NULL, '19:58', 1757000480000);

-- ---------------------------------------------------------------------
-- 广场动态
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `post`
(`id`, `author_id`, `author_name`, `author_avatar`, `author_tag`, `content`,
 `images`, `like_count`, `comment_count`, `time_ago`, `is_liked`, `sort_no`)
VALUES
('post_001', 'u_wangnainai', '王奶奶', '/static/avatars/wangnainai.jpg', NULL,
 '马玲玲那孩子脾气好又有耐心', NULL, 12, 1, '1分钟前', 0, 1),
('post_002', 'u_zhangdongmei', '张冬梅', '/static/avatars/zhangdongmei.jpg', NULL,
 '感谢吴小应同学今天陪我去第一医院做检查，跑前跑后非常周到，现在的大学生真有爱心！', NULL, 28, 4, '昨天', 1, 2);

-- ---------------------------------------------------------------------
-- 帖子评论
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `post_comment` (`id`, `post_id`, `user_name`, `avatar`, `content`, `sort_no`)
VALUES
('cmt_001', 'post_001', '张冬梅', '/static/avatars/zhangdongmei.jpg', '下次介绍给我', 1),
('cmt_002', 'post_002', '王奶奶', '/static/avatars/wangnainai.jpg', '很乖的一小女孩', 2),
('cmt_003', 'post_002', '李老头', '/static/avatars/lilaotou.jpg', '现在的好学生真是难得', 3),
('cmt_004', 'post_002', '马玲玲', '/static/avatars/malingling.jpg', '张阿姨身体要紧，下次还来陪您！', 4),
('cmt_005', 'post_002', '王秋华', '/static/avatars/wangqiuhua.jpg', '社区已记录，给您点赞！', 5);

-- ---------------------------------------------------------------------
-- 收货地址
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `address` (`id`, `name`, `phone`, `detail`, `is_default`, `sort_no`)
VALUES
('addr_default_1', '张冬梅', '12345678987', '福建省泉州市丰泽区凤阳小区8号楼302', 1, 1),
('addr_default_2', '张晓梅', '12345678987', '福建省厦门市集美区杏林湾路***', 0, 2);

-- ---------------------------------------------------------------------
-- 积分商城商品
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `goods` (`id`, `name`, `price`, `image`, `unit`, `stock`, `sort_no`)
VALUES
('g_egg', '鸡蛋/盒', 1000, '/static/goods/egg.jpg', '盒', 99, 1),
('g_bag', '袋子/捆', 50, '/static/goods/bag.jpg', '捆', 300, 2),
('g_paste', '牙膏/盒', 100, '/static/goods/toothpaste.jpg', '盒', 120, 3),
('g_tissue', '纸巾/盒', 500, '/static/goods/tissue.jpg', '盒', 180, 4),
('g_culture_fan', '文创团扇/把', 800, '/static/goods/fan.jpg', '把', 60, 5),
('g_culture_bag', '文创手提袋/个', 600, '/static/goods/paper-bag.jpg', '个', 80, 6);

-- =====================================================================
-- 应用专用账号
--   WSL 转发场景下 root@localhost 认证不到，后端默认改用下面的账号连接。
--   如需改用 root，请确认存在 'root'@'%' 或 'root'@'172.%' 账号。
-- =====================================================================

CREATE USER IF NOT EXISTS 'bbb'@'%' IDENTIFIED BY 'bbb123456';
GRANT ALL PRIVILEGES ON `bbb`.* TO 'bbb'@'%';
FLUSH PRIVILEGES;
