package com.bangbangbang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录令牌（服务端会话）
 *
 * 采用「随机 token + 服务端存储」而不是无状态 JWT：
 * 1. 无额外依赖，实现直观
 * 2. 支持主动失效（退出登录 / 改密码后踢下线）
 */
@Data
@TableName("user_token")
public class UserToken {

    @TableId(type = IdType.INPUT)
    private String token;

    private String userId;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
}
