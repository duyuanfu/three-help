package com.bangbangbang.config;

import com.bangbangbang.common.NameInitial;
import com.bangbangbang.service.PasswordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据结构与数据迁移（幂等，每次启动都会执行）
 *
 * 目的：让「已经存在的库」和「全新初始化的库」最终拥有一致结构，
 * 不需要手工执行 ALTER 语句，也不会重复插入数据。
 *
 * 主要变更：
 * 1. sys_user 增加 password_hash（真实登录）
 * 2. 新增 user_token 表（登录令牌）
 * 3. contact 增加 owner_id / target_user_id（通讯录私有 + 关联真实账号）
 * 4. chat_session 增加 user_a_id / user_b_id / unread_a / unread_b（双人共享会话）
 * 5. post_comment 增加 author_id
 * 6. address / exchange_order 增加 owner_id
 * 7. 头像地址由小程序包内路径迁到服务器上传目录
 */
@Slf4j
@Component
public class DataMigration implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final PasswordService passwordService;

    public DataMigration(JdbcTemplate jdbc, PasswordService passwordService) {
        this.jdbc = jdbc;
        this.passwordService = passwordService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            migrateColumns();
            migrateTokensTable();
            migrateAvatars();
            migrateUsers();
            migrateAdmins();
            migrateOwners();
            migrateSessions();
            syncProfileSnapshots();
            syncPostCommentCounts();
            log.info("[迁移] 数据结构与数据检查完成");
        } catch (Exception e) {
            log.error("[迁移] 执行失败", e);
        }
    }

    /* ------------------------------------------------------------------ */

    private void migrateColumns() {
        addColumn("sys_user", "password_hash", "VARCHAR(200) DEFAULT NULL COMMENT 'PBKDF2密码哈希'");
        addColumn("contact", "owner_id", "VARCHAR(64) DEFAULT NULL COMMENT '归属用户'");
        addColumn("contact", "target_user_id", "VARCHAR(64) DEFAULT NULL COMMENT '对方账号ID'");
        addColumn("chat_session", "user_a_id", "VARCHAR(64) DEFAULT NULL COMMENT '会话方A'");
        addColumn("chat_session", "user_b_id", "VARCHAR(64) DEFAULT NULL COMMENT '会话方B'");
        addColumn("chat_session", "unread_a", "INT NOT NULL DEFAULT 0 COMMENT 'A未读数'");
        addColumn("chat_session", "unread_b", "INT NOT NULL DEFAULT 0 COMMENT 'B未读数'");
        addColumn("post_comment", "author_id", "VARCHAR(64) DEFAULT NULL COMMENT '评论者账号ID'");
        addColumn("address", "owner_id", "VARCHAR(64) DEFAULT NULL COMMENT '归属用户'");
        addColumn("exchange_order", "owner_id", "VARCHAR(64) DEFAULT NULL COMMENT '归属用户'");
        addColumn("sys_user", "is_admin", "TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否管理员'");
    }

    private void migrateTokensTable() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS `user_token` (" +
                "  `token`      VARCHAR(64) NOT NULL COMMENT '令牌'," +
                "  `user_id`    VARCHAR(64) NOT NULL COMMENT '用户ID'," +
                "  `created_at` DATETIME    NOT NULL COMMENT '签发时间'," +
                "  `expires_at` DATETIME    NOT NULL COMMENT '过期时间'," +
                "  PRIMARY KEY (`token`)," +
                "  KEY `idx_user` (`user_id`)" +
                ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '登录令牌'");
    }

    /** 头像从小程序包内静态资源迁到服务器上传目录 */
    private void migrateAvatars() {
        String[] tables = {"sys_user", "contact", "post", "post_comment", "chat_session",
                "help_order", "helper"};
        for (String table : tables) {
            for (String column : avatarColumnsOf(table)) {
                jdbc.update("UPDATE `" + table + "` SET `" + column + "` = REPLACE(`" + column + "`, " +
                        "'/static/avatars/', '/uploads/default/') WHERE `" + column + "` LIKE '/static/avatars/%'");
            }
        }
    }

    private String[] avatarColumnsOf(String table) {
        return switch (table) {
            case "sys_user" -> new String[]{"avatar"};
            case "contact" -> new String[]{"avatar"};
            case "post" -> new String[]{"author_avatar"};
            case "post_comment" -> new String[]{"avatar"};
            case "chat_session" -> new String[]{"target_user_avatar"};
            case "help_order" -> new String[]{"creator_avatar", "helper_avatar"};
            case "helper" -> new String[]{"avatar"};
            default -> new String[0];
        };
    }

    /** 为示例人物补齐真实账号，让聊天/接单都有真实对象 */
    private void migrateUsers() {
        String demoPassword = passwordService.hash("123456");

        upsertUser("u_zhangdongmei", "张冬梅", "13859201234", "/uploads/default/zhangdongmei.jpg",
                "女", 68, "elder", null, demoPassword);
        upsertUser("u_lilaotou", "李老头", "13800000001", "/uploads/default/lilaotou.jpg",
                "男", 72, "elder", null, demoPassword);
        upsertUser("u_malingling", "马玲玲", "13900000002", "/uploads/default/malingling.jpg",
                "女", 21, "volunteer", "@学生", demoPassword);
        upsertUser("u_wangqiuhua", "王秋华", "13700000003", "/uploads/default/wangqiuhua.jpg",
                "女", 45, "volunteer", "@村委会干事", demoPassword);

        // 订单/动态里出现、但历史数据中只有名字没有账号的人物。
        // 不补的话，接单人点"联系发单人"会报"对方账号不存在"，会话也建不起来。
        upsertUser("u_wangnainai", "王奶奶", "13800000004", "/uploads/default/wangnainai.jpg",
                "女", 76, "elder", null, demoPassword);
        upsertUser("u_wuxiaoying", "吴小应", "13900000005", "/uploads/default/wuxiaoying.jpg",
                "男", 22, "volunteer", "@学生", demoPassword);
        upsertUser("u_liuxiaoyan", "刘小燕", "13900000006", "/uploads/default/liuxiaoyan.jpg",
                "女", 24, "volunteer", "@学生", demoPassword);
        upsertUser("u_languiping", "蓝贵萍", "13900000007", "/uploads/default/languiping.jpg",
                "女", 48, "volunteer", "@社区居民", demoPassword);

        // 已有的示例用户若没有密码，补一个初始密码（123456），避免无法登录
        jdbc.update("UPDATE sys_user SET password_hash = ? WHERE password_hash IS NULL OR password_hash = ''",
                demoPassword);
    }

    private void upsertUser(String id, String name, String phone, String avatar,
                            String gender, int age, String role, String identityTag, String passwordHash) {
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id = ?", Integer.class, id);
        if (exists != null && exists > 0) {
            // 已存在时**绝不能**覆盖 avatar / name / phone：
            // 用户可能自己改过头像或昵称，每次启动被重置会让"修改头像"永久失效。
            // 这里只兜底补齐缺失的角色，其余字段尊重数据库现值。
            jdbc.update("UPDATE sys_user SET role = ? WHERE id = ? AND (role IS NULL OR role = '')", role, id);
            return;
        }
        jdbc.update("INSERT INTO sys_user (id, name, avatar, gender, age, phone, is_real_name, identity_tag, " +
                        "occupation, address, organization, points, role, is_logged_in, password_hash, " +
                        "created_at, updated_at) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                id, name, avatar, gender, age, phone, true,
                identityTag == null ? "" : identityTag,
                role.equals("volunteer") ? "志愿者" : "退休职工",
                "", "",
                role.equals("volunteer") ? 200 : 120, role, true, passwordHash);
    }

    /**
     * 管理员账号
     *
     * 管理后台需要能查看/编辑/删除**全部**数据，所以必须有一个明确的管理员身份，
     * 不能靠"是不是自己创建的"来判断。演示环境下固定两个：
     *  - u_admin（专门的后台账号）
     *  - u_wangqiuhua（村委会干事，语义上就是社区管理者）
     */
    private void migrateAdmins() {
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id = 'u_admin'",
                Integer.class);
        if (exists == null || exists == 0) {
            jdbc.update("INSERT INTO sys_user (id, name, avatar, gender, age, phone, is_real_name, " +
                            "identity_tag, occupation, address, organization, points, role, is_logged_in, " +
                            "is_admin, password_hash, created_at, updated_at) " +
                            "VALUES ('u_admin', '系统管理员', '/uploads/default/avatar.jpg', '男', 35, " +
                            "'13800000000', TRUE, '@平台管理员', '平台运营', '', '', 999, 'admin', TRUE, TRUE, ?, " +
                            "NOW(), NOW())",
                    passwordService.hash("admin123"));
        } else {
            // 已存在时只保证管理员标记，不覆盖密码与资料
            jdbc.update("UPDATE sys_user SET is_admin = TRUE WHERE id = 'u_admin'");
        }

        // 村委会干事在演示数据里即社区管理者
        jdbc.update("UPDATE sys_user SET is_admin = TRUE WHERE id = 'u_wangqiuhua'");
    }

    /** 把历史数据归属到示例用户，并关联真实账号 */
    private void migrateOwners() {
        jdbc.update("UPDATE contact SET owner_id = 'u_zhangdongmei' WHERE owner_id IS NULL");
        jdbc.update("UPDATE address SET owner_id = 'u_zhangdongmei' WHERE owner_id IS NULL");
        jdbc.update("UPDATE exchange_order SET owner_id = 'u_zhangdongmei' WHERE owner_id IS NULL");
        jdbc.update("UPDATE post_comment SET author_id = (SELECT id FROM sys_user WHERE name = post_comment.user_name LIMIT 1) " +
                "WHERE author_id IS NULL");

        // 联系人按手机号关联到真实账号
        jdbc.update("UPDATE contact c JOIN sys_user u ON c.phone = u.phone " +
                "SET c.target_user_id = u.id, c.name = u.name, c.avatar = u.avatar " +
                "WHERE c.target_user_id IS NULL");
    }

    /** 旧会话改成双人共享会话（sessionId 归一化，消息同步迁移） */
    private void migrateSessions() {
        List<java.util.Map<String, Object>> rows = jdbc.queryForList(
                "SELECT session_id, target_user_id FROM chat_session WHERE user_a_id IS NULL");
        for (java.util.Map<String, Object> row : rows) {
            String oldId = String.valueOf(row.get("session_id"));
            String targetUserId = row.get("target_user_id") == null ? null : String.valueOf(row.get("target_user_id"));

            // 旧数据里的 target_user_id 可能是联系人ID（c_xxx），按手机号映射到真实账号
            String realTarget = queryOneString("SELECT u.id FROM contact c JOIN sys_user u ON c.phone = u.phone " +
                    "WHERE c.id = ? LIMIT 1", targetUserId);
            if (realTarget == null) {
                realTarget = queryOneString("SELECT id FROM sys_user WHERE id = ? LIMIT 1", targetUserId);
            }
            if (realTarget == null || realTarget.equals("u_zhangdongmei")) {
                continue;
            }

            String newId = "sess_" + min(realTarget, "u_zhangdongmei") + "__" + max(realTarget, "u_zhangdongmei");

            // 规范会话已存在：说明是种子脚本重新插入的旧行，直接清掉，保持幂等
            Integer existed = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM chat_session WHERE session_id = ?", Integer.class, newId);
            if (existed != null && existed > 0) {
                jdbc.update("DELETE FROM chat_message WHERE session_id = ?", oldId);
                jdbc.update("DELETE FROM chat_session WHERE session_id = ?", oldId);
                continue;
            }

            jdbc.update("UPDATE chat_message SET session_id = ? WHERE session_id = ?", newId, oldId);
            jdbc.update("UPDATE chat_session SET session_id = ?, user_a_id = ?, user_b_id = ?, " +
                            "target_user_id = ?, unread_a = 0, unread_b = 0 WHERE session_id = ?",
                    newId, min(realTarget, "u_zhangdongmei"), max(realTarget, "u_zhangdongmei"), realTarget, oldId);
        }

        // 兜底：映射不到真实账号的旧会话直接移除（避免前端出现空对象会话）
        jdbc.update("DELETE FROM chat_message WHERE session_id IN " +
                "(SELECT session_id FROM chat_session WHERE user_a_id IS NULL)");
        jdbc.update("DELETE FROM chat_session WHERE user_a_id IS NULL");
    }

    /**
     * 用 sys_user 里的最新资料，把各表冗余的姓名/头像/标签全量刷新一遍。
     *
     * 这些字段是写入时的快照，用户改过资料之后就永久停在旧值上，
     * 于是同一个人会在「全部动态」显示旧名、在「私信」显示新名。
     * 用户资料变更时 UserProfileSyncService 会做增量同步，这里负责修历史存量，
     * 启动时执行一次、幂等，数据量很小（百行级），不会影响启动速度。
     */
    private void syncProfileSnapshots() {
        // 已经是同一个人的展示字段，空值不覆盖（避免把已有的头像抹掉）
        int post = jdbc.update("UPDATE post p JOIN sys_user u ON u.id = p.author_id " +
                "SET p.author_name = IF(u.name = '', p.author_name, u.name), " +
                "    p.author_avatar = IF(u.avatar = '', p.author_avatar, u.avatar), " +
                "    p.author_tag = u.identity_tag");

        int comment = jdbc.update("UPDATE post_comment c JOIN sys_user u ON u.id = c.author_id " +
                "SET c.user_name = IF(u.name = '', c.user_name, u.name), " +
                "    c.avatar = IF(u.avatar = '', c.avatar, u.avatar)");

        int creator = jdbc.update("UPDATE help_order o JOIN sys_user u ON u.id = o.creator_id " +
                "SET o.creator_name = IF(u.name = '', o.creator_name, u.name), " +
                "    o.creator_avatar = IF(u.avatar = '', o.creator_avatar, u.avatar), " +
                "    o.creator_tag = u.identity_tag");

        int helper = jdbc.update("UPDATE help_order o JOIN sys_user u ON u.id = o.helper_id " +
                "SET o.helper_name = IF(u.name = '', o.helper_name, u.name), " +
                "    o.helper_avatar = IF(u.avatar = '', o.helper_avatar, u.avatar), " +
                "    o.helper_tag = u.identity_tag, " +
                "    o.helper_phone = IF(u.phone = '', o.helper_phone, u.phone)");

        // 会话列表本身是实时查用户，这两个字段只是兼容旧结构，一并刷成最新值
        int session = jdbc.update("UPDATE chat_session s JOIN sys_user u ON u.id = s.target_user_id " +
                "SET s.target_user_name = IF(u.name = '', s.target_user_name, u.name), " +
                "    s.target_user_avatar = IF(u.avatar = '', s.target_user_avatar, u.avatar), " +
                "    s.target_user_tag = u.identity_tag");

        // 通讯录只同步「已关联真实账号」的条目；手工录入的联系人保留用户自己填的名字
        int contact = jdbc.update("UPDATE contact c JOIN sys_user u ON u.id = c.target_user_id " +
                "SET c.name = IF(u.name = '', c.name, u.name), " +
                "    c.avatar = IF(u.avatar = '', c.avatar, u.avatar), " +
                "    c.tag = u.identity_tag, " +
                "    c.phone = IF(u.phone = '', c.phone, u.phone)");
        refreshContactLetters();

        log.info("[迁移] 用户资料快照回填：动态 {}、评论 {}、订单发布人 {}、订单接单人 {}、会话 {}、联系人 {}",
                post, comment, creator, helper, session, contact);
    }

    /**
     * 用真实评论数校准 post.comment_count。
     *
     * 广场改成游标分页后，前端不再把评论全量读进内存、也就没法在本地数一遍了，
     * 卡片上的「评论 N」直接取这个字段。历史数据里如果留着早期种子数据的假数字，
     * 会出现「显示 12 条评论、点进去一条都没有」。启动时对一次账，幂等。
     */
    private void syncPostCommentCounts() {
        int fixed = jdbc.update("UPDATE post p " +
                "LEFT JOIN (SELECT post_id, COUNT(*) AS c FROM post_comment GROUP BY post_id) t " +
                "  ON t.post_id = p.id " +
                "SET p.comment_count = IFNULL(t.c, 0) " +
                // 用 <=> 而不是 <>：MySQL 里 NULL <> 0 结果是 NULL，等于不漏也不修
                "WHERE NOT (p.comment_count <=> IFNULL(t.c, 0))");
        if (fixed > 0) {
            log.info("[迁移] 动态评论数校准：修正 {} 条", fixed);
        }
    }

    /** 姓名变了，通讯录的首字母分组也要跟着变（字母映射在 Java 里，SQL 算不了） */
    private void refreshContactLetters() {
        List<java.util.Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, name FROM contact WHERE target_user_id IS NOT NULL AND target_user_id <> ''");
        for (java.util.Map<String, Object> row : rows) {
            Object id = row.get("id");
            Object name = row.get("name");
            jdbc.update("UPDATE contact SET letter = ? WHERE id = ?",
                    NameInitial.of(name == null ? "" : String.valueOf(name)), id);
        }
    }

    /* ------------------------------------------------------------------ */

    private String queryOneString(String sql, Object... args) {
        List<String> list = jdbc.queryForList(sql, String.class, args);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    private void addColumn(String table, String column, String definition) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        if (count != null && count > 0) {
            return;
        }
        Integer tableExists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class, table);
        if (tableExists == null || tableExists == 0) {
            return;
        }
        jdbc.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
        log.info("[迁移] {}.{} 已新增", table, column);
    }

    private String min(String a, String b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private String max(String a, String b) {
        return a.compareTo(b) <= 0 ? b : a;
    }
}
