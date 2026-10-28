package com.bangbangbang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.List;

/**
 * 积分兑换订单（原 db.ts 的 exchange_orders 表）
 */
@Data
@TableName(value = "exchange_order", autoResultMap = true)
public class ExchangeOrder implements Sortable {

    @TableId(type = IdType.INPUT)
    private String id;

    private String orderNo;
    private String goodsName;
    private String goodsImage;
    private Integer quantity;
    private Integer pointsCost;
    private String address;
    private String expressCompany;
    private String senderCity;
    private String receiverCity;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<LogisticsStep> steps;

    private String createdAt;
    private Long sortNo;
}
