package com.bangbangbang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 积分商城商品（原 db.ts 的 goods 表）
 */
@Data
@TableName("goods")
public class Goods implements Sortable {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;
    private Integer price;
    private String image;
    private String unit;
    private Integer stock;
    private Long sortNo;
}
