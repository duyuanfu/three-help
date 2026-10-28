package com.bangbangbang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 聊天消息（原 db.ts 的 messages 单行表，改造为一行一条消息）
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.INPUT)
    private String id;

    private String sessionId;
    private String senderId;
    private String receiverId;
    private String type;
    private String content;
    private Integer duration;
    private String audioPath;
    private String createdAt;
    private Long timestamp;
}
