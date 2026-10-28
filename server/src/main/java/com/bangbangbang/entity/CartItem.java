package com.bangbangbang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 购物车行（原 db.ts 的 cart 表）
 * 库中只存 goodsId + quantity，goods 明细由 Service 层回填
 */
@Data
@TableName("cart_item")
public class CartItem {

    @TableId(type = IdType.INPUT)
    private String id;

    private String userId;
    private String goodsId;
    private Integer quantity;

    /** 商品明细，非数据库字段，由 Service 回填；写入时反推 goodsId */
    @TableField(exist = false)
    private Goods goods;

    public void setGoods(Goods goods) {
        this.goods = goods;
        if (goods != null && goods.getId() != null
                && (this.goodsId == null || this.goodsId.isEmpty())) {
            this.goodsId = goods.getId();
        }
    }

    /* -------- 视图属性：前端使用 count 字段名 -------- */

    @JsonProperty("count")
    public Integer getCountView() {
        return quantity;
    }

    @JsonProperty("count")
    public void setCountView(Integer count) {
        this.quantity = count;
    }
}
