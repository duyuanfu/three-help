package com.bangbangbang.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话对方用户，对应前端 ChatSession.targetUser
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TargetUser {
    private String id;
    private String name;
    private String avatar;
    private String tag;
}
