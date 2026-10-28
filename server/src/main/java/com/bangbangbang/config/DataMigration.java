package com.bangbangbang.config;

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
