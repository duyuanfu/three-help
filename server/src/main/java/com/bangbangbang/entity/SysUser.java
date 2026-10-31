package com.bangbangbang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户（原 db.ts 的 user 单行表）
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;
    private String avatar;
    private String gender;
    private Integer age;
    private String phone;

    /** PBKDF2 密码哈希，永不下发到前端 */
    @JsonIgnore
    private String passwordHash;

    private Boolean isRealName;
    private String identityTag;
    private String occupation;
    private String address;
    private String organization;
    private Integer points;
    private String role;
    private Boolean isLoggedIn;

    /** 是否为管理员（可进入管理后台查看/编辑/删除任意数据） */
    private Boolean isAdmin;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
