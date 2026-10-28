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
