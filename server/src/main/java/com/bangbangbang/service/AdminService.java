package com.bangbangbang.service;

import com.bangbangbang.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理后台数据服务
 *
 * 与面向普通用户的 TableService 不同，这里**不做归属过滤**，
 * 管理员可以看到、修改、删除任意一条记录。
 *
 * 安全设计：
 * 1. 表名只能来自下面的静态白名单，绝不接受前端传入的表名
 * 2. 列名来自 information_schema，并且剔除隐藏列（如密码哈希）
 * 3. 所有值一律用 ? 占位符，不做字符串拼接
 * 4. 分页参数强制转成整数并限制范围
 */
@Slf4j
@Service
public class AdminService {

    /** 一张可管理的表 */
    public record TableMeta(String key, String label, String table, String pk, Set<String> hidden) {
    }

    private static final Map<String, TableMeta> TABLES = new LinkedHashMap<>();

    static {
        put("users", "用户", "sys_user", "id", Set.of("password_hash"));
        put("orders", "互助订单", "help_order", "id", Set.of());
        put("exchangeOrders", "兑换订单", "exchange_order", "id", Set.of());
        put("sessions", "聊天会话", "chat_session", "session_id", Set.of());
        put("messages", "聊天消息", "chat_message", "id", Set.of());
        put("contacts", "通讯录", "contact", "id", Set.of());
        put("posts", "广场动态", "post", "id", Set.of());
        put("comments", "动态评论", "post_comment", "id", Set.of());
        put("addresses", "收货地址", "address", "id", Set.of());
        put("helpers", "帮手档案", "helper", "id", Set.of());
        put("goods", "商品", "goods", "id", Set.of());
        put("cart", "购物车", "cart_item", "id", Set.of());
        put("setting", "应用设置", "app_setting", "id", Set.of());
    }

    private static void put(String key, String label, String table, String pk, Set<String> hidden) {
        TABLES.put(key, new TableMeta(key, label, table, pk, hidden));
    }

    /** 字段中文名，仅用于前端展示，未收录的字段直接显示原名 */
    private static final Map<String, String> FIELD_LABELS = new LinkedHashMap<>();

    static {
        FIELD_LABELS.put("id", "ID");
        FIELD_LABELS.put("name", "姓名");
        FIELD_LABELS.put("avatar", "头像");
        FIELD_LABELS.put("gender", "性别");
        FIELD_LABELS.put("age", "年龄");
        FIELD_LABELS.put("phone", "手机号");
        FIELD_LABELS.put("is_real_name", "已实名");
        FIELD_LABELS.put("identity_tag", "身份标签");
        FIELD_LABELS.put("occupation", "职业");
        FIELD_LABELS.put("address", "地址");
        FIELD_LABELS.put("organization", "所属组织");
        FIELD_LABELS.put("points", "积分");
        FIELD_LABELS.put("role", "角色");
        FIELD_LABELS.put("is_logged_in", "在线");
        FIELD_LABELS.put("is_admin", "管理员");
        FIELD_LABELS.put("created_at", "创建时间");
        FIELD_LABELS.put("updated_at", "更新时间");
        FIELD_LABELS.put("title", "标题");
        FIELD_LABELS.put("description", "描述");
        FIELD_LABELS.put("status", "状态");
        FIELD_LABELS.put("creator_id", "发单人ID");
        FIELD_LABELS.put("creator_name", "发单人");
        FIELD_LABELS.put("creator_avatar", "发单人头像");
        FIELD_LABELS.put("helper_id", "接单人ID");
        FIELD_LABELS.put("helper_name", "接单人");
        FIELD_LABELS.put("helper_avatar", "接单人头像");
        FIELD_LABELS.put("helper_tag", "接单人标签");
        FIELD_LABELS.put("reward", "酬谢");
        FIELD_LABELS.put("location", "地点");
        FIELD_LABELS.put("hope_time", "期望时间");
        FIELD_LABELS.put("contact_phone", "联系电话");
        FIELD_LABELS.put("status_text", "状态说明");
        FIELD_LABELS.put("completed_at", "完成时间");
        FIELD_LABELS.put("sort_no", "排序");
        FIELD_LABELS.put("content", "内容");
        FIELD_LABELS.put("images", "图片");
        FIELD_LABELS.put("like_count", "点赞数");
        FIELD_LABELS.put("comment_count", "评论数");
        FIELD_LABELS.put("author_id", "作者ID");
        FIELD_LABELS.put("author_name", "作者");
        FIELD_LABELS.put("author_avatar", "作者头像");
        FIELD_LABELS.put("author_tag", "作者标签");
        FIELD_LABELS.put("user_name", "用户名");
        FIELD_LABELS.put("post_id", "动态ID");
        FIELD_LABELS.put("owner_id", "归属用户");
        FIELD_LABELS.put("target_user_id", "对方账号ID");
        FIELD_LABELS.put("target_user_name", "对方姓名");
        FIELD_LABELS.put("target_user_avatar", "对方头像");
        FIELD_LABELS.put("target_user_tag", "对方标签");
        FIELD_LABELS.put("session_id", "会话ID");
        FIELD_LABELS.put("user_a_id", "会话方A");
        FIELD_LABELS.put("user_b_id", "会话方B");
        FIELD_LABELS.put("unread_a", "A未读");
        FIELD_LABELS.put("unread_b", "B未读");
        FIELD_LABELS.put("last_message", "最后一条消息");
        FIELD_LABELS.put("last_time", "最后时间");
        FIELD_LABELS.put("sender_id", "发送者ID");
        FIELD_LABELS.put("receiver_id", "接收者ID");
        FIELD_LABELS.put("type", "类型");
        FIELD_LABELS.put("duration", "时长");
        FIELD_LABELS.put("audio_path", "语音文件");
        FIELD_LABELS.put("timestamp", "时间戳");
        FIELD_LABELS.put("letter", "首字母");
        FIELD_LABELS.put("is_friend", "已是好友");
        FIELD_LABELS.put("tag", "标签");
        FIELD_LABELS.put("price", "价格");
        FIELD_LABELS.put("goods_id", "商品ID");
        FIELD_LABELS.put("quantity", "数量");
        FIELD_LABELS.put("goods_name", "商品名");
        FIELD_LABELS.put("goods_image", "商品图");
        FIELD_LABELS.put("exchange_type", "兑换方式");
        FIELD_LABELS.put("font_scale_level", "字号档位");
        FIELD_LABELS.put("high_contrast", "高对比度");
    }

    private final JdbcTemplate jdbc;
    private final UserProfileSyncService profileSyncService;

    public AdminService(JdbcTemplate jdbc, UserProfileSyncService profileSyncService) {
        this.jdbc = jdbc;
        this.profileSyncService = profileSyncService;
    }

    /* ------------------------------------------------------------------ */
    /* 表清单                                                              */
    /* ------------------------------------------------------------------ */

    public List<Map<String, Object>> listTables() {
        List<Map<String, Object>> list = new ArrayList<>();
        TABLES.values().forEach(meta -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", meta.key());
            item.put("label", meta.label());
            item.put("table", meta.table());
            item.put("pk", meta.pk());
            try {
                Integer count = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM `" + meta.table() + "`", Integer.class);
                item.put("count", count == null ? 0 : count);
            } catch (DataAccessException e) {
                // 表不存在（例如还没建）不影响其他表
                log.warn("[管理后台] 统计 {} 失败: {}", meta.table(), e.getMessage());
                item.put("count", 0);
            }
            list.add(item);
        });
        return list;
    }

    /* ------------------------------------------------------------------ */
    /* 分页查询                                                            */
    /* ------------------------------------------------------------------ */

    public Map<String, Object> page(String key, int page, int size, String keyword) {
        TableMeta meta = requireMeta(key);
        List<Map<String, Object>> columns = describeColumns(meta);
        List<String> colNames = new ArrayList<>();
        List<String> textCols = new ArrayList<>();
        for (Map<String, Object> col : columns) {
            String name = asString(col.get("name"));
            colNames.add(name);
            if (isTextColumn(asString(col.get("type")))) {
                textCols.add(name);
            }
        }

        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(5, size));

        StringBuilder where = new StringBuilder();
        List<Object> args = new ArrayList<>();
        String kw = keyword == null ? "" : keyword.trim();
        if (!kw.isEmpty() && !textCols.isEmpty()) {
            where.append(" WHERE (");
            for (int i = 0; i < textCols.size(); i++) {
                if (i > 0) {
                    where.append(" OR ");
                }
                where.append("`").append(textCols.get(i)).append("` LIKE ?");
                args.add("%" + kw + "%");
            }
            where.append(")");
        }

        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `" + meta.table() + "`" + where, Integer.class, args.toArray());
        int offset = (safePage - 1) * safeSize;

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM `" + meta.table() + "`" + where +
                        " ORDER BY `" + meta.pk() + "` DESC LIMIT " + safeSize + " OFFSET " + offset,
                args.toArray());

        // 剔除隐藏列（例如 password_hash），并对可空值做字符串兜底
        for (Map<String, Object> row : rows) {
            meta.hidden().forEach(row::remove);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", meta.key());
        result.put("label", meta.label());
        result.put("pk", meta.pk());
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", total == null ? 0 : total);
        result.put("columns", columns);
        result.put("rows", rows);
        return result;
    }

    /* ------------------------------------------------------------------ */
    /* 更新与删除                                                          */
    /* ------------------------------------------------------------------ */

    public Map<String, Object> update(String key, String id, Map<String, Object> body) {
        TableMeta meta = requireMeta(key);
        if (id == null || id.isBlank()) {
            throw BizException.of("缺少记录 ID");
        }
        List<Map<String, Object>> columns = describeColumns(meta);
        List<String> editable = new ArrayList<>();
        for (Map<String, Object> col : columns) {
            editable.add(asString(col.get("name")));
        }

        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            String col = entry.getKey();
            if (col.equals(meta.pk()) || meta.hidden().contains(col) || !editable.contains(col)) {
                continue;
            }
            sets.add("`" + col + "` = ?");
            args.add(entry.getValue());
        }
        if (sets.isEmpty()) {
            throw BizException.of("没有可更新的字段");
        }

        args.add(id);
        int n = jdbc.update("UPDATE `" + meta.table() + "` SET " + String.join(", ", sets) +
                " WHERE `" + meta.pk() + "` = ?", args.toArray());
        if (n == 0) {
            // 值完全没变化时 MySQL 也返回 0，此时不算失败
            Integer exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM `" + meta.table() + "` WHERE `" + meta.pk() + "` = ?",
                    Integer.class, id);
            if (exists == null || exists == 0) {
                throw BizException.of("记录不存在");
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("updated", n);
        // 管理后台改了用户资料同样要扩散到各表快照，否则用户会发现「后台改了名、动态上还是旧名」
        if ("sys_user".equals(meta.table())) {
            profileSyncService.syncByUserId(id);
        }
        return result;
    }

    public void delete(String key, String id) {
        TableMeta meta = requireMeta(key);
        if (id == null || id.isBlank()) {
            throw BizException.of("缺少记录 ID");
        }
        int n = jdbc.update("DELETE FROM `" + meta.table() + "` WHERE `" + meta.pk() + "` = ?", id);
        if (n == 0) {
            throw BizException.of("记录不存在或已被删除");
        }
        // 删除用户时顺手清掉他能登录的令牌，避免留下还能用的会话
        if ("sys_user".equals(meta.table())) {
            jdbc.update("DELETE FROM user_token WHERE user_id = ?", id);
        }
    }

    /* ------------------------------------------------------------------ */
    /* 工具                                                                */
    /* ------------------------------------------------------------------ */

    private TableMeta requireMeta(String key) {
        TableMeta meta = TABLES.get(key);
        if (meta == null) {
            throw BizException.of("不支持的数据表：" + key);
        }
        return meta;
    }

    /** 从 information_schema 读取表的真实列，附带中文名 */
    private List<Map<String, Object>> describeColumns(TableMeta meta) {
        List<Map<String, Object>> raw = jdbc.queryForList(
                "SELECT COLUMN_NAME AS name, DATA_TYPE AS type, IS_NULLABLE AS nullable, " +
                        "COLUMN_DEFAULT AS defaultValue, COLUMN_COMMENT AS comment " +
                        "FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                meta.table());

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> col : raw) {
            String name = asString(col.get("name"));
            if (meta.hidden().contains(name)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", name);
            item.put("label", labelOf(name));
            item.put("type", asString(col.get("type")));
            item.put("nullable", "YES".equalsIgnoreCase(asString(col.get("nullable"))));
            item.put("primary", name.equals(meta.pk()));
            item.put("comment", asString(col.get("comment")));
            list.add(item);
        }
        return list;
    }

    private boolean isTextColumn(String type) {
        return "varchar".equals(type) || "char".equals(type) || "text".equals(type)
                || "mediumtext".equals(type) || "longtext".equals(type);
    }

    private String labelOf(String column) {
        return FIELD_LABELS.getOrDefault(column, column);
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
