package com.bangbangbang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 互助订单（原 db.ts 的 orders 表）
 * helper、review 在库中平铺为列，序列化时还原成嵌套对象以保持与前端结构一致
 */
@Data
@TableName("help_order")
public class HelpOrder implements Sortable {

    @TableId(type = IdType.INPUT)
    private String id;

    private String creatorId;
    private String creatorName;
    private String creatorAvatar;
    private String creatorTag;
    private String category;
    private String title;
    private String content;
    private String appointTime;
    private String address;
    private Integer bounty;
    private String status;
    private String statusText;

    private String helperId;
    private String helperName;
    private String helperTag;
    private String helperAvatar;
    private String helperPhone;

    private Integer reviewStars;
    private String reviewComment;

    private String createdAt;
    private String completedAt;
    private Long sortNo;

    /**
     * 当前登录用户与该订单的关系，由服务层按令牌填充，不落库。
     * bootstrap 与 /api/orders 两条路径都要带上，前端据此决定
     * 展示"发单人"还是"接单人"，以及按钮文案。
     */
    @TableField(exist = false)
    @JsonProperty("isMine")
    private Boolean isMine;

    @TableField(exist = false)
    @JsonProperty("isHelper")
    private Boolean isHelper;

    /* -------- 视图属性：与前端 JSON 结构对齐 -------- */

    @JsonProperty("helper")
    public HelperInfo getHelperView() {
        if (helperId == null && helperName == null) {
            return null;
        }
        return new HelperInfo(helperId, helperName, helperTag, helperAvatar, helperPhone);
    }

    @JsonProperty("helper")
    public void setHelperView(HelperInfo helper) {
        if (helper == null) {
            this.helperId = null;
            this.helperName = null;
            this.helperTag = null;
            this.helperAvatar = null;
            this.helperPhone = null;
            return;
        }
        this.helperId = helper.getId();
        this.helperName = helper.getName();
        this.helperTag = helper.getTag();
        this.helperAvatar = helper.getAvatar();
        this.helperPhone = helper.getPhone();
    }

    @JsonProperty("review")
    public Review getReviewView() {
        if (reviewStars == null && reviewComment == null) {
            return null;
        }
        return new Review(reviewStars, reviewComment);
    }

    @JsonProperty("review")
    public void setReviewView(Review review) {
        if (review == null) {
            this.reviewStars = null;
            this.reviewComment = null;
            return;
        }
        this.reviewStars = review.getStars();
        this.reviewComment = review.getComment();
    }
}
