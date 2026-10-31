package com.bangbangbang.service;

import com.bangbangbang.common.NameInitial;
import com.bangbangbang.entity.Contact;
import com.bangbangbang.entity.HelpOrder;
import com.bangbangbang.entity.Post;
import com.bangbangbang.entity.PostComment;
import com.bangbangbang.entity.SysUser;
import com.bangbangbang.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 展示层「用户信息校准」
 *
 * 动态、评论、订单、通讯录这些表在写入时把姓名/头像/标签一起存了一份快照，
 * 好处是查询不用 JOIN，代价是用户改了资料之后快照不会自己变 ——
 * 同一个人会在「全部动态」显示旧名、在「私信」显示新名（会话列表是实时查的）。
 *
 * 这里在**返回给前端之前**统一用 sys_user 的最新值覆盖一遍：
 *   · 不依赖数据库里的历史数据是否干净（管理员直接改过库也能正确显示）
 *   · 只改内存对象，不回写数据库（回写由 UserProfileSyncService 负责）
 *
 * 批量接口都按 ID 做一次 IN 查询，不会产生 N+1。
 */
@Service
public class UserDisplayService {

    private final SysUserMapper userMapper;

    public UserDisplayService(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 动态作者信息 */
    public void fillPosts(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }
        Set<String> ids = new LinkedHashSet<>();
        for (Post post : posts) {
            ids.add(post.getAuthorId());
        }
        Map<String, SysUser> users = loadUsers(ids);
        for (Post post : posts) {
            SysUser user = users.get(post.getAuthorId());
            if (user == null) {
                continue;
            }
            if (notEmpty(user.getName())) {
                post.setAuthorName(user.getName());
            }
            if (notEmpty(user.getAvatar())) {
                post.setAuthorAvatar(user.getAvatar());
            }
            post.setAuthorTag(user.getIdentityTag());
        }
    }

    /** 评论人信息 */
    public void fillComments(List<PostComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }
        Set<String> ids = new LinkedHashSet<>();
        for (PostComment comment : comments) {
            ids.add(comment.getAuthorId());
        }
        Map<String, SysUser> users = loadUsers(ids);
        for (PostComment comment : comments) {
            SysUser user = users.get(comment.getAuthorId());
            if (user == null) {
                continue;
            }
            if (notEmpty(user.getName())) {
                comment.setUserName(user.getName());
            }
            if (notEmpty(user.getAvatar())) {
                comment.setAvatar(user.getAvatar());
            }
        }
    }

    /** 订单里的发布人 + 接单人 */
    public void fillOrders(List<HelpOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        Set<String> ids = new LinkedHashSet<>();
        for (HelpOrder order : orders) {
            ids.add(order.getCreatorId());
            ids.add(order.getHelperId());
        }
        Map<String, SysUser> users = loadUsers(ids);
        for (HelpOrder order : orders) {
            applyCreator(order, users.get(order.getCreatorId()));
            applyHelper(order, users.get(order.getHelperId()));
        }
    }

    /** 单条订单（接单、完成等接口只返回一条） */
    public void fillOrder(HelpOrder order) {
        if (order == null) {
            return;
        }
        Set<String> ids = new LinkedHashSet<>();
        ids.add(order.getCreatorId());
        ids.add(order.getHelperId());
        Map<String, SysUser> users = loadUsers(ids);
        applyCreator(order, users.get(order.getCreatorId()));
        applyHelper(order, users.get(order.getHelperId()));
    }

    /**
     * 通讯录：只校准「已关联真实账号」的条目。
     * 手工录入的联系人（target_user_id 为空）保留用户自己填的姓名，不去覆盖。
     */
    public void fillContacts(List<Contact> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return;
        }
        Set<String> ids = new LinkedHashSet<>();
        for (Contact contact : contacts) {
            ids.add(contact.getTargetUserId());
        }
        Map<String, SysUser> users = loadUsers(ids);
        for (Contact contact : contacts) {
            SysUser user = users.get(contact.getTargetUserId());
            if (user == null) {
                continue;
            }
            if (notEmpty(user.getName())) {
                contact.setName(user.getName());
                contact.setLetter(NameInitial.of(user.getName()));
            }
            if (notEmpty(user.getAvatar())) {
                contact.setAvatar(user.getAvatar());
            }
            if (notEmpty(user.getPhone())) {
                contact.setPhone(user.getPhone());
            }
            contact.setTag(user.getIdentityTag());
        }
    }

    /* ------------------------------------------------------------------ */

    private void applyCreator(HelpOrder order, SysUser user) {
        if (user == null) {
            return;
        }
        if (notEmpty(user.getName())) {
            order.setCreatorName(user.getName());
        }
        if (notEmpty(user.getAvatar())) {
            order.setCreatorAvatar(user.getAvatar());
        }
        order.setCreatorTag(user.getIdentityTag());
    }

    private void applyHelper(HelpOrder order, SysUser user) {
        if (user == null) {
            return;
        }
        if (notEmpty(user.getName())) {
            order.setHelperName(user.getName());
        }
        if (notEmpty(user.getAvatar())) {
            order.setHelperAvatar(user.getAvatar());
        }
        if (notEmpty(user.getPhone())) {
            order.setHelperPhone(user.getPhone());
        }
        order.setHelperTag(user.getIdentityTag());
    }

    /** 一次 IN 查询取回所有用户，忽略空 ID，查询结果按 ID 建索引 */
    private Map<String, SysUser> loadUsers(Collection<String> ids) {
        Map<String, SysUser> users = new HashMap<>();
        Set<String> valid = new LinkedHashSet<>();
        for (String id : ids) {
            if (notEmpty(id)) {
                valid.add(id);
            }
        }
        if (valid.isEmpty()) {
            return users;
        }
        List<SysUser> list = userMapper.selectBatchIds(valid);
        if (list != null) {
            for (SysUser user : list) {
                users.put(user.getId(), user);
            }
        }
        return users;
    }

    private boolean notEmpty(String value) {
        return value != null && !value.isEmpty();
    }
}
