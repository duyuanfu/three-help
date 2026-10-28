package com.bangbangbang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 聊天会话（原 db.ts 的 sessions 表）
 * targetUser 在库中平铺为列，序列化时还原成嵌套对象
 */
@Data
@TableName("chat_session")
public class ChatSession implements Sortable {

    @TableId(type = IdType.INPUT)
    private String sessionId;

    /** 会话双方（A、B 按 ID 字典序存放，保证同一对用户只有一条会话） */
    private String userAId;
    private String userBId;

    /** 双方各自的未读数 */
    private Integer unreadA;
    private Integer unreadB;

    private String targetUserId;
    private String targetUserName;
    private String targetUserAvatar;
    private String targetUserTag;

    private String lastMessage;
    private String lastTime;
    private Integer unreadCount;
    private String relatedOrderId;
    private Long sortNo;

    /* -------- 视图属性：与前端 JSON 结构对齐 -------- */

    @JsonProperty("targetUser")
    public TargetUser getTargetUserView() {
        return new TargetUser(targetUserId, targetUserName, targetUserAvatar, targetUserTag);
    }

    @JsonProperty("targetUser")
    public void setTargetUserView(TargetUser targetUser) {
        if (targetUser == null) {
            return;
        }
        this.targetUserId = targetUser.getId();
        this.targetUserName = targetUser.getName();
        this.targetUserAvatar = targetUser.getAvatar();
        this.targetUserTag = targetUser.getTag();
    }
}
