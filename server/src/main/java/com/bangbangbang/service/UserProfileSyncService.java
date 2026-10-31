package com.bangbangbang.service;

import com.bangbangbang.common.NameInitial;
import com.bangbangbang.entity.SysUser;
import com.bangbangbang.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户资料变更后的「冗余字段同步」
 *
 * 背景：为了少做 JOIN，动态、评论、订单、会话、通讯录这些表在写入时
 * 都把当时的姓名/头像/标签一并存了一份快照（post.author_name 等）。
 * 快照一旦落库就不会自己变，于是出现「同一个人」在不同页面名字不一样：
 *
 *   · 动态卡片读的是 post.author_name —— 发布那一刻的快照
 *   · 私信会话读的是实时 JOIN sys_user —— 永远是最新值
 *
 * 解决思路是「以 sys_user 为唯一事实来源」：
 *   1. 资料一变更（个人资料页、实名认证、后台改用户）就调用本服务扩散；
 *   2. 启动时由 DataMigration 再做一次全量回填，修历史脏数据。
 *
 * 为什么必须集中在服务端做：这些字段前端可以随意传，
 * 真让客户端同步，一次漏传就永久不一致。
 */
@Slf4j
@Service
public class UserProfileSyncService {

    private final JdbcTemplate jdbc;
    private final SysUserMapper userMapper;

    public UserProfileSyncService(JdbcTemplate jdbc, SysUserMapper userMapper) {
        this.jdbc = jdbc;
        this.userMapper = userMapper;
    }

    /** 按用户 ID 同步（内部先读一次最新资料） */
    public void syncByUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        sync(user);
    }

    /** 用一份最新的用户资料刷新它在各表里的快照 */
    @Transactional
    public void sync(SysUser user) {
        if (user == null || user.getId() == null || user.getId().isEmpty()) {
            return;
        }
        String id = user.getId();
        String name = nvl(user.getName());
        String avatar = nvl(user.getAvatar());
        String tag = nvl(user.getIdentityTag());
        String phone = nvl(user.getPhone());

        // 空值保护：资料里没填头像时不把别人已有的头像抹掉
        int post = jdbc.update(
                "UPDATE post SET author_name = IF(? = '', author_name, ?), " +
                        "author_avatar = IF(? = '', author_avatar, ?), " +
                        "author_tag = ? WHERE author_id = ?",
                name, name, avatar, avatar, tag, id);

        int comment = jdbc.update(
                "UPDATE post_comment SET user_name = IF(? = '', user_name, ?), " +
                        "avatar = IF(? = '', avatar, ?) WHERE author_id = ?",
                name, name, avatar, avatar, id);

        // 发布人
        jdbc.update(
                "UPDATE help_order SET creator_name = IF(? = '', creator_name, ?), " +
                        "creator_avatar = IF(? = '', creator_avatar, ?), " +
                        "creator_tag = ? WHERE creator_id = ?",
                name, name, avatar, avatar, tag, id);

        // 接单人：电话也一并同步，接单后双方靠这个号联系
        jdbc.update(
                "UPDATE help_order SET helper_name = IF(? = '', helper_name, ?), " +
                        "helper_avatar = IF(? = '', helper_avatar, ?), " +
                        "helper_tag = ?, helper_phone = IF(? = '', helper_phone, ?) WHERE helper_id = ?",
                name, name, avatar, avatar, tag, phone, phone, id);

        // 会话里的兼容字段（列表接口本身是实时查用户，这里保持字段不陈旧）
        jdbc.update(
                "UPDATE chat_session SET target_user_name = IF(? = '', target_user_name, ?), " +
                        "target_user_avatar = IF(? = '', target_user_avatar, ?), " +
                        "target_user_tag = ? WHERE target_user_id = ?",
                name, name, avatar, avatar, tag, id);

        // 通讯录：只同步「已关联真实账号」的条目。
        // 手工录入的联系人（target_user_id 为空）保留用户自己填的名字，不去覆盖。
        int contact = jdbc.update(
                "UPDATE contact SET name = IF(? = '', name, ?), " +
                        "avatar = IF(? = '', avatar, ?), " +
                        "tag = ?, phone = IF(? = '', phone, ?), letter = ? WHERE target_user_id = ?",
                name, name, avatar, avatar, tag, phone, phone, NameInitial.of(name), id);

        log.info("[资料同步] 用户 {} 的展示字段已刷新：动态 {} 条、评论 {} 条、联系人 {} 条",
                id, post, comment, contact);
    }

    /** MySQL 里 NULL 与 '' 对前端都是「没填」，统一按空串处理 */
    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
