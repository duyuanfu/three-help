package com.bangbangbang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 帖子评论（原 db.ts 的 comments 表）
 */
@Data
@TableName("post_comment")
public class PostComment implements Sortable {

    @TableId(type = IdType.INPUT)
    private String id;

    private String postId;

    /** 评论者账号ID（由登录令牌决定，用于权限判断） */
    private String authorId;

    private String userName;
    private String avatar;
    private String content;
    private Long sortNo;
}
