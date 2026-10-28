package com.bangbangbang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 收货地址（原 db.ts 的 addresses 表）
 */
@Data
@TableName("address")
public class Address implements Sortable {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;
    private String phone;
    private String detail;
    private Boolean isDefault;
    private Long sortNo;
}
