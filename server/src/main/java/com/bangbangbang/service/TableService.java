package com.bangbangbang.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bangbangbang.common.BizException;
import com.bangbangbang.entity.*;
import com.bangbangbang.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通用表服务：把前端 db.ts 的 13 张逻辑表暴露为统一的表操作。
 *
 * 安全约束：
 * 1. 私有表（通讯录、地址、购物车、兑换单）默认按当前用户过滤与归属
 * 2. 订单、消息、动态、评论等业务表不允许通过通用接口写入，
 *    必须走各自的业务接口，避免客户端伪造身份字段
 */
@Service
public class TableService {

    /** 只读表：不允许通过通用接口写入 */
    private static final Set<String> READ_ONLY = Set.of(
            "orders", "messages", "sessions", "contacts", "posts", "comments", "helpers", "goods"
    );

    /** 私有表 → 归属列 */
    private static final Map<String, String> OWNER_COLUMN = Map.of(
            "addresses", "owner_id",
            "exchangeOrders", "owner_id",
            "cart", "user_id"
    );

    @Getter
    public static class TableDef {
        private final String name;
        private final BaseMapper<Object> mapper;
        private final String idField;
        private final boolean single;
        private final String orderBy;
        private final boolean insertAtHead;
        private final Class<?> entityClass;

        TableDef(String name, BaseMapper<Object> mapper, String idField,
                 boolean single, String orderBy, boolean insertAtHead, Class<?> entityClass) {
            this.name = name;
            this.mapper = mapper;
            this.idField = idField;
            this.single = single;
            this.orderBy = orderBy;
            this.insertAtHead = insertAtHead;
            this.entityClass = entityClass;
        }
    }

    private final Map<String, TableDef> tables = new LinkedHashMap<>();
    private final ObjectMapper objectMapper;

    public TableService(ObjectMapper objectMapper,
                        SysUserMapper sysUserMapper,
                        AppSettingMapper appSettingMapper,
                        HelpOrderMapper helpOrderMapper,
                        ExchangeOrderMapper exchangeOrderMapper,
                        ContactMapper contactMapper,
                        ChatSessionMapper chatSessionMapper,
                        ChatMessageMapper chatMessageMapper,
                        PostMapper postMapper,
                        PostCommentMapper commentMapper,
                        AddressMapper addressMapper,
                        HelperMapper helperMapper,
                        GoodsMapper goodsMapper,
                        CartItemMapper cartItemMapper) {
        this.objectMapper = objectMapper;

        add("user", sysUserMapper, "id", true, null, true, SysUser.class);
        add("setting", appSettingMapper, "id", true, null, true, AppSetting.class);
        add("orders", helpOrderMapper, "id", false, "sort_no ASC", true, HelpOrder.class);
        add("exchangeOrders", exchangeOrderMapper, "id", false, "sort_no ASC", true, ExchangeOrder.class);
        add("contacts", contactMapper, "id", false, "sort_no ASC", false, Contact.class);
        add("sessions", chatSessionMapper, "sessionId", false, "sort_no ASC", true, ChatSession.class);
        add("messages", chatMessageMapper, "id", false, "timestamp ASC", false, ChatMessage.class);
        add("posts", postMapper, "id", false, "sort_no ASC", true, Post.class);
        add("comments", commentMapper, "id", false, "sort_no ASC", false, PostComment.class);
        add("addresses", addressMapper, "id", false, "sort_no ASC", false, Address.class);
        add("helpers", helperMapper, "id", false, "sort_no ASC", false, Helper.class);
        add("goods", goodsMapper, "id", false, "sort_no ASC", false, Goods.class);
        add("cart", cartItemMapper, "id", false, null, false, CartItem.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void add(String name, BaseMapper mapper, String idField, boolean single,
                     String orderBy, boolean insertAtHead, Class<?> entityClass) {
        tables.put(name, new TableDef(name, mapper, idField, single, orderBy, insertAtHead, entityClass));
    }

    public TableDef def(String name) {
        TableDef def = tables.get(name);
        if (def == null) {
            throw BizException.of("未知的数据表：" + name);
        }
        return def;
    }

    public List<String> tableNames() {
        return new ArrayList<>(tables.keySet());
    }

    /* ------------------------------------------------------------------ */
    /* 查询                                                               */
    /* ------------------------------------------------------------------ */

    public List<Object> listAll(String name, String userId) {
        TableDef def = def(name);
        QueryWrapper<Object> wrapper = new QueryWrapper<>();
        String ownerColumn = OWNER_COLUMN.get(name);
        if (ownerColumn != null) {
            wrapper.eq(ownerColumn, userId);
        }
        if (def.getOrderBy() != null) {
            wrapper.last("ORDER BY " + def.getOrderBy());
        }
        List<Object> list = def.getMapper().selectList(wrapper);
        return list == null ? new ArrayList<>() : list;
    }

    /** 单行表返回对象，其余返回数组（与前端 TABLES.xxx.all() 语义一致） */
    public Object all(String name, String userId) {
        if ("user".equals(name)) {
            return def(name).getMapper().selectById(userId);
        }
        TableDef def = def(name);
        List<Object> list = listAll(name, userId);
        if (def.isSingle()) {
            return list.isEmpty() ? null : list.get(0);
        }
        return list;
    }

    /* ------------------------------------------------------------------ */
    /* 写入                                                               */
    /* ------------------------------------------------------------------ */

    public Object insert(String name, Map<String, Object> body, String userId) {
        TableDef def = def(name);
        assertWritable(name);

        if ("user".equals(name)) {
            Object entity = objectMapper.convertValue(body, def.getEntityClass());
            body.put("id", userId);
            entity = objectMapper.convertValue(body, def.getEntityClass());
            def.getMapper().updateById(entity);
            return def.getMapper().selectById(userId);
        }

        if (def.isSingle()) {
            // 单行表可能只提交业务字段（例如只传字号，不带 id）。
            // 缺主键时必须补齐，否则 INSERT 会因 id 为 null 直接抛 500。
            String idField = def.getIdField();
            if (body.get(idField) == null) {
                Object existingId = null;
                List<Object> rows = def.getMapper().selectList(null);
                if (rows != null && !rows.isEmpty()) {
                    existingId = readId(rows.get(0), def);
                }
                body.put(idField, existingId != null ? existingId : 1);
            }

            Object entity = objectMapper.convertValue(body, def.getEntityClass());
            Object idValue = readId(entity, def);
            Object existing = idValue == null ? null : def.getMapper().selectById((java.io.Serializable) idValue);
            if (existing != null) {
                def.getMapper().updateById(entity);
            } else {
                def.getMapper().insert(entity);
            }
            return entity;
        }

        stampOwner(name, body, userId);
        Object entity = objectMapper.convertValue(body, def.getEntityClass());
        assignSortNo(def, entity, userId);
        fillIdIfAbsent(def, entity);
        def.getMapper().insert(entity);
        return entity;
    }

    public Object patch(String name, String id, Map<String, Object> patch, String userId) {
        TableDef def = def(name);
        assertWritable(name);

        if ("user".equals(name)) {
            Map<String, Object> body = new LinkedHashMap<>(patch);
            body.put("id", userId);
            Object entity = objectMapper.convertValue(body, def.getEntityClass());
            def.getMapper().updateById(entity);
            return def.getMapper().selectById(userId);
        }

        Object existing = def.getMapper().selectById(convertId(def, id));
        if (existing == null) {
            throw BizException.of("记录不存在：" + name + "/" + id);
        }
        assertOwner(name, existing, userId);

        Map<String, Object> safe = new LinkedHashMap<>(patch);
        safe.remove(def.getIdField());
        OWNER_COLUMN.forEach((table, column) -> {
            if (table.equals(name)) {
                safe.remove(toCamel(column));
            }
        });
        try {
            objectMapper.updateValue(existing, safe);
        } catch (Exception e) {
            throw BizException.of("更新失败：" + e.getMessage());
        }
        def.getMapper().updateById(existing);
        return existing;
    }

    /** 整表覆盖：只清空当前用户自己的行，再按数组顺序重建 */
    public List<Object> replaceAll(String name, List<Map<String, Object>> rows, String userId) {
        TableDef def = def(name);
        assertWritable(name);

        if ("user".equals(name)) {
            List<Object> saved = new ArrayList<>();
            if (rows != null && !rows.isEmpty()) {
                saved.add(insert(name, rows.get(0), userId));
            }
            return saved;
        }

        if (def.isSingle()) {
            List<Object> saved = new ArrayList<>();
            if (rows != null && !rows.isEmpty()) {
                saved.add(insert(name, rows.get(0), userId));
            }
            return saved;
        }

        String ownerColumn = OWNER_COLUMN.get(name);
        QueryWrapper<Object> deleteWrapper = new QueryWrapper<>();
        if (ownerColumn != null) {
            deleteWrapper.eq(ownerColumn, userId);
        }
        def.getMapper().delete(deleteWrapper);

        List<Object> saved = new ArrayList<>();
        if (rows == null) {
            return saved;
        }
        long index = 1;
        for (Map<String, Object> row : rows) {
            stampOwner(name, row, userId);
            Object entity = objectMapper.convertValue(row, def.getEntityClass());
            if (entity instanceof Sortable sortable) {
                sortable.setSortNo(index++);
            }
            fillIdIfAbsent(def, entity);
            def.getMapper().insert(entity);
            saved.add(entity);
        }
        return saved;
    }

    public void delete(String name, String id, String userId) {
        TableDef def = def(name);
        assertWritable(name);
        Object existing = def.getMapper().selectById(convertId(def, id));
        if (existing == null) {
            return;
        }
        assertOwner(name, existing, userId);
        def.getMapper().deleteById(convertId(def, id));
    }

    public void clear(String name, String userId) {
        assertWritable(name);
        QueryWrapper<Object> wrapper = new QueryWrapper<>();
        String ownerColumn = OWNER_COLUMN.get(name);
        if (ownerColumn != null) {
            wrapper.eq(ownerColumn, userId);
        }
        def(name).getMapper().delete(wrapper);
    }

    /* ------------------------------------------------------------------ */
    /* 内部工具                                                            */
    /* ------------------------------------------------------------------ */

    private void assertWritable(String name) {
        if (READ_ONLY.contains(name)) {
            throw BizException.of("该数据表不支持直接写入，请使用对应业务接口");
        }
    }

    private void stampOwner(String name, Map<String, Object> body, String userId) {
        String ownerColumn = OWNER_COLUMN.get(name);
        if (ownerColumn == null) {
            return;
        }
        body.put(toCamel(ownerColumn), userId);
    }

    private void assertOwner(String name, Object entity, String userId) {
        String ownerColumn = OWNER_COLUMN.get(name);
        if (ownerColumn == null) {
            return;
        }
        try {
            Field field = entity.getClass().getDeclaredField(toCamel(ownerColumn));
            field.setAccessible(true);
            Object value = field.get(entity);
            if (value != null && !userId.equals(String.valueOf(value))) {
                throw BizException.of("无权操作他人的数据");
            }
            field.set(entity, userId);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // 实体没有归属字段时跳过
        }
    }

    private String toCamel(String column) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : column.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                sb.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return sb.toString();
    }

    private void assignSortNo(TableDef def, Object entity, String userId) {
        if (!(entity instanceof Sortable sortable)) {
            return;
        }
        if (sortable.getSortNo() != null) {
            return;
        }
        String aggregate = def.isInsertAtHead() ? "MIN(sort_no)" : "MAX(sort_no)";
        QueryWrapper<Object> wrapper = new QueryWrapper<>();
        String ownerColumn = OWNER_COLUMN.get(def.getName());
        if (ownerColumn != null) {
            wrapper.eq(ownerColumn, userId);
        }
        wrapper.select(aggregate + " AS agg");
        List<Map<String, Object>> maps = def.getMapper().selectMaps(wrapper);
        long base = 0L;
        if (maps != null && !maps.isEmpty() && maps.get(0) != null) {
            Object agg = maps.get(0).get("agg");
            if (agg instanceof Number number) {
                base = number.longValue();
            }
        }
        sortable.setSortNo(def.isInsertAtHead() ? base - 1 : base + 1);
    }

    private void fillIdIfAbsent(TableDef def, Object entity) {
        try {
            Field field = def.getEntityClass().getDeclaredField(def.getIdField());
            field.setAccessible(true);
            Object value = field.get(entity);
            if (value == null || "".equals(value)) {
                if (field.getType() == String.class) {
                    field.set(entity, "auto_" + System.nanoTime());
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // 字段缺失时交由数据库约束报错
        }
    }

    private Object readId(Object entity, TableDef def) {
        try {
            Field field = def.getEntityClass().getDeclaredField(def.getIdField());
            field.setAccessible(true);
            return field.get(entity);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    private java.io.Serializable convertId(TableDef def, String id) {
        try {
            Field field = def.getEntityClass().getDeclaredField(def.getIdField());
            if (field.getType() == Integer.class) {
                return Integer.valueOf(id);
            }
            if (field.getType() == Long.class) {
                return Long.valueOf(id);
            }
        } catch (NoSuchFieldException ignored) {
            // 默认按字符串处理
        }
        return id;
    }
}
