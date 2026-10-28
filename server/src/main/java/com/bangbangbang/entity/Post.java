package com.bangbangbang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.List;

/**
 * 广场动态（原 db.ts 的 posts 表）
 */
@Data
@TableName(value = "post", autoResultMap = true)
public class Post implements Sortable {

    @TableId(type = IdType.INPUT)
    private String id;

    private String authorId;
    private String authorName;
    private String authorAvatar;
    private String authorTag;
    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private Integer likeCount;
    private Integer commentCount;
    private String timeAgo;
    private Boolean isLiked;
    private Long sortNo;
}
