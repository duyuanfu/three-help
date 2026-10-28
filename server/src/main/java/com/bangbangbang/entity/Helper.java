package com.bangbangbang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 志愿者帮手（原 db.ts 的 helpers 表）
 */
@Data
@TableName("helper")
public class Helper implements Sortable {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;
    private String tag;
    private String avatar;
    private String phone;
    private Long sortNo;
}
