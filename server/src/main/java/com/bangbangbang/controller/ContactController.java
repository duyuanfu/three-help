package com.bangbangbang.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bangbangbang.common.ApiResult;
import com.bangbangbang.common.BizException;
import com.bangbangbang.common.NameInitial;
import com.bangbangbang.common.UserContext;
import com.bangbangbang.entity.Contact;
import com.bangbangbang.entity.SysUser;
import com.bangbangbang.mapper.ContactMapper;
import com.bangbangbang.mapper.SysUserMapper;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通讯录：每个人只能看到、操作自己的联系人
 */
@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactMapper contactMapper;
    private final SysUserMapper userMapper;

    public ContactController(ContactMapper contactMapper, SysUserMapper userMapper) {
        this.contactMapper = contactMapper;
        this.userMapper = userMapper;
    }

    @Data
    public static class AddContactRequest {
        private String name;
        private String phone;
        private String tag;
        private String avatar;
        /** 已知对方账号ID时可直接关联（例如从广场动态加好友） */
        private String targetUserId;
    }

    @GetMapping
    public ApiResult<List<Contact>> list() {
        String meId = UserContext.require().id();
        QueryWrapper<Contact> wrapper = new QueryWrapper<>();
        wrapper.eq("owner_id", meId).orderByAsc("sort_no");
        return ApiResult.ok(contactMapper.selectList(wrapper));
    }

    /**
     * 新增联系人
     * 若手机号对应已注册用户，则自动关联对方真实账号与头像，
     * 这样点进聊天就是和真实的人对话，而不是一个孤立的名字。
     */
    @PostMapping
    public ApiResult<Contact> add(@RequestBody AddContactRequest req) {
        UserContext.CurrentUser me = UserContext.require();

        // 情形一：已知对方账号（从广场动态加好友）
        if (req.getTargetUserId() != null && !req.getTargetUserId().isEmpty()) {
            return ApiResult.ok(addByUserId(me, req.getTargetUserId()));
        }

        // 情形二：只知道手机号（手动录入通讯录）
        String phone = req.getPhone() == null ? "" : req.getPhone().trim();
        String name = req.getName() == null ? "" : req.getName().trim();

        if (name.isEmpty()) {
            throw BizException.of("请填写联系人姓名");
        }
        if (!phone.matches("\\d{11}")) {
            throw BizException.of("请填写 11 位手机号");
        }
        if (phone.equals(me.phone())) {
            throw BizException.of("不能添加自己为联系人");
        }

        QueryWrapper<Contact> existWrapper = new QueryWrapper<>();
        existWrapper.eq("owner_id", me.id()).eq("phone", phone);
        List<Contact> exists = contactMapper.selectList(existWrapper);
        if (exists != null && !exists.isEmpty()) {
            throw BizException.of("该联系人已在通讯录中");
        }

        SysUser target = findByPhone(phone);

        Contact contact = new Contact();
        contact.setId("c_" + System.currentTimeMillis());
        contact.setOwnerId(me.id());
        contact.setPhone(phone);
        contact.setName(target != null ? target.getName() : name);
        contact.setAvatar(target != null ? target.getAvatar() : (req.getAvatar() == null ? "" : req.getAvatar()));
        contact.setTag(target != null ? target.getIdentityTag() : req.getTag());
        contact.setTargetUserId(target == null ? null : target.getId());
        contact.setIsFriend(true);
        contact.setLetter(NameInitial.of(contact.getName()));
        contact.setSortNo(nextSortNo(me.id()));
        contactMapper.insert(contact);
        return ApiResult.ok(contact);
    }

    /** 按账号ID添加联系人（对方已成为平台用户） */
    private Contact addByUserId(UserContext.CurrentUser me, String targetUserId) {
        if (targetUserId.equals(me.id())) {
            throw BizException.of("不能添加自己为联系人");
        }
        SysUser target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw BizException.of("对方账号不存在");
        }

        QueryWrapper<Contact> existWrapper = new QueryWrapper<>();
        existWrapper.eq("owner_id", me.id()).eq("target_user_id", targetUserId);
        List<Contact> exists = contactMapper.selectList(existWrapper);
        if (exists != null && !exists.isEmpty()) {
            throw BizException.of("该联系人已在通讯录中");
        }

        Contact contact = new Contact();
        contact.setId("c_" + System.currentTimeMillis());
        contact.setOwnerId(me.id());
        contact.setPhone(target.getPhone());
        contact.setName(target.getName());
        contact.setAvatar(target.getAvatar());
        contact.setTag(target.getIdentityTag());
        contact.setTargetUserId(target.getId());
        contact.setIsFriend(true);
        contact.setLetter(NameInitial.of(target.getName()));
        contact.setSortNo(nextSortNo(me.id()));
        contactMapper.insert(contact);
        return contact;
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable String id) {
        String meId = UserContext.require().id();
        Contact contact = contactMapper.selectById(id);
        if (contact == null || !meId.equals(contact.getOwnerId())) {
            throw BizException.of("联系人不存在");
        }
        contactMapper.deleteById(id);
        return ApiResult.ok();
    }

    /* ------------------------------------------------------------------ */

    private SysUser findByPhone(String phone) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("phone", phone);
        List<SysUser> list = userMapper.selectList(wrapper);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    private long nextSortNo(String ownerId) {
        QueryWrapper<Contact> wrapper = new QueryWrapper<>();
        wrapper.eq("owner_id", ownerId).select("MAX(sort_no) AS agg");
        List<java.util.Map<String, Object>> maps = contactMapper.selectMaps(wrapper);
        if (maps != null && !maps.isEmpty() && maps.get(0) != null) {
            Object agg = maps.get(0).get("agg");
            if (agg instanceof Number number) {
                return number.longValue() + 1;
            }
        }
        return 1L;
    }

}
