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
    PRIMARY KEY (`id`)
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
