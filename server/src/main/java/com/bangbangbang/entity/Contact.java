package com.bangbangbang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 通讯录联系人（原 db.ts 的 contacts 表）
 */
@Data
@TableName("contact")
public class Contact implements Sortable {

    @TableId(type = IdType.INPUT)
    private String id;

    /** 归属用户（通讯录是每个人私有的） */
    private String ownerId;

    /** 如果该手机号对应已注册用户，则记录对方账号ID，用于真实聊天 */
    private String targetUserId;

    private String name;
    private String avatar;
    private String tag;
    private String letter;
    private String phone;
    private Boolean isFriend;
    private Long sortNo;
}
